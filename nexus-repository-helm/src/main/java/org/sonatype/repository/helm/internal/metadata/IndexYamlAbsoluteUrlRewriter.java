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
package org.sonatype.repository.helm.internal.metadata;

import java.io.InputStream;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.thread.io.StreamCopier;
import org.sonatype.repository.helm.internal.metadata.IndexYamlAbsoluteUrlRewriterSupport;

import static org.sonatype.repository.helm.internal.HelmFormat.HASH_ALGORITHMS;

/**
 * Removes absolute URL entries from index.yaml
 *
 * @since 0.0.1
 */
@Named
@Singleton
public class IndexYamlAbsoluteUrlRewriter
    extends IndexYamlAbsoluteUrlRewriterSupport
{
  private StorageFacet storageFacet;

  public TempBlob removeUrlsFromIndexYamlAndWriteToTempBlob(final TempBlob index,
                                                            final Repository repository)
  {
    storageFacet = repository.facet(StorageFacet.class);

    return new StreamCopier<>(outputStream -> updateUrls(index.get(), outputStream),
        this::createTempBlob).read();
  }

  private TempBlob createTempBlob(final InputStream is) {
    return storageFacet.createTempBlob(is, HASH_ALGORITHMS);
  }
}
