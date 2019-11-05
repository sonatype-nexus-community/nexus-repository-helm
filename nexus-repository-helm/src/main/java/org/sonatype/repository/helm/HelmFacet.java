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
package org.sonatype.repository.helm;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.repository.helm.internal.AssetKind;

import com.google.common.base.Supplier;

/**
 * @since 1.0.next
 */
@Facet.Exposed
public interface HelmFacet
    extends Facet
{
  Asset findOrCreateAssetWithComponent(
      final String assetPath,
      final AssetKind assetKind,
      final StorageTx tx,
      final Bucket bucket,
      final HelmAttributes chart);

  Asset findOrCreateAssetWithAttributes(
      final String assetPath,
      final AssetKind assetKind,
      final StorageTx tx,
      final Bucket bucket,
      final HelmAttributes chart);

  Iterable<Asset> browseComponentAssets(final StorageTx tx,
                                        final Bucket bucket);

  Asset findAsset(final StorageTx tx, final Bucket bucket, final String assetName);

  Component findOrCreateComponent(
      final StorageTx tx,
      final Bucket bucket,
      final HelmAttributes chart);

  Content saveAsset(final StorageTx tx,
                    final Asset asset,
                    final Supplier<InputStream> contentSupplier,
                    final String contentType,
                    @Nullable final AttributesMap contentAttributes) throws IOException;

  Content saveAsset(final StorageTx tx,
                    final Asset asset,
                    final Supplier<InputStream> contentSupplier,
                    final Payload payload) throws IOException;

  Content toContent(final Asset asset, final Blob blob);
}
