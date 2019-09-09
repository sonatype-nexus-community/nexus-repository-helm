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

import java.net.URL;

import javax.annotation.Nonnull;

import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.plugins.helm.internal.fixtures.RepositoryRuleHelm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport;

import org.junit.Rule;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

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

  public static final String TGZ_EXT = ".tgz";

  public static final String YAML_EXT = ".yaml";

  public static final String MONGO_PKG_FILE_NAME_600_TGZ = format("%s-%s%s",
    MONGO_PKG_NAME, MONGO_PKG_VERSION_600, TGZ_EXT);

  public static final String CONTENT_TYPE_TGZ = "application/x-tgz";

  public static final String CONTENT_TYPE_YAML = "text/x-yaml";

  public static final String YAML_FILE_NAME = String.format("%s%s", YAML_NAME, YAML_EXT);

  @Rule
  public RepositoryRuleHelm repos = new RepositoryRuleHelm(() -> repositoryManager);

  public HelmITSupport() {
    testData.addDirectory(NexusPaxExamSupport.resolveBaseFile("target/test-classes/helm"));
  }

  @Nonnull
  protected HelmClient helmClient(final Repository repository) throws Exception {
    checkNotNull(repository);
    final URL repositoryUrl = repositoryBaseUrl(repository);
    return helmClient(repositoryUrl);
  }

  protected HelmClient helmClient(final URL repositoryUrl) throws Exception {
    return new HelmClient(
        clientBuilder(repositoryUrl).build(),
        clientContext(),
        repositoryUrl.toURI()
    );
  }
}
