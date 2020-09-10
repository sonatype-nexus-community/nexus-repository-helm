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
package org.sonatype.nexus.plugins.helm.internal.cleanup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.plugins.helm.internal.HelmClient;
import org.sonatype.nexus.plugins.helm.internal.fixtures.RepositoryRuleHelm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.testsuite.testsupport.cleanup.CleanupITSupport;

import org.apache.http.entity.ByteArrayEntity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.sonatype.nexus.plugins.helm.HelmITConfig.configureHelmBase;
import static org.sonatype.nexus.plugins.helm.internal.HelmITSupport.MONGO_PKG_FILE_NAME_404_TGZ;
import static org.sonatype.nexus.plugins.helm.internal.HelmITSupport.MONGO_PKG_FILE_NAME_600_TGZ;
import static org.sonatype.nexus.plugins.helm.internal.HelmITSupport.MONGO_PKG_FILE_NAME_728_TGZ;
import static org.sonatype.nexus.plugins.helm.internal.HelmITSupport.PKG_PATH;
import static org.sonatype.nexus.plugins.helm.internal.HelmITSupport.TGZ_EXT;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;

public class CleanupTaskHelmIT
    extends CleanupITSupport
{
  public static final String[] NAMES = {MONGO_PKG_FILE_NAME_404_TGZ};

  public Repository repository;

  @Rule
  public RepositoryRuleHelm repos = new RepositoryRuleHelm(() -> repositoryManager);

  @Configuration
  public static Option[] configureNexus() {
    return configureHelmBase();
  }

  @Before
  public void setup() {
    testData.addDirectory(NexusPaxExamSupport.resolveBaseFile("target/test-classes/helm"));
    repository = repos.createHelmHosted(testName.getMethodName());
    assertThat(deployArtifacts(NAMES), is(NAMES.length));
  }

  @Test
  public void cleanupByLastBlobUpdated() throws Exception {
    assertLastBlobUpdatedComponentsCleanedUp(repository, NAMES.length,
        () -> deployArtifacts(MONGO_PKG_FILE_NAME_728_TGZ), 1L);
  }

  @Test
  public void cleanupByLastDownloaded() throws Exception {
    assertLastDownloadedComponentsCleanedUp(repository, NAMES.length,
        () -> deployArtifacts(MONGO_PKG_FILE_NAME_728_TGZ), 1L);
  }

  @Test
  public void cleanupByRegex() throws Exception {
    assertCleanupByRegex(repository, NAMES.length, "bin.*-[4,7].*.tgz",
        () -> deployArtifacts(MONGO_PKG_FILE_NAME_600_TGZ, MONGO_PKG_FILE_NAME_728_TGZ), 1L);
  }

  @Override
  protected boolean componentMatchesByVersion(final Component component, final String version) {
    return version
        .equals(format("%s_%s%s", component.name().toLowerCase(), component.version().toLowerCase(), TGZ_EXT));
  }

  private int deployArtifacts(final String... names) {
    try {
      HelmClient client = new HelmClient(clientBuilder().build(),
          clientContext(),
          resolveUrl(nexusUrl, format("/repository/%s/", repository.getName())).toURI()
      );

      for (String name : names) {
        assertThat(status(client.put(format("%s/%s", PKG_PATH, name), new ByteArrayEntity(getBytesFromTestData(name)))),
            is(OK));
      }

      return names.length;
    }
    catch (Exception e) {
      log.error("", e);
    }
    return 0;
  }

  private byte[] getBytesFromTestData(String path) throws IOException {
    final File file = testData.resolveFile(path);
    return Files.readAllBytes(Paths.get(file.getAbsolutePath()));
  }
}
