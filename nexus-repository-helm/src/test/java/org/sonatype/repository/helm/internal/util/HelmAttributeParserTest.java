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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.internal.AssetKind;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class HelmAttributeParserTest
    extends TestSupport
{
  private YamlParser yamlParser;

  private TgzParser tgzParser;

  private ProvenanceParser provenanceParser;

  private HelmAttributeParser underTest;

  @Before
  public void setUp() throws Exception {
    yamlParser = new YamlParser();
    tgzParser = new TgzParser();
    provenanceParser = new ProvenanceParser();
    underTest = new HelmAttributeParser(tgzParser, yamlParser, provenanceParser);
  }

  @Test
  public void testGetAttributesFromChart() throws Exception {
    String name = "mongodb-0.4.9.tgz";
    InputStream chart = getClass().getResourceAsStream(name);
    AssetKind assetKind = AssetKind.getAssetKindByFileName(name);
    HelmAttributes result = underTest.getAttributes(assetKind, chart);

    assertThat(result.getName(), is(notNullValue()));
    assertThat(result.getVersion(), is(notNullValue()));
    assertThat(result.getDescription(), is(notNullValue()));
    assertThat(result.getIcon(), is(notNullValue()));
    assertThat(result.getMaintainers(), is(notNullValue()));
    assertThat(result.getSources(), is(notNullValue()));
  }
}
