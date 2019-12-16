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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.api.model.AbstractRepositoryApiRequest;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;
import org.sonatype.repository.helm.internal.HelmFormat;

import org.apache.http.HttpResponse;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;

public class HelmHostedIT
    extends HelmITSupport
{
  private HelmClient client;

  private Repository repository;

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
    repository = repos.createHelmHosted("helm-hosted-test");
    client = createHelmClient(repository);
  }

  @Test
  public void testPackageUpload() throws IOException
  {
    uploadPackages(MONGO_PKG_FILE_NAME_600_TGZ, MONGO_PKG_FILE_NAME_728_TGZ);
    //Verify DB contains data about uploaded component and asset
    Component component = findComponent(repository, MONGO_PKG_NAME);
    assertThat(component.name(), is(equalTo(MONGO_PKG_NAME)));
    assertThat(component.version(), is(equalTo(MONGO_PKG_VERSION_600)));

    //Verify Asset is created.
    Asset asset = findAsset(repository, MONGO_PATH_FULL_600_TARGZ);
    assertThat(asset.name(), is(equalTo(MONGO_PATH_FULL_600_TARGZ)));
    assertThat(asset.format(), is(equalTo(HELM_FORMAT_NAME)));
  }

  @Test
  public void testFetchPackageOnEmptyRepository() throws Exception
  {
    HttpResponse resp = client.fetch(MONGO_PATH_FULL_728_TARGZ, CONTENT_TYPE_TGZ);
    assertThat(resp.getEntity().getContentType().getValue(), equalTo(MediaType.TEXT_HTML));
    MatcherAssert.assertThat(status(resp), is(NOT_FOUND));
  }

  @Test
  public void testFetchPackage() throws Exception
  {
    uploadPackages(MONGO_PKG_FILE_NAME_600_TGZ, MONGO_PKG_FILE_NAME_728_TGZ);
    HttpResponse resp = client.fetch(MONGO_PATH_FULL_728_TARGZ, CONTENT_TYPE_TGZ);
    assertThat(resp.getEntity().getContentType().getValue(), equalTo(CONTENT_TYPE_TGZ));
    assertSuccessResponseMatches(resp, MONGO_PKG_FILE_NAME_728_TGZ);
  }

  @Test
  public void testMetadataProcessing() throws Exception
  {
    uploadPackages(MONGO_PKG_FILE_NAME_600_TGZ, MONGO_PKG_FILE_NAME_728_TGZ);
    // We need to wait for 1.3 sec after packages are uploaded at #setUp because of CreateIndexFacetImpl#maybeWait()
    TimeUnit.SECONDS.sleep(2);
    // Verify metadata contains appropriate content about helm package.
    final InputStream content = client.fetch(YAML_FILE_NAME, CONTENT_TYPE_YAML).getEntity().getContent();

    checkYamlIncludesContent(content, YAML_MONGO_728_STRING_DATA);

    //Verify metadata is clean if component has been deleted
    List<Component> components = getAllComponents(repository);
    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteComponent(components.get(1).getEntityMetadata().getId());

    //Sleeping again to rebuild index.yaml
    TimeUnit.SECONDS.sleep(2);
    final InputStream contentAfterDelete = client.fetch(YAML_FILE_NAME, CONTENT_TYPE_YAML).getEntity().getContent();
    checkYamlIncludesContent(contentAfterDelete, YAML_MONGO_600_STRING_DATA);
  }

  @Test
  public void testDeletingRemainingAssetAlsoDeletesComponent() throws IOException {
    uploadPackages(MONGO_PKG_FILE_NAME_600_TGZ, MONGO_PKG_FILE_NAME_728_TGZ);
    final Asset asset = findAsset(repository, MONGO_PATH_FULL_600_TARGZ);
    assertNotNull(asset);
    assertNotNull(asset.componentId());

    final Component component = findComponentById(repository, asset.componentId());
    assertNotNull(component);
    assertEquals(1, findAssetsByComponent(repository, component).size());

    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteAsset(asset.getEntityMetadata().getId(), true);

    assertNull(findAsset(repository, MONGO_PATH_FULL_600_TARGZ));
    assertNull(findComponentById(repository, asset.componentId()));
  }

  @Test
  public void testDeletingComponentDeletesAllAssociatedAssets() throws IOException {
    uploadPackages(MONGO_PKG_FILE_NAME_600_TGZ, MONGO_PKG_FILE_NAME_728_TGZ);
    final Asset asset = findAsset(repository, MONGO_PATH_FULL_600_TARGZ);
    assertNotNull(asset);
    assertNotNull(asset.componentId());

    final Component component = findComponentById(repository, asset.componentId());
    assertNotNull(component);

    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteComponent(component.getEntityMetadata().getId(), true);

    assertNull(findAsset(repository, MONGO_PATH_FULL_600_TARGZ));
    assertNull(findComponentById(repository, asset.componentId()));
  }

  private void uploadPackages(String... names) throws IOException {
    assertThat(getAllComponents(repository), hasSize(0));
    for (String name : names) {
      uploadSinglePackage(name);
    }
    assertThat(getAllComponents(repository), hasSize(names.length));
  }

  private void uploadSinglePackage(String name) throws IOException {
    client.put(format("%s/%s", PKG_PATH, name), fileToHttpEntity(name));
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
