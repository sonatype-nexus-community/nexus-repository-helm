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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.thread.io.StreamCopier;

import org.apache.http.client.utils.URIBuilder;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.events.CollectionEndEvent;
import org.yaml.snakeyaml.events.CollectionStartEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.ScalarEvent;

import static org.sonatype.repository.helm.internal.util.HelmDataAccess.HASH_ALGORITHMS;

/**
 * Removes absolute URL entries from index.yaml
 *
 * @since 0.0.1
 */
@Named
@Singleton
public class IndexYamlAbsoluteUrlRewriter
    extends ComponentSupport
{
  private static final String URLS = "urls";

  private static final String HTTP = "http://";

  private static final String HTTPS = "https://";

  private StorageFacet storageFacet;

  public TempBlob removeUrlsFromIndexYamlAndWriteToTempBlob(final TempBlob index,
                                                            final Repository repository)
  {
    storageFacet = repository.facet(StorageFacet.class);

    return new StreamCopier<>(outputStream -> updateUrls(index.get(), outputStream),
        this::createTempBlob).read();
  }

  private void updateUrls(final InputStream is,
                          final OutputStream os)
  {
    try (Reader reader = new InputStreamReader(is);
         Writer writer = new OutputStreamWriter(os)) {
      Yaml yaml = new Yaml();
      Emitter emitter = new Emitter(writer, new DumperOptions());
      boolean rewrite = false;
      for (Event event : yaml.parse(reader)) {
        if (event instanceof ScalarEvent) {
          ScalarEvent scalarEvent = (ScalarEvent) event;
          if (rewrite) {
            event = maybeSetAbsoluteUrlAsRelative(scalarEvent);
          }
          else if (URLS.equals(scalarEvent.getValue())) {
            rewrite = true;
          }
        }
        else if (event instanceof CollectionStartEvent) {
          // NOOP
        }
        else if (event instanceof CollectionEndEvent) {
          rewrite = false;
        }
        emitter.emit(event);
      }
    }
    catch (IOException ex) {
      log.debug("Error rewriting urls in index.yaml", ex);
    }
  }

  private Event maybeSetAbsoluteUrlAsRelative(ScalarEvent scalarEvent) throws MalformedURLException {
    String oldUrl = scalarEvent.getValue();
    try {
      URI uri = new URIBuilder(oldUrl).build();
      if (uri.isAbsolute()) {
        String fileName = uri.getPath();
        // Strip leading slash
        if (!fileName.isEmpty()) {
            fileName = fileName.substring(1);
        }
        scalarEvent = new ScalarEvent(scalarEvent.getAnchor(), scalarEvent.getTag(),
            scalarEvent.getImplicit(), fileName, scalarEvent.getStartMark(),
            scalarEvent.getEndMark(), scalarEvent.getStyle());
      }
    }
    catch (URISyntaxException ex) {
      log.debug("Invalid URI in index.yaml", ex);
    }
    return scalarEvent;
  }

  private TempBlob createTempBlob(final InputStream is) {
    return storageFacet.createTempBlob(is, HASH_ALGORITHMS);
  }
}
