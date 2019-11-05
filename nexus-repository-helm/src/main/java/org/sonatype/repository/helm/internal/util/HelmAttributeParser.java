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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.repository.helm.AttributesMapAdapter;
import org.sonatype.repository.helm.internal.AssetKind;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 0.0.2
 */
@Named
@Singleton
public class HelmAttributeParser
{
  private TgzParser tgzParser;
  private YamlParser yamlParser;

  @Inject
  public HelmAttributeParser(final TgzParser tgzParser,
                             final YamlParser yamlParser) {
    this.tgzParser = checkNotNull(tgzParser);
    this.yamlParser = checkNotNull(yamlParser);

  }

  public AttributesMapAdapter getAttributesFromInputStream(final AssetKind assetKind, final InputStream inputStream) throws IOException {
    return new AttributesMapAdapter(assetKind, getAttributesFromInputStream(inputStream));
  }

  private AttributesMap getAttributesFromInputStream(final InputStream inputStream) throws IOException {
    try (InputStream is = tgzParser.getChartFromInputStream(inputStream)) {
      Map<String, Object> attributes = yamlParser.load(is);
      return new AttributesMap(attributes);
    }
  }
}
