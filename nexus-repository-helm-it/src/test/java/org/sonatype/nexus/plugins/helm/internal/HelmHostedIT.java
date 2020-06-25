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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.plugins.helm.HelmITConfig.configureHelmBase;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;

public class HelmHostedIT
    extends HelmITSupport
{
  private HelmClient client;

  private Repository repository;

  @Configuration
  public static Option[] configureNexus() {
    return configureHelmBase();
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
    //Verify DB contains data about uploaded components and assets
    assertTrue(componentAssetTestHelper.componentExists(repository, MONGO_PKG_NAME, MONGO_PKG_VERSION_600));
    assertTrue(componentAssetTestHelper.componentExists(repository, MONGO_PKG_NAME, MONGO_PKG_VERSION_728));

    //Verify Assets are created.
    assertTrue(componentAssetTestHelper.assetExists(repository, MONGO_PATH_FULL_600_TARGZ));
    assertTrue(componentAssetTestHelper.assetExists(repository, MONGO_PATH_FULL_728_TARGZ));
    List<String> assetPaths = componentAssetTestHelper.findAssetPaths(repository.getName());
    assertTrue(assetPaths.contains(MONGO_PATH_FULL_600_TARGZ));
    assertTrue(assetPaths.contains(MONGO_PATH_FULL_728_TARGZ));
    assertThat(componentAssetTestHelper.contentTypeFor(repository.getName(), MONGO_PATH_FULL_600_TARGZ),
        is(equalTo(CONTENT_TYPE_TGZ)));
    assertThat(componentAssetTestHelper.contentTypeFor(repository.getName(), MONGO_PATH_FULL_728_TARGZ),
        is(equalTo(CONTENT_TYPE_TGZ)));
    assertTrue(
        componentAssetTestHelper.attributes(repository, MONGO_PATH_FULL_600_TARGZ).contains(HELM_FORMAT_NAME));
    assertTrue(
        componentAssetTestHelper.attributes(repository, MONGO_PATH_FULL_728_TARGZ).contains(HELM_FORMAT_NAME));
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
    componentAssetTestHelper.deleteComponent(repository, null, MONGO_PKG_NAME, MONGO_PKG_VERSION_600);

    //Sleeping again to rebuild index.yaml
    TimeUnit.SECONDS.sleep(2);
    final InputStream contentAfterDelete = client.fetch(YAML_FILE_NAME, CONTENT_TYPE_YAML).getEntity().getContent();
    checkYamlIncludesContent(contentAfterDelete, YAML_MONGO_600_STRING_DATA);
  }

  @Test
  public void testDeletingRemainingAssetAlsoDeletesComponent() throws IOException {
    uploadPackages(MONGO_PKG_FILE_NAME_600_TGZ, MONGO_PKG_FILE_NAME_728_TGZ);
    assertTrue(componentAssetTestHelper.assetExists(repository, MONGO_PATH_FULL_600_TARGZ));
    assertTrue(componentAssetTestHelper.assetExists(repository, MONGO_PATH_FULL_728_TARGZ));
    assertTrue(componentAssetTestHelper.componentExists(repository, MONGO_PKG_NAME, MONGO_PKG_VERSION_600));
    assertTrue(componentAssetTestHelper.componentExists(repository, MONGO_PKG_NAME, MONGO_PKG_VERSION_728));

    componentAssetTestHelper.removeAsset(repository, MONGO_PATH_FULL_600_TARGZ);
    assertFalse(componentAssetTestHelper.assetExists(repository, MONGO_PATH_FULL_600_TARGZ));
    assertTrue(componentAssetTestHelper.assetExists(repository, MONGO_PATH_FULL_728_TARGZ));
    assertFalse(componentAssetTestHelper.componentExists(repository, MONGO_PKG_NAME, MONGO_PKG_VERSION_600));
    assertTrue(componentAssetTestHelper.componentExists(repository, MONGO_PKG_NAME, MONGO_PKG_VERSION_728));
  }

  @Test
  public void testDeletingComponentDeletesAllAssociatedAssets() throws IOException {
    uploadPackages(MONGO_PKG_FILE_NAME_600_TGZ, MONGO_PKG_FILE_NAME_728_TGZ);
    final Component component = findComponent(repository, MONGO_PKG_NAME);
    assertNotNull(component);
    assertEquals(2, componentAssetTestHelper.countComponents(repository));
    assertFalse(findAssetsByComponent(repository, component).isEmpty());

    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteComponent(component.getEntityMetadata().getId(), true);

    assertEquals(1, componentAssetTestHelper.countComponents(repository));
    assertTrue(findAssetsByComponent(repository, component).isEmpty());
  }

  private void uploadPackages(String... names) throws IOException {
    assertThat(componentAssetTestHelper.findAssetPaths(repository.getName()), hasSize(0));
    for (String name : names) {
      uploadSinglePackage(name);
    }
    assertThat(componentAssetTestHelper.findAssetPaths(repository.getName()), hasSize(names.length));
  }

  private void uploadSinglePackage(String name) throws IOException {
    client.put(format("%s/%s", PKG_PATH, name), fileToHttpEntity(name));
  }
}
