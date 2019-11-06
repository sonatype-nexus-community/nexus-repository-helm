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
package org.sonatype.repository.helm.internal.hosted;

import com.google.common.base.Supplier;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.helm.AttributesMapAdapter;
import org.sonatype.repository.helm.HelmFacet;
import org.sonatype.repository.helm.internal.AssetKind;
import org.sonatype.repository.helm.internal.util.HelmAttributeParser;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.repository.helm.internal.AssetKind.HELM_PACKAGE;
import static org.sonatype.repository.helm.internal.HelmFormat.HASH_ALGORITHMS;

/**
 * {@link HelmHostedFacetImpl implementation}
 *
 * @since 0.0.2
 */
@Named
public class HelmHostedFacetImpl
    extends FacetSupport
    implements HelmHostedFacet
{
  private final HelmAttributeParser helmAttributeParser;

  private HelmFacet helmFacet;

  @Inject
  public HelmHostedFacetImpl(final HelmAttributeParser helmAttributeParser)
  {
    this.helmAttributeParser = helmAttributeParser;
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    getRepository().facet(StorageFacet.class).registerWritePolicySelector(new HelmHostedWritePolicySelector());
    helmFacet = facet(HelmFacet.class);
  }

  @Nullable
  @Override
  @TransactionalTouchBlob
  public Content get(final String path) {
    checkNotNull(path);
    StorageTx tx = UnitOfWork.currentTx();

    Optional<Asset> assetOpt = helmFacet.findAsset(path);
    if (!assetOpt.isPresent()) {
      return null;
    }
    Asset asset = assetOpt.get();
    if (asset.markAsDownloaded()) {
      tx.saveAsset(asset);
    }

    return helmFacet.toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  @Override
  public void upload(final String path,
                     final Payload payload,
                     final AssetKind assetKind) throws IOException
  {
    checkNotNull(path);
    checkNotNull(payload);

    if (assetKind != HELM_PACKAGE) {
      throw new IllegalArgumentException("Unsupported assetKind: " + assetKind);
    }

    try (TempBlob tempBlob = facet(StorageFacet.class).createTempBlob(payload, HASH_ALGORITHMS)) {
      storeChart(path, tempBlob, payload);
    }
  }

  @Override
  public Asset upload(String path, TempBlob tempBlob,Payload payload) throws IOException {
    checkNotNull(path);
    checkNotNull(tempBlob);
    return storeChart(path, tempBlob, payload);
  }

  @Override
  @TransactionalDeleteBlob
  public boolean delete(final String path) {
    checkNotNull(path);

    Optional<Asset> asset = helmFacet.findAsset(path);
    if (!asset.isPresent()) {
      return false;
    } else {
      StorageTx tx = UnitOfWork.currentTx();
      tx.deleteAsset(asset.get());
      return true;
    }
  }

  @TransactionalStoreBlob
  protected Asset storeChart(final String assetPath,
                             final Supplier<InputStream> chartContent,
                             final Payload payload) throws IOException
  {
    AttributesMapAdapter chart = helmAttributeParser.getAttributesFromInputStream(chartContent.get(), HELM_PACKAGE);
    chart.addAdditionalContent(payload);
    return helmFacet.findOrCreateAssetWithBlob(assetPath, chart, chartContent).getLeft();
  }
}
