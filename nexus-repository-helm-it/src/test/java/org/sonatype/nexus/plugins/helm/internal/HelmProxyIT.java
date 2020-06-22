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
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.error;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.file;
import static org.sonatype.nexus.plugins.helm.HelmITConfig.configureHelmBase;

public class HelmProxyIT
    extends HelmITSupport
{
  private HelmClient client;

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
    client = helmClient(repository);
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }

  @Test
  public void fetchTgzPackageFile() throws Exception {
    checkAsset(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ);
  }

  @Test
  public void fetchMetaData() throws Exception {
    checkAsset(YAML_FILE_NAME, CONTENT_TYPE_YAML);
  }

  @Test
  public void checkComponentCreated() throws Exception {
    Assert.assertFalse(componentAssetTestHelper.componentExists(repository, MONGO_PKG_NAME, MONGO_PKG_VERSION_600));
    client.fetch(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ);
    Assert.assertTrue(componentAssetTestHelper.componentExists(repository, MONGO_PKG_NAME, MONGO_PKG_VERSION_600));
  }

  @Test
  public void testDeletingComponentDeletesAllAssociatedAssets() throws Exception {
    client.fetch(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ);
    final Component component = findComponent(repository, MONGO_PKG_NAME);
    Assert.assertNotNull(component);
    Assert.assertEquals(1, componentAssetTestHelper.countComponents(repository));
    Assert.assertFalse(findAssetsByComponent(repository, component).isEmpty());

    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteComponent(component.getEntityMetadata().getId(), true);

    Assert.assertEquals(0, componentAssetTestHelper.countComponents(repository));
    Assert.assertTrue(findAssetsByComponent(repository, component).isEmpty());
  }

  @Test
  public void testDeletingRemainingAssetAlsoDeletesComponent() throws Exception {
    client.fetch(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ);
    Assert.assertTrue(componentAssetTestHelper.assetExists(repository, MONGO_PKG_FILE_NAME_600_TGZ));
    Assert.assertTrue(componentAssetTestHelper.componentExists(repository, MONGO_PKG_NAME, MONGO_PKG_VERSION_600));

    componentAssetTestHelper.removeAsset(repository, MONGO_PKG_FILE_NAME_600_TGZ);
    Assert.assertFalse(componentAssetTestHelper.assetExists(repository, MONGO_PKG_FILE_NAME_600_TGZ));
    Assert.assertFalse(componentAssetTestHelper.componentExists(repository, MONGO_PKG_NAME, MONGO_PKG_VERSION_600));
  }

  @Test
  public void shouldCacheMetadata() throws Exception {
    client.fetch(YAML_FILE_NAME, CONTENT_TYPE_YAML);
    server.stop();
    assertSuccessResponseMatches(client.fetch(YAML_FILE_NAME, CONTENT_TYPE_YAML), YAML_FILE_NAME);
  }

  @Test
  public void shouldCacheTgzPackageFile() throws Exception {
    client.fetch(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ);
    server.stop();
    assertSuccessResponseMatches(client.fetch(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ), MONGO_PKG_FILE_NAME_600_TGZ);
  }

  public void checkAsset(final String path, final String type) throws IOException {
    assertSuccessResponseMatches(client.fetch(path, type), path);
    Assert.assertTrue(componentAssetTestHelper.assetExists(repository, path));
    List<String> assetPaths = componentAssetTestHelper.findAssetPaths(repository.getName());
    Assert.assertThat(assetPaths.get(0), is(equalTo(path)));
    Assert.assertThat(componentAssetTestHelper.contentTypeFor(repository.getName(), path), is(equalTo(type)));
    Assert.assertTrue(componentAssetTestHelper.attributes(repository, path).contains(HELM_FORMAT_NAME));
  }
}
