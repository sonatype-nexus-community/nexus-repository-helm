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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.annotation.Nonnull;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.plugins.helm.internal.fixtures.RepositoryRuleHelm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport;

import org.apache.commons.collections.IteratorUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.tika.io.IOUtils;
import org.junit.Rule;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Support class for Helm ITs.
 *
 * @since 1.0.0
 */
public class HelmITSupport
    extends RepositoryITSupport
{
  public static final String HELM_FORMAT_NAME = "helm";

  public static final String MONGO_PKG_NAME = "mongodb";

  public static final String YAML_NAME = "index";

  public static final String MONGO_PKG_VERSION_600 = "6.0.0";

  public static final String MONGO_PKG_VERSION_728 = "7.2.8";

  public static final String TGZ_EXT = ".tgz";

  public static final String YAML_EXT = ".yaml";

  public static final String MONGO_PKG_FILE_NAME_600_TGZ = format("%s-%s%s",
    MONGO_PKG_NAME, MONGO_PKG_VERSION_600, TGZ_EXT);

  public static final String MONGO_PKG_FILE_NAME_728_TGZ = format("%s-%s%s",
      MONGO_PKG_NAME, MONGO_PKG_VERSION_728, TGZ_EXT);

  public static final String CONTENT_TYPE_TGZ = "application/x-tgz";

  public static final String CONTENT_TYPE_YAML = "text/x-yaml";

  public static final String YAML_FILE_NAME = String.format("%s%s", YAML_NAME, YAML_EXT);

  public static final String PKG_PATH = "bin/macosx/el-capitan/contrib/3.6";

  public static final String MONGO_PATH_FULL_600_TARGZ = format("%s/%s", PKG_PATH, MONGO_PKG_FILE_NAME_600_TGZ);

  public static final String MONGO_PATH_FULL_728_TARGZ = format("%s/%s", PKG_PATH, MONGO_PKG_FILE_NAME_728_TGZ);

  public static final String YAML_MONGO_600_STRING_DATA = "urls:\n    - mongodb-6.0.0.tgz\n    version: 6.0.0";
  public static final String YAML_MONGO_728_STRING_DATA = "urls:\n    - mongodb-7.2.8.tgz\n    version: 7.2.8";

  @Rule
  public RepositoryRuleHelm repos = new RepositoryRuleHelm(() -> repositoryManager);

  public HelmITSupport() {
    testData.addDirectory(NexusPaxExamSupport.resolveBaseFile("target/test-classes/helm"));
  }

  @Nonnull
  protected HelmClient helmClient(final Repository repository) throws Exception {
    checkNotNull(repository);
    final URL repositoryUrl = repositoryBaseUrl(repository);

    return new HelmClient(
        clientBuilder(repositoryUrl).build(),
        clientContext(),
        repositoryUrl.toURI()
    );
  }

  @Nonnull
  protected HelmClient createHelmClient(final Repository repository) throws Exception {
    return new HelmClient(
        clientBuilder().build(),
        clientContext(),
        resolveUrl(nexusUrl, format("/repository/%s/", repository.getName())).toURI()
    );
  }

  protected HttpEntity fileToHttpEntity(String name) throws IOException {
    return new ByteArrayEntity(Files.readAllBytes(getFilePathByName(name)));
  }

  protected void checkYamlIncludesContent(InputStream is, String expectedContent) throws Exception {
    String downloadedPackageData = IOUtils.toString(is);
    assertThat(downloadedPackageData, containsString(expectedContent));
  }

  private Path getFilePathByName(String fileName){
    return Paths.get(testData.resolveFile(fileName).getAbsolutePath());
  }

  protected Component findComponentById(final Repository repository, final EntityId componentId) {
    try (StorageTx tx = getStorageTx(repository)) {
      tx.begin();
      return tx.findComponent(componentId);
    }
  }

  protected List<Asset> findAssetsByComponent(final Repository repository, final Component component) {
    try (StorageTx tx = getStorageTx(repository)) {
      tx.begin();
      return IteratorUtils.toList(tx.browseAssets(component).iterator());
    }
  }
}
