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
package org.sonatype.repository.helm.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.Query.Builder;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

import static java.util.Collections.singletonList;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_COMPONENT;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;
import static org.sonatype.nexus.repository.storage.Query.builder;
import static org.sonatype.repository.helm.internal.AssetKind.HELM_PACKAGE;
import static org.sonatype.repository.helm.internal.database.HelmProperties.ATTRIBUTES_HELM_ASSET_KIND;

/**
 * Shared code between Helm facets.
 */
@Named
public class HelmDataAccess
{
  public static final List<HashAlgorithm> HASH_ALGORITHMS = ImmutableList.of(SHA1, SHA256);

  /**
   * Find a component by its name and tag (version)
   *
   * @return found component or null if not found
   */
  @Nullable
  public Component findComponent(final StorageTx tx,
                                 final Repository repository,
                                 final String name,
                                 final String version)
  {
    Iterable<Component> components = tx.findComponents(
        Query.builder()
            .where(P_NAME).eq(name)
            .and(P_VERSION).eq(version)
            .build(),
        singletonList(repository)
    );
    if (components.iterator().hasNext()) {
      return components.iterator().next();
    }
    return null;
  }

  /**
   * Find assets for Helm components (charts)
   *
   * @return found assets or null if not found
   */
  @Nullable
  public Iterable<Asset> browseComponentAssets(final StorageTx tx,
                                               final Bucket bucket)
  {
    Builder builder = builder()
        .where(P_COMPONENT).isNotNull();

    Query query = builder.build();

    return tx.browseAssets(query, bucket);
  }

  /**
   * Find an asset by its name.
   *
   * @return found asset or null if not found
   */
  @Nullable
  public Asset findAsset(final StorageTx tx, final Bucket bucket, final String assetName) {
    return tx.findAssetWithProperty(MetadataNodeEntityAdapter.P_NAME, assetName, bucket);
  }

  /**
   * Save an asset and create blob.
   *
   * @return blob content
   */
  public Content saveAsset(final StorageTx tx,
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
    return saveAsset(tx, asset, contentSupplier, contentType, contentAttributes);
  }

  /**
   * Save an asset and create blob.
   *
   * @return blob content
   */
  public Content saveAsset(final StorageTx tx,
                           final Asset asset,
                           final Supplier<InputStream> contentSupplier,
                           final String contentType,
                           @Nullable final AttributesMap contentAttributes) throws IOException
  {
    Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));
    AssetBlob assetBlob = tx.setBlob(
        asset, asset.name(), contentSupplier, HASH_ALGORITHMS, null, contentType, false
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
    Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes());
    return content;
  }
}
