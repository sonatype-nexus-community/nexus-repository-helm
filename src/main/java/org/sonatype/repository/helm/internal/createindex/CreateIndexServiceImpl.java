/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2018-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.repository.helm.internal.createindex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.constraints.Null;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.helm.internal.metadata.ChartEntry;
import org.sonatype.repository.helm.internal.metadata.ChartIndex;
import org.sonatype.repository.helm.internal.metadata.IndexYamlBuilder;
import org.sonatype.repository.helm.internal.util.HelmDataAccess;

import com.google.common.collect.Iterables;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.repository.helm.internal.database.HelmProperties.APP_VERSION;
import static org.sonatype.repository.helm.internal.database.HelmProperties.DESCRIPTION;
import static org.sonatype.repository.helm.internal.database.HelmProperties.ICON;
import static org.sonatype.repository.helm.internal.database.HelmProperties.MAINTAINERS;
import static org.sonatype.repository.helm.internal.database.HelmProperties.NAME;
import static org.sonatype.repository.helm.internal.database.HelmProperties.SOURCES;
import static org.sonatype.repository.helm.internal.database.HelmProperties.VERSION;

/**
 * Build index.yaml file for Helm Hosted
 *
 * @since 0.0.2
 */
@Named
@Singleton
public class CreateIndexServiceImpl
    extends ComponentSupport
    implements CreateIndexService
{
  private final static String API_VERSION = "1.0";

  private HelmDataAccess helmDataAccess;

  private IndexYamlBuilder indexYamlBuilder;

  @Inject
  public CreateIndexServiceImpl(final HelmDataAccess helmDataAccess,
                                final IndexYamlBuilder indexYamlBuilder) {
    this.helmDataAccess = checkNotNull(helmDataAccess);
    this.indexYamlBuilder = checkNotNull(indexYamlBuilder);
  }

  @TransactionalStoreBlob
  @Nullable
  public TempBlob buildIndexYaml(final Repository repository) {
    StorageFacet storageFacet = repository.facet(StorageFacet.class);
    StorageTx tx = UnitOfWork.currentTx();

    ChartIndex index = new ChartIndex();

    for (Asset asset : helmDataAccess.browseComponentAssets(tx, tx.findBucket(repository))) {
      parseAssetIntoChartEntry(index, asset);
    }

    if (index.getEntries().size() == 0) {
      return null;
    }
    else {
      index.setApiVersion(API_VERSION);
      index.setGenerated(new DateTime());
      return indexYamlBuilder.build(index, storageFacet);
    }
  }

  private void parseAssetIntoChartEntry(final ChartIndex index, final Asset asset) {
    NestedAttributesMap formatAttributes = asset.formatAttributes();
    NestedAttributesMap assetAttributes = asset.attributes();
    ChartEntry chartEntry = new ChartEntry();
    chartEntry.setName(formatAttributes.get(NAME, String.class));
    chartEntry.setVersion(formatAttributes.get(VERSION, String.class));
    chartEntry.setDescription(formatAttributes.get(DESCRIPTION, String.class));
    chartEntry.setIcon(formatAttributes.get(ICON, String.class));
    chartEntry.setCreated(asset.blobCreated());
    chartEntry.setAppVersion(formatAttributes.get(APP_VERSION, String.class));
    chartEntry.setMaintainers(formatAttributes.get(MAINTAINERS, List.class));
    chartEntry.setDigest(assetAttributes.get("checksum", Map.class)
        .get("sha256").toString());
    createListOfRelativeUrls(formatAttributes, chartEntry);
    chartEntry.setSources(formatAttributes.get(SOURCES, List.class));
    index.addEntry(chartEntry);
  }

  private void createListOfRelativeUrls(final NestedAttributesMap formatAttributes, final ChartEntry chartEntry) {
    List<String> urls = new ArrayList<>();
    urls.add(String.format("%s-%s.tgz",
        formatAttributes.get(NAME, String.class),
        formatAttributes.get(VERSION, String.class)));
    chartEntry.setUrls(urls);
  }
}
