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
import org.sonatype.nexus.repository.storage.TempBlob;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

public class TgzParserTest
    extends TestSupport
{
  private TgzParser underTest;

  @Mock
  private TempBlob tempBlob;

  @Before
  public void setUp() throws Exception {
    this.underTest = new TgzParser();
  }

  @Test
  public void getYamlFromTgzTest() throws Exception {
    InputStream is = getClass().getResourceAsStream("mongodb-0.4.9.tgz");
    when(tempBlob.get()).thenReturn(is);

    InputStream yaml = underTest.getChartFromTempBlob(tempBlob);
    assertThat(yaml, is(instanceOf(InputStream.class)));
  }
}
