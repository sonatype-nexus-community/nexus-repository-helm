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

import java.util.List;

import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;

import org.apache.http.HttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.error;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.file;
import static org.sonatype.nexus.plugins.helm.HelmITConfig.configureHelmBase;

public class HelmProxyIT
    extends HelmITSupport
{
  private Repository repository;

  private Server server;

  @Configuration
  public static Option[] configureNexus() {
    return configureHelmBase();
  }

  @Before
  public void setup() throws Exception {
    BaseUrlHolder.set(this.nexusUrl.toString());
    server = Server.withPort(0)
        .serve("/*").withBehaviours(error(200))
        .serve("/" + MONGO_PKG_FILE_NAME_600_TGZ).withBehaviours(file(testData.resolveFile(MONGO_PKG_FILE_NAME_600_TGZ)))
        .serve("/" + YAML_FILE_NAME).withBehaviours(file(testData.resolveFile(YAML_FILE_NAME)))
        .start();
    repository = repos.createHelmProxy("helm-proxy-test", server.getUrl().toExternalForm());
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }

  @Test
  public void fetchTgzPackageFile() throws Exception {
    HelmClient client = helmClient(repository);
    client.fetch(YAML_FILE_NAME, CONTENT_TYPE_YAML);
    checkAsset(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ);
  }

  @Test
  public void fetchMetaData() throws Exception {
    checkAsset(YAML_FILE_NAME, CONTENT_TYPE_YAML);
  }

  @Test
  public void checkComponentCreated() throws Exception {
    HelmClient client = helmClient(repository);

    client.fetch(YAML_FILE_NAME, CONTENT_TYPE_YAML);
    assertFalse(componentAssetTestHelper.componentExists(repository, MONGO_PKG_NAME, MONGO_PKG_VERSION_600));
    client.fetch(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ);
    assertTrue(componentAssetTestHelper.componentExists(repository, MONGO_PKG_NAME, MONGO_PKG_VERSION_600));
  }

  @Test
  public void testDeletingComponentDeletesAllAssociatedAssets() throws Exception {
    HelmClient client = helmClient(repository);
    client.fetch(YAML_FILE_NAME, CONTENT_TYPE_YAML);
    client.fetch(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ);
    final Component component = findComponent(repository, MONGO_PKG_NAME);
    assertNotNull(component);
    assertEquals(1, componentAssetTestHelper.countComponents(repository));
    assertFalse(findAssetsByComponent(repository, component).isEmpty());

    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteComponent(component.getEntityMetadata().getId(), true);

    assertEquals(0, componentAssetTestHelper.countComponents(repository));
    assertTrue(findAssetsByComponent(repository, component).isEmpty());
  }

  @Test
  public void testDeletingRemainingAssetAlsoDeletesComponent() throws Exception {
    HelmClient client = helmClient(repository);

    client.fetch(YAML_FILE_NAME, CONTENT_TYPE_YAML);
    client.fetch(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ);
    assertTrue(componentAssetTestHelper.assetExists(repository, MONGO_PKG_FILE_NAME_600_TGZ));
    assertTrue(componentAssetTestHelper.componentExists(repository, MONGO_PKG_NAME, MONGO_PKG_VERSION_600));

    componentAssetTestHelper.removeAsset(repository, MONGO_PKG_FILE_NAME_600_TGZ);
    assertFalse(componentAssetTestHelper.assetExists(repository, MONGO_PKG_FILE_NAME_600_TGZ));
    assertFalse(componentAssetTestHelper.componentExists(repository, MONGO_PKG_NAME, MONGO_PKG_VERSION_600));
  }

  @Test
  public void shouldCacheMetadata() throws Exception {
    HelmClient client = helmClient(repository);

    client.fetch(YAML_FILE_NAME, CONTENT_TYPE_YAML);
    server.stop();
    // recreate client for avoid test freezing
    assertSuccessResponseMatches(helmClient(repository).fetch(YAML_FILE_NAME, CONTENT_TYPE_YAML), YAML_FILE_NAME);
  }

  @Test
  public void shouldCacheTgzPackageFile() throws Exception {

    HelmClient helmClient = helmClient(repository);
    helmClient.fetch(YAML_FILE_NAME, CONTENT_TYPE_YAML);
    helmClient.fetch(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ);
    server.stop();
    // recreate client for avoid test freezing
    HttpResponse response = helmClient(repository).fetch(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ);
    assertSuccessResponseMatches(response, MONGO_PKG_FILE_NAME_600_TGZ);
  }

  public void checkAsset(final String path, final String type) throws Exception {
    HelmClient client = helmClient(repository);

    assertSuccessResponseMatches(client.fetch(path, type), path);
    assertTrue(componentAssetTestHelper.assetExists(repository, path));
    List<String> assetPaths = componentAssetTestHelper.findAssetPaths(repository.getName());
    assertThat(assetPaths.get(0), is(equalTo(path)));
    assertThat(componentAssetTestHelper.contentTypeFor(repository.getName(), path), is(equalTo(type)));
    assertTrue(componentAssetTestHelper.attributes(repository, path).contains(HELM_FORMAT_NAME));
  }
}
