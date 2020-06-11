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
package org.sonatype.nexus.plugins.helm.internal.restore;

import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.restore.RestoreBlobStrategy;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.plugins.helm.internal.HelmClient;
import org.sonatype.nexus.plugins.helm.internal.HelmITSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;
import org.sonatype.nexus.testsuite.testsupport.blobstore.restore.BlobstoreRestoreTestHelper;
import org.sonatype.repository.helm.internal.HelmFormat;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.file;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;
import static org.sonatype.nexus.repository.storage.Bucket.REPO_NAME_HEADER;

public class HelmRestoreBlobIT
    extends HelmITSupport
{
  @Inject
  private BlobstoreRestoreTestHelper testHelper;

  @Inject
  @Named(HelmFormat.NAME)
  private RestoreBlobStrategy helmRestoreBlobStrategy;

  private static final String HOSTED_REPO_NAME = "helm-hosted";

  private static final String PROXY_REPO_NAME = "helm-proxy";

  private Server proxyServer;

  private HelmClient hostedClient;

  private HelmClient proxyClient;

  private Repository hostedRepository;

  private Repository proxyRepository;

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-helm"),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-restore-helm")
    );
  }

  @Before
  public void setup() throws Exception {
    BaseUrlHolder.set(this.nexusUrl.toString());
    hostedRepository = repos.createHelmHosted(HOSTED_REPO_NAME);
    hostedClient = createHelmClient(hostedRepository);

    proxyServer = Server.withPort(0)
        .serve("/" + MONGO_PATH_FULL_728_TARGZ)
        .withBehaviours(file(testData.resolveFile(MONGO_PKG_FILE_NAME_728_TGZ)))
        .start();

    proxyRepository = repos.createHelmProxy(PROXY_REPO_NAME, "http://localhost:" + proxyServer.getPort() + "/");
    proxyClient = createHelmClient(proxyRepository);

    assertThat(hostedClient.put(MONGO_PATH_FULL_728_TARGZ,
        fileToHttpEntity(MONGO_PKG_FILE_NAME_728_TGZ)).getStatusLine().getStatusCode(), is(HttpStatus.OK));
    assertThat(proxyClient.fetch(MONGO_PATH_FULL_728_TARGZ, CONTENT_TYPE_TGZ).getStatusLine().getStatusCode(), is(HttpStatus.OK));
  }

  @After
  public void tearDown() throws Exception {
    if (proxyServer != null) {
      proxyServer.stop();
    }
  }

  @Test
  public void testMetadataRestoreWhenBothAssetsAndComponentsAreMissing() throws Exception {
    verifyMetadataRestored(testHelper::simulateComponentAndAssetMetadataLoss);
  }

  @Test
  public void testMetadataRestoreWhenOnlyAssetsAreMissing() throws Exception {
    verifyMetadataRestored(testHelper::simulateAssetMetadataLoss);
  }

  @Test
  public void testMetadataRestoreWhenOnlyComponentsAreMissing() throws Exception {
    verifyMetadataRestored(testHelper::simulateComponentMetadataLoss);
  }

  @Test
  public void testNotDryRunRestore()
  {
    runBlobRestore(false);
    testHelper.assertAssetInRepository(proxyRepository, MONGO_PATH_FULL_728_TARGZ);
  }

  @Test
  public void testDryRunRestore()
  {
    runBlobRestore(true);
    testHelper.assertAssetNotInRepository(proxyRepository, MONGO_PATH_FULL_728_TARGZ);
  }

  private void runBlobRestore(final boolean isDryRun) {
    Asset asset;
    Blob blob;
    try (StorageTx tx = getStorageTx(proxyRepository)) {
      tx.begin();
      asset = tx.findAssetWithProperty(AssetEntityAdapter.P_NAME, MONGO_PATH_FULL_728_TARGZ,
          tx.findBucket(proxyRepository));
      assertThat(asset, Matchers.notNullValue());
      blob = tx.getBlob(asset.blobRef());
    }
    testHelper.simulateAssetMetadataLoss();
    Properties properties = new Properties();
    properties.setProperty(HEADER_PREFIX + REPO_NAME_HEADER, proxyRepository.getName());
    properties.setProperty(HEADER_PREFIX + BLOB_NAME_HEADER, asset.name());
    properties.setProperty(HEADER_PREFIX + CONTENT_TYPE_HEADER, asset.contentType());

    helmRestoreBlobStrategy.restore(properties, blob, BlobStoreManager.DEFAULT_BLOBSTORE_NAME, isDryRun);
  }

  private void verifyMetadataRestored(final Runnable metadataLossSimulation) throws Exception {
    metadataLossSimulation.run();

    testHelper.runRestoreMetadataTask();

    testHelper.assertComponentInRepository(hostedRepository, MONGO_PKG_NAME);
    testHelper.assertComponentInRepository(proxyRepository, MONGO_PKG_NAME);

    testHelper.assertAssetMatchesBlob(hostedRepository, MONGO_PATH_FULL_728_TARGZ);
    testHelper.assertAssetMatchesBlob(proxyRepository, MONGO_PATH_FULL_728_TARGZ);

    testHelper.assertAssetAssociatedWithComponent(hostedRepository, MONGO_PKG_NAME, MONGO_PATH_FULL_728_TARGZ);
    testHelper.assertAssetAssociatedWithComponent(proxyRepository, MONGO_PKG_NAME, MONGO_PATH_FULL_728_TARGZ);

    assertThat(hostedClient.get(MONGO_PATH_FULL_728_TARGZ).getStatusLine().getStatusCode(), is(HttpStatus.OK));
    assertThat(proxyClient.get(MONGO_PATH_FULL_728_TARGZ).getStatusLine().getStatusCode(), is(HttpStatus.OK));
  }
}
