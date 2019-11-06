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

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.Query.Builder;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.helm.AttributesMapAdapter;
import org.sonatype.repository.helm.HelmFacet;

import com.google.common.base.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_COMPONENT;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;
import static org.sonatype.nexus.repository.storage.Query.builder;

/**
 * {@link HelmFacet} implementation.
 *
 * @since 1.0.next
 */
@Named
public class HelmFacetImpl
    extends FacetSupport
    implements HelmFacet
{
  @Override
  public Asset findOrCreateAsset(
      final String assetPath,
      final AttributesMapAdapter helmAttributes)
  {
    Optional<Asset> assetOpt = findAsset(assetPath);
    return assetOpt.orElseGet(() ->
        createAsset(assetPath, helmAttributes, helmAttributes.getAssetKind()));
  }

  @Override
  public Content findOrCreateAssetWithBlob(
      final String assetPath,
      final AttributesMapAdapter helmAttributes,
      final Supplier<InputStream> contentSupplier)
  {
    Asset asset = findOrCreateAsset(assetPath, helmAttributes);
    return saveAsset(asset, contentSupplier, helmAttributes.getContentType(), helmAttributes.getContentAttributes());
  }

  private Asset createAsset(
      final String assetPath,
      final AttributesMapAdapter helmAttributes,
      final AssetKind assetKind)
  {
    checkNotNull(assetKind);

    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = assetKind == AssetKind.HELM_PACKAGE
        ? tx.createAsset(bucket, findOrCreateComponent(helmAttributes))
        : tx.createAsset(bucket, getRepository().getFormat());
    helmAttributes.populate(asset.formatAttributes());
    asset.name(assetPath);
    tx.saveAsset(asset);
    return asset;
  }

  private Component findOrCreateComponent(final AttributesMapAdapter chart)
  {
    Optional<Component> componentOpt = findComponent(chart.getName(), chart.getVersion());
    if (!componentOpt.isPresent()) {
      StorageTx tx = UnitOfWork.currentTx();
      Bucket bucket = tx.findBucket(getRepository());
      Component component = tx.createComponent(bucket, getRepository().getFormat())
          .name(chart.getName())
          .version(chart.getVersion());
      tx.saveComponent(component);
      return component;
    }
    return componentOpt.get();
  }

  /**
   * Find a component by its name and tag (version)
   *
   * @return found Optional<component> or Optional.empty if not found
   */
  private Optional<Component> findComponent(final String name,
                                 final String version)
  {
    StorageTx tx = UnitOfWork.currentTx();
    Query query = builder()
        .where(P_NAME).eq(name)
        .and(P_VERSION).eq(version)
        .build();
    return StreamSupport.stream(tx.findComponents(query, singletonList(getRepository())).spliterator(), false)
        .findFirst();
  }

  /**
   * Find assets for Helm components (charts)
   *
   * @return found assets or null if not found
   */
  @Nullable
  public Iterable<Asset> browseComponentAssets()
  {
    Builder builder = builder()
        .where(P_COMPONENT).isNotNull();

    Query query = builder.build();

    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    return tx.browseAssets(query, bucket);
  }

  /**
   * Find an asset by its name.
   *
   * @return found Optional<Asset> or Optional.empty if not found
   */
  @Override
  public Optional<Asset> findAsset(final String assetName) {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = tx.findAssetWithProperty(P_NAME, assetName, bucket);
    return Optional.ofNullable(asset);
  }

  /**browseComponentAssets
   * Save an asset and create blob.
   *
   * @return blob content
   */
  private Content saveAsset(final Asset asset,
                           final Supplier<InputStream> contentSupplier,
                           final String contentType,
                           final AttributesMap contentAttributes)
  {
    try
    {
      StorageTx tx = UnitOfWork.currentTx();
      return saveAsset(tx, asset, contentSupplier, contentType, contentAttributes);
    }
    catch (IOException ex){
      log.warn("Could not set blob {}", ex);
      return null;
    }
  }

  /**
   * Save an asset and create blob.
   *
   * @return blob content
   */
  private Content saveAsset(final StorageTx tx,
                           final Asset asset,
                           final Supplier<InputStream> contentSupplier,
                           @Nullable final String contentType,
                           @Nullable final AttributesMap contentAttributes) throws IOException
  {
    Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));
    AssetBlob assetBlob = tx.setBlob(
        asset, asset.name(), contentSupplier, HelmFormat.HASH_ALGORITHMS, null, contentType, false
    );
    asset.markAsDownloaded();
    tx.saveAsset(asset);
    return toContent(asset, assetBlob.getBlob());
  }

  /**
   * Convert an asset blob to {@link Content}.
   *
   * @return content of asset blob
   */
  public Content toContent(final Asset asset, final Blob blob) {
    Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
    Content.extractFromAsset(asset, HelmFormat.HASH_ALGORITHMS, content.getAttributes());
    return content;
  }
}
