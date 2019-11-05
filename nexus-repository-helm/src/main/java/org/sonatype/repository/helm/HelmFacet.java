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

import java.io.InputStream;
import java.util.Iterator;
import java.util.Optional;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

import com.google.common.base.Supplier;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @since 1.0.next
 */
@Facet.Exposed
public interface HelmFacet
    extends Facet
{
  Iterator<Asset> getAllAssets();

  Optional<Asset> findAsset(final String assetName);

  Optional<Pair<String, AttributesMap>> parsePayload(Payload payload);

  Pair<Asset, Content> findOrCreateAsset(
      final String assetPath,
      final AttributesMapAdapter helmAttributes,
      final Supplier<InputStream> contentSupplier,
      final String contentType);

  Content toContent(final Asset asset, final Blob blob);
}
