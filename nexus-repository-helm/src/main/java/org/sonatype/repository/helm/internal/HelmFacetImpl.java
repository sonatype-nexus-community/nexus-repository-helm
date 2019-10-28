/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2017-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.repository.helm.internal;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.HelmFacet;
import org.sonatype.repository.helm.internal.util.HelmAttributeParser;
import org.sonatype.repository.helm.internal.util.HelmDataAccess;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.repository.helm.internal.AssetKind.HELM_PACKAGE;

/**
 * {@link HelmFacet} implementation.
 *
 * @since 1.1.next
 */
@Named
public class HelmFacetImpl
    extends FacetSupport
    implements HelmFacet
{
  private final HelmDataAccess helmDataAccess;

  private final HelmAssetAttributePopulator helmAssetAttributePopulator;

  private final HelmAttributeParser helmAttributeParser;

  @Inject
  public HelmFacetImpl(
      final HelmDataAccess helmDataAccess,
      final HelmAssetAttributePopulator helmAssetAttributePopulator,
      final HelmAttributeParser helmAttributeParser)
  {
    this.helmDataAccess = checkNotNull(helmDataAccess);
    this.helmAssetAttributePopulator = checkNotNull(helmAssetAttributePopulator);
    this.helmAttributeParser = checkNotNull(helmAttributeParser);
  }

  @Override
  public HelmDataAccess getHelmDataAccess() {
    return helmDataAccess;
  }

  @Override
  public HelmAssetAttributePopulator getHelmAssetAttributePopulator() {
    return helmAssetAttributePopulator;
  }

  @Override
  public HelmAttributeParser getHelmAttributeParser() {
    return helmAttributeParser;
  }

  @Override
  public Asset findOrCreateAssetWithComponent(
      final String assetPath,
      final StorageTx tx,
      final Bucket bucket,
      final HelmAttributes chart)
  {
    Asset asset = getHelmDataAccess().findAsset(tx, bucket, assetPath);
    if (asset == null) {
      Component component = findOrCreateComponent(tx, bucket, chart);
      asset = tx.createAsset(bucket, component);
      asset.name(assetPath);
      asset.formatAttributes().set(P_ASSET_KIND, HELM_PACKAGE.name());
    }

    getHelmAssetAttributePopulator().populate(asset.formatAttributes(), chart);

    return asset;
  }

  @Override
  public Asset findOrCreateAssetWithAttributes(
      final String assetPath,
      final StorageTx tx,
      final Bucket bucket,
      final HelmAttributes chart)
  {

    Asset asset = getHelmDataAccess().findAsset(tx, bucket, assetPath);
    if (asset == null) {
      asset = tx.createAsset(bucket, getRepository().getFormat());
      asset.name(assetPath);
      asset.formatAttributes().set(P_ASSET_KIND, HELM_PACKAGE.name());
    }

    getHelmAssetAttributePopulator().populate(asset.formatAttributes(), chart);

    return asset;
  }

  @Override
  public Component findOrCreateComponent(
      final StorageTx tx,
      final Bucket bucket,
      final HelmAttributes chart)
  {
    Component component = getHelmDataAccess().findComponent(tx, getRepository(), chart.getName(), chart.getVersion());
    if (component == null) {
      component = tx.createComponent(bucket, getRepository().getFormat())
          .name(chart.getName())
          .version(chart.getVersion());
      tx.saveComponent(component);
    }
    return component;
  }
}
