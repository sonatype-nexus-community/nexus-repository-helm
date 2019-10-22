/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2017-present Sonatype, Inc.
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

import javax.ws.rs.client.Client;
import javax.ws.rs.core.UriBuilder;

import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.rest.client.RestClientConfiguration;
import org.sonatype.nexus.script.ScriptClient;
import org.sonatype.nexus.script.ScriptXO;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ScriptResourceIT
    extends HelmITSupport
{

  private ScriptClient scriptClient;

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-helm")
    );
  }

  @Before
  public void setup() throws Exception {
    BaseUrlHolder.set(this.nexusUrl.toString());

    final CloseableHttpClient closeableHttpClient = clientBuilder().build();
    Client client2 = restClientFactory.create(
        RestClientConfiguration.DEFAULTS.withHttpClient(() -> closeableHttpClient));
    client2.register(new BasicAuthentication("admin", "admin123"));
    scriptClient = restClientFactory.proxy(ScriptClient.class, client2,
        UriBuilder.fromUri(nexusUrl.toURI()).path("service/rest").build());
  }

  @Test
  public void createHelmProxyScript() {
    String repoName = "helm-script-repo";
    String content = format("repository.createHelmProxy('%s','http://someurl')", repoName);

    scriptClient.add(new ScriptXO(repoName, content, "groovy"));
    scriptClient.run(repoName, "");
    Repository repo = repositoryManager.get(repoName);
    assertThat(repo.getFormat().getValue(), is("helm"));
    assertThat(repo.getName(), is(repoName));
    assertThat(repo.getType().getValue(), is("proxy"));
  }

  @Test
  public void createHelmHostedScript() {
    String repoName = "helm-script-hosted-repo";
    String content = format("repository.createHelmHosted('%s')", repoName);

    scriptClient.add(new ScriptXO(repoName, content, "groovy"));
    scriptClient.run(repoName, "");
    Repository repo = repositoryManager.get(repoName);
    assertThat(repo.getFormat().getValue(), is("helm"));
    assertThat(repo.getName(), is(repoName));
    assertThat(repo.getType().getValue(), is("hosted"));
  }
}
