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

import org.sonatype.repository.helm.internal.metadata.HelmAttributes;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.repository.helm.internal.database.HelmProperties.*;

/**
 * @since 0.0.2
 */
@Named
@Singleton
public class HelmAttributeParser {
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
            if (attributes.get(DESCRIPTION) != null) {
                helmAttributes.setDescription(attributes.get(DESCRIPTION).toString());
            }
            if (attributes.get(ICON) != null) {
                helmAttributes.setIcon(attributes.get(ICON).toString());
            }
            if (attributes.get(MAINTAINERS) != null) {
                helmAttributes.setMaintainers((List<Map<String, String>>) attributes.get(MAINTAINERS));
            }
            if (attributes.get(SOURCES) != null) {
                helmAttributes.setSources((List<String>) attributes.get(SOURCES));
            }

            return helmAttributes;
        }
    }
}
