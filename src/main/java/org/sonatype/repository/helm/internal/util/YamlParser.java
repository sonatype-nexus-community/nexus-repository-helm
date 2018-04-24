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

import javax.inject.Named;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility methods for getting attributes from yaml files
 *
 * @since 0.0.1
 */
@Named
@Singleton
public class YamlParser
{
  private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  public Map<String, Object> load(InputStream is) throws IOException {
    checkNotNull(is);
    String data = IOUtils.toString(new UnicodeReader(is));

    Map<String, Object> map;

    try {
      Yaml yaml = new Yaml(new Constructor(), new Representer(),
          new DumperOptions(), new Resolver());
      map = (Map<String, Object>) yaml.load(data);
    }
    catch (YAMLException e) {
      map = (Map<String, Object>) mapper.readValue(data, Map.class);
    }
    return map;
  }
}
