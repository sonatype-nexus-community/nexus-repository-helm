/*
 * Copyright (c) 2008-present Sonatype, Inc.
 *
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.repository.helm.api;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.internal.RepositoryImpl;
import org.sonatype.nexus.repository.rest.api.ApiRepositoryAdapter;
import org.sonatype.nexus.repository.rest.api.SimpleApiRepositoryAdapter;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.repository.helm.internal.HelmFormat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class HelmApiRepositoryAdapterTest
    extends TestSupport
{
  private ApiRepositoryAdapter underTest;

  @Mock
  private RoutingRuleStore routingRuleStore;

  @Before
  public void setup() {
    underTest = new SimpleApiRepositoryAdapter(routingRuleStore);
    BaseUrlHolder.set("http://nexus-url");

    Configuration configuration = new Configuration();
    configuration.setOnline(true);
  }

  @Test
  public void hostedRepository() throws Exception {
    String repositoryName = "helm-hosted";
    Format format = new HelmFormat();
    Type type = new HostedType();
    boolean online = true;

    Repository repository = createRepository(repositoryName, format, type, online);
    AbstractApiRepository hostedRepository = underTest.adapt(repository);
    assertRepository(hostedRepository, repositoryName, type, online);
  }

  @Test
  public void proxyRepository() throws Exception {
    String repositoryName = "helm-proxy";
    Format format = new HelmFormat();
    Type type = new ProxyType();
    boolean online = true;

    Repository repository = createRepository(repositoryName, format, type, online);
    AbstractApiRepository hostedRepository = underTest.adapt(repository);
    assertRepository(hostedRepository, repositoryName, type, online);
  }

  private static void assertRepository(
      final AbstractApiRepository repository,
      String repositoryName,
      final Type type,
      final boolean online)
  {
    assertThat(repository.getFormat(), is(HelmFormat.NAME));
    assertThat(repository.getName(), is(repositoryName));
    assertThat(repository.getOnline(), is(online));
    assertThat(repository.getType(), is(type.getValue()));
    assertThat(repository.getUrl(), is(BaseUrlHolder.get() + "/repository/" + repositoryName));
  }

  private static Repository createRepository(String repositoryName, Format format, Type type, boolean online)
      throws Exception
  {
    Repository repository = new RepositoryImpl(Mockito.mock(EventManager.class), type, format);
    Configuration configuration = new Configuration();
    configuration.setOnline(online);
    configuration.setRepositoryName(repositoryName);
    repository.init(configuration);
    return repository;
  }
}
