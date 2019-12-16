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

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.api.model.AbstractRepositoryApiRequest;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;
import org.sonatype.repository.helm.internal.HelmFormat;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HelmResourceITTest
    extends HelmITSupport
{
  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-helm")
    );
  }

  @Before
  public void setUp() throws Exception {
    BaseUrlHolder.set(this.nexusUrl.toString());
    repos.createHelmHosted("helm-hosted-test");
    //client = createHelmClient(helmHosted);
  }

  @Test
  public void createProxy() throws Exception {
    AbstractRepositoryApiRequest request = createProxyRequest(true);
    Response response = post(getCreateRepositoryPathFor(ProxyType.NAME), request);
    assertEquals(response.getStatus(), Status.CREATED.getStatusCode());

    Repository repository = repositoryManager.get(request.getName());
    assertNotNull(repository);
    assertEquals(repository.getFormat().getValue(), HelmFormat.NAME);
    assertEquals(repository.getType().getValue(), ProxyType.NAME);

    repositoryManager.delete(request.getName());
  }

  @Test
  public void createProxy_noAuthc() throws Exception {
    setBadCredentials();
    AbstractRepositoryApiRequest request = createProxyRequest(true);
    Response response = post(getCreateRepositoryPathFor(ProxyType.NAME), request);
    assertEquals(response.getStatus(), Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void createProxy_noAuthz() throws Exception {
    setUnauthorizedUser();
    AbstractRepositoryApiRequest request = createProxyRequest(true);
    Response response = post(getCreateRepositoryPathFor(ProxyType.NAME), request);
    assertEquals(response.getStatus(), Status.FORBIDDEN.getStatusCode());
  }

  @Test
  public void createHosted() throws Exception {
    AbstractRepositoryApiRequest request = createHostedRequest(true);
    Response response = post(getCreateRepositoryPathFor(HostedType.NAME), request);
    assertEquals(response.getStatus(), Status.CREATED.getStatusCode());

    Repository repository = repositoryManager.get(request.getName());
    assertNotNull(repository);
    assertEquals(repository.getFormat().getValue(), HelmFormat.NAME);
    assertEquals(repository.getType().getValue(), HostedType.NAME);

    repositoryManager.delete(request.getName());
  }

  @Test
  public void createHosted_noAuthc() throws Exception {
    setBadCredentials();
    AbstractRepositoryApiRequest request = createHostedRequest(true);
    Response response = post(getCreateRepositoryPathFor(HostedType.NAME), request);
    assertEquals(response.getStatus(), Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void createHosted_noAuthz() throws Exception {
    setUnauthorizedUser();
    AbstractRepositoryApiRequest request = createHostedRequest(true);
    Response response = post(getCreateRepositoryPathFor(HostedType.NAME), request);
    assertEquals(response.getStatus(), Status.FORBIDDEN.getStatusCode());
  }

  @Test
  public void updateProxy() throws Exception {
    repos.createHelmProxy(PROXY_NAME, "http://example.com");

    AbstractRepositoryApiRequest request = createProxyRequest(false);

    Response response = put(getUpdateRepositoryPathFor(ProxyType.NAME, PROXY_NAME), request);
    assertEquals(response.getStatus(), Status.NO_CONTENT.getStatusCode());

    Repository repository = repositoryManager.get(request.getName());
    assertNotNull(repository);

    assertThat(repository.getConfiguration().attributes("storage")
            .get("strictContentTypeValidation"),
        is(false));
    repositoryManager.delete(PROXY_NAME);
  }

  @Test
  public void updateProxy_noAuthc() throws Exception {
    setBadCredentials();
    AbstractRepositoryApiRequest request = createProxyRequest(false);

    Response response = put(getUpdateRepositoryPathFor(ProxyType.NAME, PROXY_NAME), request);
    assertEquals(response.getStatus(), Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void updateProxy_noAuthz() throws Exception {
    repos.createHelmProxy(PROXY_NAME, "http://example.com");

    setUnauthorizedUser();
    AbstractRepositoryApiRequest request = createProxyRequest(false);

    Response response = put(getUpdateRepositoryPathFor(ProxyType.NAME, PROXY_NAME), request);
    assertEquals(response.getStatus(), Status.FORBIDDEN.getStatusCode());
  }

  @Test
  public void updateHosted() throws Exception {
    repos.createHelmHosted(HOSTED_NAME);

    AbstractRepositoryApiRequest request = createHostedRequest(false);

    Response response = put(getUpdateRepositoryPathFor(HostedType.NAME, HOSTED_NAME), request);
    assertEquals(response.getStatus(), Status.NO_CONTENT.getStatusCode());

    Repository repository = repositoryManager.get(request.getName());
    assertNotNull(repository);

    assertThat(repository.getConfiguration().attributes("storage")
            .get("strictContentTypeValidation"),
        is(false));

    repositoryManager.delete(HOSTED_NAME);
  }

  @Test
  public void updateHosted_noAuthc() throws Exception {
    setBadCredentials();
    AbstractRepositoryApiRequest request = createHostedRequest(false);

    Response response = put(getUpdateRepositoryPathFor(HostedType.NAME, HOSTED_NAME), request);
    assertEquals(response.getStatus(), Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void updateHosted_noAuthz() throws Exception {
    repos.createHelmHosted(HOSTED_NAME);

    setUnauthorizedUser();
    AbstractRepositoryApiRequest request = createHostedRequest(false);

    Response response = put(getUpdateRepositoryPathFor(HostedType.NAME, HOSTED_NAME), request);
    assertEquals(response.getStatus(), Status.FORBIDDEN.getStatusCode());
  }
}
