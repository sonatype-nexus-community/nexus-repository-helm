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
package org.sonatype.nexus.plugins.helm.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.api.model.AbstractRepositoryApiRequest;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.repository.helm.internal.HelmFormat;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.sonatype.nexus.plugins.helm.HelmITConfig.configureHelmBase;

public class HelmResourceIT
    extends ResourceITSupport
{
  @Configuration
  public static Option[] configureNexus() {
    return configureHelmBase();
  }

  @Before
  public void before() {
    BaseUrlHolder.set(this.nexusUrl.toString());
  }

  @Test
  public void createProxy() throws Exception {
    AbstractRepositoryApiRequest request = createProxyRequest(true);
    Response response = post(getCreateRepositoryPathUrl(ProxyType.NAME), request);
    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

    Repository repository = repositoryManager.get(request.getName());
    assertNotNull(repository);
    assertEquals(HelmFormat.NAME, repository.getFormat().getValue());
    assertEquals(ProxyType.NAME, repository.getType().getValue());

    repositoryManager.delete(request.getName());
  }

  @Test
  public void createProxy_noAuthc() throws Exception {
    setBadCredentials();
    AbstractRepositoryApiRequest request = createProxyRequest(true);
    Response response = post(getCreateRepositoryPathUrl(ProxyType.NAME), request);
    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
  }

  @Test
  public void createProxy_noAuthz() throws Exception {
    setUnauthorizedUser();
    AbstractRepositoryApiRequest request = createProxyRequest(true);
    Response response = post(getCreateRepositoryPathUrl(ProxyType.NAME), request);
    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
  }

  @Test
  public void createHosted() throws Exception {
    AbstractRepositoryApiRequest request = createHostedRequest(true);
    Response response = post(getCreateRepositoryPathUrl(HostedType.NAME), request);
    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

    Repository repository = repositoryManager.get(request.getName());
    assertNotNull(repository);
    assertEquals(HelmFormat.NAME, repository.getFormat().getValue());
    assertEquals(HostedType.NAME, repository.getType().getValue());

    repositoryManager.delete(request.getName());
  }

  @Test
  public void createHosted_noAuthc() throws Exception {
    setBadCredentials();
    AbstractRepositoryApiRequest request = createHostedRequest(true);
    Response response = post(getCreateRepositoryPathUrl(HostedType.NAME), request);
    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
  }

  @Test
  public void createHosted_noAuthz() throws Exception {
    setUnauthorizedUser();
    AbstractRepositoryApiRequest request = createHostedRequest(true);
    Response response = post(getCreateRepositoryPathUrl(HostedType.NAME), request);
    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
  }

  @Test
  public void updateProxy() throws Exception {
    repos.createHelmProxy(PROXY_NAME, "http://example.com");

    AbstractRepositoryApiRequest request = createProxyRequest(false);

    Response response = put(getUpdateRepositoryPathUrl(ProxyType.NAME, PROXY_NAME), request);
    assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

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

    Response response = put(getUpdateRepositoryPathUrl(ProxyType.NAME, PROXY_NAME), request);
    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
  }

  @Test
  public void updateProxy_noAuthz() throws Exception {
    repos.createHelmProxy(PROXY_NAME, "http://example.com");

    setUnauthorizedUser();
    AbstractRepositoryApiRequest request = createProxyRequest(false);

    Response response = put(getUpdateRepositoryPathUrl(ProxyType.NAME, PROXY_NAME), request);
    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
  }

  @Test
  public void updateHosted() throws Exception {
    repos.createHelmHosted(HOSTED_NAME);

    AbstractRepositoryApiRequest request = createHostedRequest(false);

    Response response = put(getUpdateRepositoryPathUrl(HostedType.NAME, HOSTED_NAME), request);
    assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

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

    Response response = put(getUpdateRepositoryPathUrl(HostedType.NAME, HOSTED_NAME), request);
    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
  }

  @Test
  public void updateHosted_noAuthz() throws Exception {
    repos.createHelmHosted(HOSTED_NAME);

    setUnauthorizedUser();
    AbstractRepositoryApiRequest request = createHostedRequest(false);

    Response response = put(getUpdateRepositoryPathUrl(HostedType.NAME, HOSTED_NAME), request);
    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
  }
}
