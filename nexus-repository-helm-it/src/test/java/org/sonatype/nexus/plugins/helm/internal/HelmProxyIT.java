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

public class HelmProxyIT
    extends HelmITSupport
{
  private HelmClient client;

  private Repository repository;

  private Server server;

  @Configuration
  public static Option[] configureNexus() {
    return options(
        configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-helm")
    );
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
    assertSuccessResponseMatches(client.fetch(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ), MONGO_PKG_FILE_NAME_600_TGZ);
    final Asset asset = findAsset(repository, MONGO_PKG_FILE_NAME_600_TGZ);
    Assert.assertThat(asset.name(), is(equalTo(MONGO_PKG_FILE_NAME_600_TGZ)));
    Assert.assertThat(asset.contentType(), is(equalTo(CONTENT_TYPE_TGZ)));
    Assert.assertThat(asset.format(), is(equalTo(HELM_FORMAT_NAME)));
  }

  @Test
  public void fetchMetaData() throws Exception {
    assertSuccessResponseMatches(client.fetch(YAML_FILE_NAME, CONTENT_TYPE_YAML), YAML_FILE_NAME);
    final Asset asset = findAsset(repository, YAML_FILE_NAME);
    Assert.assertThat(asset.contentType(), is(equalTo(CONTENT_TYPE_YAML)));
    Assert.assertThat(asset.format(), is(equalTo(HELM_FORMAT_NAME)));
  }

  @Test
  public void checkComponentCreated() throws Exception {
    final Component nullComponent = findComponent(repository, MONGO_PKG_NAME);
    Assert.assertThat(nullComponent, is(nullValue()));
    client.fetch(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ);

    final Component component = findComponent(repository, MONGO_PKG_NAME);
    Assert.assertThat(component.name(), is(equalTo(MONGO_PKG_NAME)));
    Assert.assertThat(component.format(), is(equalTo(HELM_FORMAT_NAME)));
    Assert.assertThat(component.group(), is(nullValue()));
    Assert.assertThat(component.version(), is(equalTo(MONGO_PKG_VERSION_600)));
  }

  @Test
  public void testDeletingComponentDeletesAllAssociatedAssets() throws Exception {
    client.fetch(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ);
    final Component component = findComponent(repository, MONGO_PKG_NAME);
    Assert.assertNotNull(component);
    Assert.assertFalse(findAssetsByComponent(repository, component).isEmpty());

    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteComponent(component.getEntityMetadata().getId(), true);

    Assert.assertNull(findComponent(repository, MONGO_PKG_NAME));
    Assert.assertTrue(findAssetsByComponent(repository, component).isEmpty());
  }

  @Test
  public void testDeletingRemainingAssetAlsoDeletesComponent() throws Exception {
    client.fetch(MONGO_PKG_FILE_NAME_600_TGZ, CONTENT_TYPE_TGZ);

    final Asset asset = findAsset(repository, MONGO_PKG_FILE_NAME_600_TGZ);
    Assert.assertNotNull(asset);
    Assert.assertNotNull(findComponentById(repository, asset.componentId()));

    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteAsset(asset.getEntityMetadata().getId(), true);

    Assert.assertNull(findAsset(repository, MONGO_PKG_FILE_NAME_600_TGZ));
    Assert.assertNull(findComponentById(repository, asset.componentId()));
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
}
