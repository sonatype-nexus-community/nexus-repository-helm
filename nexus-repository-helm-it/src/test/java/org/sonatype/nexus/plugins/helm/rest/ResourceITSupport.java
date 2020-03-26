package org.sonatype.nexus.plugins.helm.rest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.StringJoiner;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;

import org.sonatype.nexus.plugins.helm.internal.fixtures.RepositoryRuleHelm;
import org.sonatype.nexus.repository.rest.api.model.AbstractRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.CleanupPolicyAttributes;
import org.sonatype.nexus.repository.rest.api.model.HostedStorageAttributes;
import org.sonatype.nexus.repository.rest.api.model.HttpClientAttributes;
import org.sonatype.nexus.repository.rest.api.model.HttpClientConnectionAttributes;
import org.sonatype.nexus.repository.rest.api.model.HttpClientConnectionAuthenticationAttributes;
import org.sonatype.nexus.repository.rest.api.model.NegativeCacheAttributes;
import org.sonatype.nexus.repository.rest.api.model.ProxyAttributes;
import org.sonatype.nexus.repository.storage.WritePolicy;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport;
import org.sonatype.nexus.testsuite.testsupport.fixtures.SecurityRule;
import org.sonatype.repository.helm.api.HelmHostedRepositoryApiRequest;
import org.sonatype.repository.helm.api.HelmProxyRepositoryApiRequest;
import org.sonatype.repository.helm.internal.HelmFormat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.junit.Rule;

public class ResourceITSupport
    extends RepositoryITSupport
{
  // SET YOUR FORMAT DATA
  public static final String FORMAT_VALUE = HelmFormat.NAME;

  public static final String PROXY_NAME = String.format("%s-%s", FORMAT_VALUE, ProxyType.NAME);

  public static final String HOSTED_NAME = String.format("%s-%s", FORMAT_VALUE, HostedType.NAME);
  // SET YOUR FORMAT DATA

  public static final String UNRELATED_PRIVILEGE = "nx-analytics-all";

  private static final String REPOSITORIES_API_URL = "service/rest/beta/repositories";

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("admin", "admin123");

  @Rule
  public SecurityRule securityRule = new SecurityRule(() -> securitySystem, () -> selectorManager);

  @Rule
  public RepositoryRuleHelm repos = new RepositoryRuleHelm(() -> repositoryManager);

  protected void setUnauthorizedUser() {
    String randomRoleName = "role_" + UUID.randomUUID().toString();

    Role role = securityRule.createRole(randomRoleName, UNRELATED_PRIVILEGE);
    securityRule.createUser(randomRoleName, randomRoleName, role.getRoleId());
    credentials = new UsernamePasswordCredentials(randomRoleName, randomRoleName);
  }

  protected void setBadCredentials() {
    credentials = new UsernamePasswordCredentials("fake_user", "fake_user");
  }

  protected String getCreateRepositoryPathUrl(final String type) {
    return new StringJoiner("/")
        .add(REPOSITORIES_API_URL)
        .add(FORMAT_VALUE)
        .add(type)
        .toString();
  }

  protected String getUpdateRepositoryPathUrl(final String type, final String name) {
    return new StringJoiner("/")
        .add(REPOSITORIES_API_URL)
        .add(FORMAT_VALUE)
        .add(type)
        .add(name)
        .toString();
  }

  protected AbstractRepositoryApiRequest createProxyRequest(boolean strictContentTypeValidation) {
    HostedStorageAttributes storage =
        new HostedStorageAttributes("default", strictContentTypeValidation, WritePolicy.ALLOW.name());
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
        new HostedStorageAttributes("default", strictContentTypeValidation, WritePolicy.ALLOW.name());
    CleanupPolicyAttributes cleanup = new CleanupPolicyAttributes(Collections.emptyList());

    // SET YOUR FORMAT DATA
    return new HelmHostedRepositoryApiRequest(HOSTED_NAME, true, storage,
        cleanup);
  }

  protected Response post(final String url, final Object body) throws Exception {
    HttpPost request = new HttpPost();
    prepareRequest(request, url, body);
    return execute(request);
  }

  protected Response put(final String url, final Object body) throws Exception {
    HttpPut request = new HttpPut();
    prepareRequest(request, url, body);
    return execute(request);
  }

  private Response execute(HttpEntityEnclosingRequestBase request) throws Exception {
    try (CloseableHttpResponse response = clientBuilder().build().execute(request)) {
      ResponseBuilder responseBuilder = Response.status(response.getStatusLine().getStatusCode());
      Arrays.stream(response.getAllHeaders()).forEach(h -> responseBuilder.header(h.getName(), h.getValue()));

      HttpEntity entity = response.getEntity();
      if (entity != null) {
        responseBuilder.entity(new ByteArrayInputStream(IOUtils.toByteArray(entity.getContent())));
      }
      return responseBuilder.build();
    }
  }

  private void prepareRequest(HttpEntityEnclosingRequestBase request, String url, Object body) throws Exception {
    request.setEntity(new ByteArrayEntity(objectMapper.writeValueAsBytes(body), ContentType.APPLICATION_JSON));
    UriBuilder uriBuilder = UriBuilder.fromUri(nexusUrl.toString()).path(url);
    request.setURI(uriBuilder.build());
    String auth = credentials.getUserName() + ":" + credentials.getPassword();
    request.setHeader(HttpHeaders.AUTHORIZATION,
        "Basic " + new String(Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8))));
  }
}
