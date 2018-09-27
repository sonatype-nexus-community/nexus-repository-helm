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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.repository.helm.internal.metadata.HelmAttributes;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.repository.helm.internal.database.HelmProperties.APP_VERSION;
import static org.sonatype.repository.helm.internal.database.HelmProperties.DESCRIPTION;
import static org.sonatype.repository.helm.internal.database.HelmProperties.ICON;
import static org.sonatype.repository.helm.internal.database.HelmProperties.MAINTAINERS;
import static org.sonatype.repository.helm.internal.database.HelmProperties.NAME;
import static org.sonatype.repository.helm.internal.database.HelmProperties.SOURCES;
import static org.sonatype.repository.helm.internal.database.HelmProperties.VERSION;

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

  public HelmAttributes getAttributesFromInputStream(final InputStream inputStream) throws IOException {
    try (InputStream is = tgzParser.getChartFromInputStream(inputStream)) {
      Map<String, Object> attributes = yamlParser.load(is);
      HelmAttributes helmAttributes = new HelmAttributes();
      helmAttributes.setName(attributes.get(NAME).toString());
      helmAttributes.setVersion(attributes.get(VERSION).toString());
      if (null != attributes.get(DESCRIPTION)) {
        helmAttributes.setDescription(attributes.get(DESCRIPTION).toString());
      }
      if (null != attributes.get(ICON)) {
        helmAttributes.setIcon(attributes.get(ICON).toString());
      }
      if (null != attributes.get(MAINTAINERS)) {
        helmAttributes.setMaintainers((List<Map<String, String>>)attributes.get(MAINTAINERS));
      }
      if (null != attributes.get(SOURCES)) {
        helmAttributes.setSources((List<String>)attributes.get(SOURCES));
      }
      if (null != attributes.get(APP_VERSION)) {
        helmAttributes.setAppVersion(attributes.get(APP_VERSION).toString());
      }

      return helmAttributes;
    }
  }
}
