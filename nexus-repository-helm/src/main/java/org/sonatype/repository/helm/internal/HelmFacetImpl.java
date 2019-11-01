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
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.inject.Inject;
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
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.HelmFacet;

import com.google.common.base.Supplier;
import org.apache.commons.compress.utils.Lists;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
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
  private final HelmAssetAttributePopulator helmAssetAttributePopulator;

  @Inject
  public HelmFacetImpl(
      final HelmAssetAttributePopulator helmAssetAttributePopulator)
  {
    this.helmAssetAttributePopulator = checkNotNull(helmAssetAttributePopulator);
  }

  @Override
  public Asset findOrCreateAsset(
      final String assetPath,
      final AssetKind assetKind,
      final HelmAttributes helmAttributes,
      final boolean isComponentRequired)
  {
    Optional<Asset> assetOpt = findAsset(assetPath);
    Asset asset = assetOpt.orElseGet(() ->
        isComponentRequired
            ? createAsset(assetPath, assetKind,
                  (StorageTx tx, Bucket bucket) -> tx.createAsset(bucket, findOrCreateComponent(helmAttributes)))
            : createAsset(assetPath, assetKind,
                  (StorageTx tx, Bucket bucket) -> tx.createAsset(bucket, getRepository().getFormat())));
    helmAssetAttributePopulator.populate(asset.formatAttributes(), helmAttributes);

    return asset;
  }

  private Asset createAsset(
      final String assetPath,
      final AssetKind assetKind,
      final BiFunction<StorageTx, Bucket, Asset> createAssetFunction)
  {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = createAssetFunction.apply(tx, bucket);
    asset.name(assetPath);
    asset.formatAttributes().set(P_ASSET_KIND, assetKind.name());
    tx.saveAsset(asset);
    return asset;
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

  @Override
  public Component findOrCreateComponent(final HelmAttributes attributes)
  {
    return findComponent(attributes.getName(), attributes.getVersion()).orElseGet(() -> createComponent(attributes));
  }

  private Component createComponent(final HelmAttributes attributes) {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    Component component = tx.createComponent(bucket, getRepository().getFormat())
        .name(attributes.getName())
        .version(attributes.getVersion());
    tx.saveComponent(component);
    return component;
  }

  /**
   * Find a component by its name and tag (version)
   *
   * @return found Optional<component> or Optional.empty if not found
   */
  public Optional<Component> findComponent(
      final String name,
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
   * @return found list of assets or empty list if not found
   */
  @Override
  public List<Asset> getAllAssets()
  {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    Builder builder = builder()
        .where(P_COMPONENT).isNotNull();
    Query query = builder.build();

    return Lists.newArrayList(tx.browseAssets(query, bucket).iterator());
  }

  /**
   * Save an asset and create blob.
   *
   * @return blob content
   */
  public Content saveAsset(
      final Asset asset,
      final Supplier<InputStream> contentSupplier,
      final Payload payload) throws IOException
  {
    AttributesMap contentAttributes = null;
    String contentType = null;
    if (payload instanceof Content) {
      contentAttributes = ((Content) payload).getAttributes();
      contentType = payload.getContentType();
    }
    return saveAsset(asset, contentSupplier, contentType, contentAttributes);
  }

  /**
   * Save an asset and create blob.
   *
   * @return blob content
   */
  public Content saveAsset(
      final Asset asset,
      final Supplier<InputStream> contentSupplier,
      @Nullable final String contentType,
      @Nullable final AttributesMap contentAttributes) throws IOException
  {
    Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));
    StorageTx tx = UnitOfWork.currentTx();
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
