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
import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
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
import org.sonatype.repository.helm.HelmFacet;
import org.sonatype.repository.helm.AttributesMapAdapter;
import org.sonatype.repository.helm.internal.database.HelmProperties;

import com.google.common.base.Supplier;
import org.apache.commons.lang3.tuple.Pair;
import org.elasticsearch.action.index.IndexRequest.OpType;

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
  public Pair<Asset, Content> findOrCreateAsset(
      final String assetPath,
      final AttributesMapAdapter helmAttributes,
      final Supplier<InputStream> contentSupplier,
      final String contentType)
  {
    Optional<Asset> assetOpt = findAsset(assetPath);
    Asset asset = assetOpt.orElseGet(() ->
        createAsset(assetPath, helmAttributes));

      Content content =
        saveAsset(asset, contentSupplier, contentType, helmAttributes.toAttributeMap(asset.formatAttributes()));

    return Pair.of(asset, content);
  }

  private Asset createAsset(
      final String assetPath,
      final AttributesMapAdapter helmAttributes)
  {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset;
    if (helmAttributes.getAssetKind() == AssetKind.HELM_PACKAGE){
      asset = tx.createAsset(bucket, findOrCreateComponent(helmAttributes));
    }
    else {
      asset = tx.createAsset(bucket, getRepository().getFormat());
    }
    helmAttributes.toAttributeMap(asset.formatAttributes());
    asset.name(assetPath);
    tx.saveAsset(asset);
    return asset;
  }

  public Optional<Pair<String, AttributesMap>> parsePayload(Payload payload) {
    if (payload instanceof Content) {
      String contentType = payload.getContentType();
      AttributesMap contentAttributes = ((Content) payload).getAttributes();
      return Optional.of(Pair.of(contentType, contentAttributes));
    }
    return Optional.empty();
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

  private Component findOrCreateComponent(final AttributesMapAdapter attributes)
  {
    return findComponent(attributes.getName(), attributes.getVersion()).orElseGet(() -> createComponent(attributes));
  }

  private Component createComponent(final AttributesMapAdapter attributes) {
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
  public Iterator<Asset> getAllAssets()
  {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    Builder builder = builder()
        .where(P_COMPONENT).isNotNull();
    Query query = builder.build();

    return tx.browseAssets(query, bucket).iterator();
  }

  /**
   * Save an asset and create blob.
   *
   * @return blob content
   */
  //private Content saveAsset(
  //    final Asset asset,
  //    final Supplier<InputStream> contentSupplier,
  //    final Payload payload) throws IOException
  //{
  //
  //  return saveAsset(asset, contentSupplier, contentType, contentAttributes);
  //}

  /**
   * Save an asset and create blob.
   *
   * @return blob content
   */
  private Content saveAsset(
      Asset asset,
      final Supplier<InputStream> contentSupplier,
      @Nullable final String contentType,
      @Nullable final NestedAttributesMap contentAttributes)
  {
    Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));
    StorageTx tx = UnitOfWork.currentTx();
    AssetBlob assetBlob = null;
    try {
      assetBlob = tx.setBlob(
          asset, asset.name(), contentSupplier, HelmFormat.HASH_ALGORITHMS, null, contentType, false
      );
    }
    catch (IOException ex) {
      log.error("Unable to write blob", ex);
    }
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
