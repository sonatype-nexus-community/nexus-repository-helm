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
package org.sonatype.repository.helm.internal.orient.metadata;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.events.CollectionEndEvent;
import org.yaml.snakeyaml.events.CollectionStartEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.ScalarEvent;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IndexYamlAbsoluteUrlRewriterTest
    extends TestSupport
{
  private URI remoteRepositoryUri = URI.create("https://path.to.repo.infrastructure/pathtorepo/");
  
  
  private static final String INDEX_YAML = "index.yaml";

  private static final String INDEX_YAML_NO_ABSOLUTE_URLS = "indexWithRelativeUrls.yaml";

  private static final String INDEX_YAML_WITH_CUSTOM_URL = "index.yaml";

  private static final String INDEX_YAML_URL_NODE = "url";

  private static final String HTTP = "http://";

  private static final String HTTPS = "https://";

  private IndexYamlAbsoluteUrlRewriter underTest;

  @Mock
  private TempBlob tempBlob;

  @Mock
  Repository repository;

  @Mock
  StorageFacet storageFacet;

  @Before
  public void setUp() throws Exception {
    setupRepositoryMock();
    this.underTest = new IndexYamlAbsoluteUrlRewriter();
  }

  @Test
  public void checkCustomUrls() throws Exception {
    setupIndexMock(INDEX_YAML_WITH_CUSTOM_URL);
    TempBlob newTempBlob = underTest.removeUrlsFromIndexYamlAndWriteToTempBlob(tempBlob, repository, remoteRepositoryUri);
    assertThat(newTempBlob.get(), is(instanceOf(InputStream.class)));
    checkThatAbsoluteUrlRemoved(newTempBlob.get());
  }

  @Test
  public void removeUrlsFromIndexYaml() throws Exception {
    setupIndexMock(INDEX_YAML);
    TempBlob newTempBlob = underTest.removeUrlsFromIndexYamlAndWriteToTempBlob(tempBlob, repository, remoteRepositoryUri);
    assertThat(newTempBlob.get(), is(instanceOf(InputStream.class)));
    checkThatAbsoluteUrlRemoved(newTempBlob.get());
  }

  @Test
  public void doNotModifyUrlsWhenAlreadyRelative() throws Exception {
    setupIndexMock(INDEX_YAML_NO_ABSOLUTE_URLS);
    TempBlob newTempBlob = underTest.removeUrlsFromIndexYamlAndWriteToTempBlob(tempBlob, repository, remoteRepositoryUri);
    assertThat(newTempBlob.get(), is(instanceOf(InputStream.class)));
    checkRelativeUrlsInSet(newTempBlob.get());
  }
  
  // The expected URL strings based on indexWithRelativeUrls.yaml
  private static final String[] expectedConvertedURLs =
      {"acs-engine-autoscaler-2.1.3.tgz", 
          "charts/acs-engine-autoscaler-2.1.2.tgz", 
          "acs-engine-autoscaler-2.1.1.tgz",
          "pathwithinrepo/some-application-7.5.0.tgz"};

  private void checkRelativeUrlsInSet(final InputStream is) throws Exception {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      Yaml yaml = new Yaml();
      int urlIndex = 0;
      boolean inUrl = false;
      for (Event yamlEvent : yaml.parse(reader)) {
        if (yamlEvent instanceof ScalarEvent) {
          ScalarEvent scalarEvent = (ScalarEvent) yamlEvent;
          if (inUrl) {
            assertThat(scalarEvent.getValue(), equalTo(expectedConvertedURLs[urlIndex]));
            urlIndex++;
          }
          else if ("urls".equals(scalarEvent.getValue())) {
            inUrl = true;
          }
        }
        else if (yamlEvent instanceof CollectionStartEvent) {
          // NOOP
        }
        else if (yamlEvent instanceof CollectionEndEvent) {
          inUrl = false;
        }
      }
    }
  }

  private void checkThatAbsoluteUrlRemoved(final InputStream is) throws Exception {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      boolean checkNext = false;
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (checkNext) {
          assertThat(line, either(not(containsString(HTTP))).or(not(containsString(HTTPS))));
          assertThat(line, not(startsWith("- /")));
          checkNext = false;
        }
        if (line.contains(INDEX_YAML_URL_NODE)) {
          checkNext = true;
        }
      }
    }
  }

  /**
   * snakeyaml in versions other than 1.20 seems to add a ! to every line, this ensures we don't regress
   */
  @Test
  public void ensureNoExclamationMarksInYaml() throws Exception {
    setupIndexMock(INDEX_YAML);
    underTest.removeUrlsFromIndexYamlAndWriteToTempBlob(tempBlob, repository, remoteRepositoryUri);
    BufferedReader reader = new BufferedReader(new InputStreamReader(tempBlob.get()));
    String line;
    while ((line = reader.readLine()) != null) {
      line = line.trim();
      assertThat(line, is(not("!")));
    }
    reader.close();
  }

  private void setupIndexMock(final String indexYamlName) throws Exception {
    when(tempBlob.get()).thenReturn(getClass().getResourceAsStream(indexYamlName));
    when(tempBlob.getBlob()).thenReturn(mock(Blob.class));
  }

  @SuppressWarnings("unchecked")
  private void setupRepositoryMock() {
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(storageFacet.createTempBlob(any(InputStream.class), any(Iterable.class))).thenAnswer(args -> {
      InputStream inputStream = (InputStream) args.getArguments()[0];
      byte[] bytes = IOUtils.toByteArray(inputStream);
      when(tempBlob.get()).thenReturn(new ByteArrayInputStream(bytes));
      return tempBlob;
    });
  }
}
