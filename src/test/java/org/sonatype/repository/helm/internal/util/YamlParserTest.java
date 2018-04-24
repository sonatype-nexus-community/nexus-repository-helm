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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class YamlParserTest
    extends TestSupport
{
  private YamlParser underTest;

  @Before
  public void setUp() throws Exception {
    this.underTest = new YamlParser();
  }

  @Test
  public void testParseChartYaml() throws Exception {
    InputStream is = getClass().getResourceAsStream("Chart.yaml");
    Map<String, Object> helmYaml = underTest.load(is);

    assertThat(helmYaml.get("version").toString(), is(equalTo("0.4.9")));
    assertThat(helmYaml.get("name").toString(), is(equalTo("mongodb")));
    assertThat(helmYaml.get("home").toString(), is(equalTo("https://mongodb.org")));
    assertThat(helmYaml.get("description").toString(), is(equalTo("NoSQL document-oriented database that stores JSON-like documents with" +
        " dynamic schemas, simplifying the integration of data in content-driven applications.")));
    assertThat(helmYaml.get("engine").toString(), is(equalTo("gotpl")));
    assertThat(helmYaml.get("icon").toString(), is(equalTo("https://bitnami.com/assets/stacks/mongodb/img/mongodb-stack-220x234.png")));
    assertThat(helmYaml.get("keywords"), is(equalTo(getKeywords())));
    assertThat(helmYaml.get("maintainers"), is(equalTo(getMaintainers())));
    assertThat(helmYaml.get("sources"), is(equalTo(getSources())));
  }

  private List<String> getKeywords() {
    List<String> list = new ArrayList<>();
    list.add("mongodb");
    list.add("database");
    list.add("nosql");
    return list;
  }

  private List<Map<String, Object>> getMaintainers() {
    List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();
    Map<String, Object> map = new HashMap<String, Object>() {
      {
        put("email", "containers@bitnami.com");
        put("name", "Bitnami");
      }
    };
    listMap.add(map);
    return listMap;
  }

  private List<String> getSources() {
    List<String> list = new ArrayList<>();
    list.add("https://github.com/bitnami/bitnami-docker-mongodb");
    return list;
  }
}
