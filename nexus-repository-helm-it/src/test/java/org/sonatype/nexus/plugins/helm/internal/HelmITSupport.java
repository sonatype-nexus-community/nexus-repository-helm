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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.plugins.helm.internal.fixtures.RepositoryRuleHelm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.api.model.AbstractRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.CleanupPolicyAttributes;
import org.sonatype.nexus.repository.rest.api.model.HostedStorageAttributes;
import org.sonatype.nexus.repository.rest.api.model.HttpClientAttributes;
import org.sonatype.nexus.repository.rest.api.model.HttpClientConnectionAttributes;
import org.sonatype.nexus.repository.rest.api.model.HttpClientConnectionAuthenticationAttributes;
import org.sonatype.nexus.repository.rest.api.model.NegativeCacheAttributes;
import org.sonatype.nexus.repository.rest.api.model.ProxyAttributes;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.WritePolicy;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.testsuite.testsupport.FormatClientSupport;
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport;
import org.sonatype.nexus.testsuite.testsupport.fixtures.SecurityRule;
import org.sonatype.repository.helm.api.HelmHostedRepositoryApiRequest;
import org.sonatype.repository.helm.api.HelmProxyRepositoryApiRequest;
import org.sonatype.repository.helm.internal.HelmFormat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import org.apache.commons.collections.IteratorUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.tika.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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

  public static final String MONGO_PKG_VERSION_404 = "4.0.4";

  public static final String TGZ_EXT = ".tgz";

  public static final String YAML_EXT = ".yaml";

  public static final String MONGO_PKG_FILE_NAME_728_TGZ = format("%s-%s%s",
      MONGO_PKG_NAME, MONGO_PKG_VERSION_728, TGZ_EXT);

  public static final String MONGO_PKG_FILE_NAME_600_TGZ = format("%s-%s%s",
      MONGO_PKG_NAME, MONGO_PKG_VERSION_600, TGZ_EXT);

  public static final String MONGO_PKG_FILE_NAME_404_TGZ = format("%s-%s%s",
      MONGO_PKG_NAME, MONGO_PKG_VERSION_404, TGZ_EXT);

  public static final String CONTENT_TYPE_TGZ = "application/x-tgz";

  public static final String CONTENT_TYPE_YAML = "text/x-yaml";

  public static final String YAML_FILE_NAME = String.format("%s%s", YAML_NAME, YAML_EXT);

  public static final String PKG_PATH = "bin/macosx/el-capitan/contrib/3.6";

  public static final String MONGO_PATH_FULL_600_TARGZ = format("%s/%s", PKG_PATH, MONGO_PKG_FILE_NAME_600_TGZ);

  public static final String MONGO_PATH_FULL_728_TARGZ = format("%s/%s", PKG_PATH, MONGO_PKG_FILE_NAME_728_TGZ);

  public static final String YAML_MONGO_600_STRING_DATA = "urls:\n    - mongodb-6.0.0.tgz\n    version: 6.0.0";

  public static final String YAML_MONGO_728_STRING_DATA = "urls:\n    - mongodb-7.2.8.tgz\n    version: 7.2.8";

  @Rule
  public SecurityRule securityRule = new SecurityRule(() -> securitySystem, () -> selectorManager);

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

  private Path getFilePathByName(String fileName) {
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

  protected void assertGetResponseStatus(
      final FormatClientSupport client,
      final Repository repository,
      final String path,
      final int responseCode) throws IOException
  {
    try (CloseableHttpResponse response = client.get(path)) {
      StatusLine statusLine = response.getStatusLine();
      Assert.assertThat("Repository:" + repository.getName() + " Path:" + path,
          statusLine.getStatusCode(),
          is(responseCode));
    }
  }

  // SET YOUR FORMAT DATA
  public static final String FORMAT_VALUE = HelmFormat.NAME;

  public static final String PROXY_NAME = String.format("%s-%s", FORMAT_VALUE, ProxyType.NAME);

  public static final String HOSTED_NAME = String.format("%s-%s", FORMAT_VALUE, HostedType.NAME);
  // SET YOUR FORMAT DATA

  public static final String UNRELATED_PRIVILEGE = "nx-analytics-all";

  private static final String REPOSITORIES_API_URL = "service/rest/beta/repositories";

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("admin", "admin123");

  protected void setUnauthorizedUser() {
    String randomRoleName = "role_" + UUID.randomUUID().toString();

    Role role = securityRule.createRole(randomRoleName, UNRELATED_PRIVILEGE);
    securityRule.createUser(randomRoleName, randomRoleName, role.getRoleId());
    credentials = new UsernamePasswordCredentials(randomRoleName, randomRoleName);
  }

  protected void setBadCredentials() {
    credentials = new UsernamePasswordCredentials("fake_user", "fake_user");
  }

  protected String getCreateRepositoryPathFor(final String type) {
    return new StringJoiner("/")
        .add(REPOSITORIES_API_URL)
        .add(FORMAT_VALUE)
        .add(type)
        .toString();
  }

  protected String getUpdateRepositoryPathFor(final String type, final String name) {
    return new StringJoiner("/")
        .add(REPOSITORIES_API_URL)
        .add(FORMAT_VALUE)
        .add(type)
        .add(name)
        .toString();
  }

  protected AbstractRepositoryApiRequest createProxyRequest(boolean strictContentTypeValidation) {
    HostedStorageAttributes storage =
        new HostedStorageAttributes("default", strictContentTypeValidation, WritePolicy.ALLOW);
    CleanupPolicyAttributes cleanup = new CleanupPolicyAttributes(Collections.emptyList());
    ProxyAttributes proxy = new ProxyAttributes("http://example.net", 1, 2);
    NegativeCacheAttributes negativeCache = new NegativeCacheAttributes(false, 1440);
    HttpClientConnectionAuthenticationAttributes authentication =
        new HttpClientConnectionAuthenticationAttributes("username", null, null, null, null);
    HttpClientConnectionAttributes connection =
        new HttpClientConnectionAttributes(1, null, 5, false, false);
    HttpClientAttributes httpClient = new HttpClientAttributes(false, true, connection, authentication);

    // SET YOUR FORMAT DATA
    return new HelmProxyRepositoryApiRequest(PROXY_NAME, true, storage, cleanup,
        proxy, negativeCache,
        httpClient, null);
  }

  protected AbstractRepositoryApiRequest createHostedRequest(boolean strictContentTypeValidation) {
    HostedStorageAttributes storage =
        new HostedStorageAttributes("default", strictContentTypeValidation, WritePolicy.ALLOW);
    CleanupPolicyAttributes cleanup = new CleanupPolicyAttributes(Collections.emptyList());

    // SET YOUR FORMAT DATA
    return new HelmHostedRepositoryApiRequest(HOSTED_NAME, true, storage,
        cleanup);
  }

  protected Response post(final String url, final Object entity) throws Exception {
    return execute(new HttpPost(), url, entity, Collections.emptyMap());
  }

  protected Response put(final String url, final Object entity) throws Exception {
    return execute(new HttpPut(), url, entity, Collections.emptyMap());
  }

  private Response execute(
      final HttpEntityEnclosingRequestBase httpRequestBase,
      final String url,
      final Object body,
      final Map<String, String> queryParams) throws Exception
  {
    httpRequestBase.setEntity(
        new StringEntity(objectMapper.writerFor(body.getClass()).writeValueAsString(body),
            ContentType.APPLICATION_JSON));
    UriBuilder uriBuilder = UriBuilder.fromUri(nexusUrl.toString()).path(url);
    queryParams.forEach(uriBuilder::queryParam);
    httpRequestBase.setURI(uriBuilder.build());

    String auth = credentials.getUserName() + ":" + credentials.getPassword();
    httpRequestBase.setHeader(HttpHeaders.AUTHORIZATION,
        "Basic " + new String(Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8))));

    try (CloseableHttpClient client = super.clientBuilder().build()) {
      try (CloseableHttpResponse response = client.execute(httpRequestBase)) {
        ResponseBuilder responseBuilder = Response.status(response.getStatusLine().getStatusCode());
        Arrays.stream(response.getAllHeaders()).forEach(h -> responseBuilder.header(h.getName(), h.getValue()));

        HttpEntity entity = response.getEntity();
        if (entity != null) {
          responseBuilder
              .entity(new ByteArrayInputStream(org.apache.commons.io.IOUtils.toByteArray(entity.getContent())));
        }
        return responseBuilder.build();
      }
    }
  }
}
