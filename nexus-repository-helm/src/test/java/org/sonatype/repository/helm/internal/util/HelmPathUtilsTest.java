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

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.assertThat;

public class HelmPathUtilsTest
    extends TestSupport
{
  private static final String FILENAME = "mongodb-0.4.9.tgz";

  private HelmPathUtils underTest;

  @Mock
  private TokenMatcher.State state;

  @Before
  public void setUp() throws Exception {
    underTest = new HelmPathUtils();
  }

  @Test
  public void filename() throws Exception {
    Map<String, String> map = new HashMap<>();
    map.put("filename", FILENAME);
    when(state.getTokens()).thenReturn(map);
    String result = underTest.filename(state);
    assertThat(result, is(equalTo(FILENAME)));
  }
}
