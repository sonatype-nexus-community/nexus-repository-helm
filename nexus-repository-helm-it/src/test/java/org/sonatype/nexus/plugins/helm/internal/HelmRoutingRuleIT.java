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
package org.sonatype.nexus.plugins.helm.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.httpfixture.server.api.Behaviour;
import org.sonatype.goodies.httpfixture.server.fluent.Behaviours;
import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.testsuite.testsupport.FormatClientSupport;
import org.sonatype.nexus.testsuite.testsupport.fixtures.RoutingRuleRule;
import org.sonatype.repository.helm.internal.HelmFormat;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class HelmRoutingRuleIT
    extends HelmITSupport
{
  @Inject
  private RoutingRuleStore ruleStore;

  @Rule
  public RoutingRuleRule routingRules = new RoutingRuleRule(() -> ruleStore);

  private static Server proxyServer;

  private Map<String, BehaviourSpy> serverPaths = new HashMap<>();

  @Configuration
  public static Option[] configureNexus() {
    return options(
        configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", String.format("nexus-repository-%s", HelmFormat.NAME))
    );
  }

  @Before
  public void startup() throws Exception {
    proxyServer = Server.withPort(0).start();
  }

  @After
  public void shutdown() throws Exception {
    if (proxyServer != null) {
      proxyServer.stop();
    }
  }

  @Test
  public void testBlockedRoutingRule() throws Exception {
    String packageFileName = MONGO_PKG_FILE_NAME_728_TGZ;
    String blocked = "1.1";
    String allowedPackagePath = String.format("bin/macosx/el-capitan/contrib/3.6/%s", packageFileName);
    String blockedPackagePath = String.format("bin/macosx/el-capitan/contrib/%s/%s", blocked, packageFileName);

    configureProxyBehaviour(allowedPackagePath, packageFileName);
    configureProxyBehaviour(blockedPackagePath, packageFileName);

    EntityId routingRuleId = createBlockedRoutingRule(String.format("%s-blocking-rule", HelmFormat.NAME), String.format(".*/%s/.*", blocked));
    Repository proxyRepo =
        repos.createHelmProxy(String.format("test-%s-blocking-proxy", HelmFormat.NAME), proxyServer.getUrl().toString());
    FormatClientSupport client = helmClient(proxyRepo);

    attachRuleToRepository(proxyRepo, routingRuleId);

    assertGetResponseStatus(client, proxyRepo, blockedPackagePath, 403);
    assertGetResponseStatus(client, proxyRepo, allowedPackagePath, 200);
    assertNoRequests(blockedPackagePath);
  }

  private void assertNoRequests(final String reqPath) {
    BehaviourSpy spy = serverPaths.get(reqPath);
    assertNotNull("Missing spy for " + reqPath, spy);
    assertFalse("Unexpected request: " + reqPath,
        spy.requestUris.stream().anyMatch(reqPath::endsWith));
  }

  private void configureProxyBehaviour(final String proxyPath, final String fileName) {
    File file = resolveTestFile(fileName);
    BehaviourSpy spy = new BehaviourSpy(Behaviours.file(file));
    proxyServer.serve("/" + proxyPath).withBehaviours(spy);
    serverPaths.put(proxyPath, spy);
  }

  private EntityId createBlockedRoutingRule(final String name, final String matcher) {
    return routingRules.create(name, matcher).id();
  }

  private void attachRuleToRepository(final Repository repository, final EntityId routingRuleId) throws Exception {
    org.sonatype.nexus.repository.config.Configuration configuration = repository.getConfiguration();
    configuration.setRoutingRuleId(routingRuleId);
    repositoryManager.update(configuration);
  }

  protected static class BehaviourSpy
      implements Behaviour
  {
    private static final String REQUEST_URI_PATTERN = "%s?%s";

    private Behaviour delegate;

    List<String> requestUris = new ArrayList<>();

    BehaviourSpy(final Behaviour delegate) {
      this.delegate = delegate;
    }

    @Override
    public boolean execute(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Map<Object, Object> ctx) throws Exception
    {
      requestUris.add(String.format(REQUEST_URI_PATTERN, request.getRequestURI(), request.getQueryString()));
      return delegate.execute(request, response, ctx);
    }
  }
}
