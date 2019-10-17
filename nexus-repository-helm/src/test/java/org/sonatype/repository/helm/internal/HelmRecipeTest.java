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
package org.sonatype.repository.helm.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.handlers.HighAvailabilitySupportChecker;
import org.sonatype.repository.helm.internal.hosted.HelmHostedRecipe;
import org.sonatype.repository.helm.internal.proxy.HelmProxyRecipe;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HelmRecipeTest
    extends TestSupport
{
  private static final String FORMAT_NAME = HelmFormat.NAME;

  @Mock
  private HelmFormat helmFormat;

  @Mock
  private HighAvailabilitySupportChecker highAvailabilitySupportChecker;

  private HelmProxyRecipe helmProxyRecipe;

  private HelmHostedRecipe helmHostedRecipe;

  @Before
  public void setUp() {
    when(helmFormat.getValue()).thenReturn(FORMAT_NAME);
    helmProxyRecipe = new HelmProxyRecipe(new ProxyType(), helmFormat);
    helmHostedRecipe = new HelmHostedRecipe(new HostedType(), helmFormat);
    helmProxyRecipe.setHighAvailabilitySupportChecker(highAvailabilitySupportChecker);
    helmHostedRecipe.setHighAvailabilitySupportChecker(highAvailabilitySupportChecker);
  }

  @Test
  public void haEnabledHostedRepository() {
    when(highAvailabilitySupportChecker.isSupported(FORMAT_NAME)).thenReturn(true);
    assertThat(helmHostedRecipe.isFeatureEnabled(), is(equalTo(true)));
    verify(highAvailabilitySupportChecker).isSupported(FORMAT_NAME);
  }

  @Test
  public void haDisabledHostedRepository() {
    when(highAvailabilitySupportChecker.isSupported(FORMAT_NAME)).thenReturn(false);
    assertThat(helmHostedRecipe.isFeatureEnabled(), is(equalTo(false)));
    verify(highAvailabilitySupportChecker).isSupported(FORMAT_NAME);
  }

  @Test
  public void haEnabledProxyRepository() {
    when(highAvailabilitySupportChecker.isSupported(FORMAT_NAME)).thenReturn(true);
    assertThat(helmProxyRecipe.isFeatureEnabled(), is(equalTo(true)));
    verify(highAvailabilitySupportChecker).isSupported(FORMAT_NAME);
  }

  @Test
  public void haDisabledProxyRepository() {
    when(highAvailabilitySupportChecker.isSupported(FORMAT_NAME)).thenReturn(false);
    assertThat(helmProxyRecipe.isFeatureEnabled(), is(equalTo(false)));
    verify(highAvailabilitySupportChecker).isSupported(FORMAT_NAME);
  }
}
