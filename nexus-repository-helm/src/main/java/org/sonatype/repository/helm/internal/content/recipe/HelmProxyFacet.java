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
package org.sonatype.repository.helm.internal.content.recipe;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.content.facet.ContentProxyFacetSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.repository.helm.internal.AssetKind;
import org.sonatype.repository.helm.internal.content.HelmContentFacet;
import org.sonatype.repository.helm.internal.metadata.IndexYamlAbsoluteUrlRewriter;
import org.sonatype.repository.helm.internal.util.HelmPathUtils;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * @since 1.0.11
 */
@Named
public class HelmProxyFacet
    extends ContentProxyFacetSupport
{
  private final HelmPathUtils helmPathUtils;
  private IndexYamlAbsoluteUrlRewriter indexYamlRewriter;

  private static final String INDEX_YAML = "/index.yaml";

  @Inject
  public HelmProxyFacet(final HelmPathUtils helmPathUtils,
                        final IndexYamlAbsoluteUrlRewriter indexYamlRewriter)
  {
    this.helmPathUtils = checkNotNull(helmPathUtils);
    this.indexYamlRewriter = checkNotNull(indexYamlRewriter);
  }

  @Override
  protected Content getCachedContent(final Context context) {
    Content content = content().getAsset(getAssetPath(context)).orElse(null);
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    if (assetKind == AssetKind.HELM_INDEX) {
      return indexYamlRewriter.removeUrlsFromIndexYaml(content);
    }
    return content;
  }

  /**
   * {@link org.sonatype.nexus.repository.content.store.AssetData} required forwarding slash in path,
   * but {@link java.net.URI#resolve(URI)} will not working correctly in this case.
   *
   * E.g. resolve '/index.yaml' on remote 'https://kubernetes.github.io/ingress-nginx' = 'https://kubernetes.github.io/index.yaml'
   * will produce 404. So, remove forwarding slash during fetching.
   */
  @Override
  protected Content fetch(String url, Context context, @Nullable Content stale) throws IOException {
    String newUrl = url;
    if (url.startsWith("/")) {
      newUrl = url.substring(1);
    }
    return super.fetch(newUrl, context, stale);
  }

  @Nonnull
  @Override
  protected CacheController getCacheController(@Nonnull final Context context)
  {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    return cacheControllerHolder.require(assetKind.getCacheType());
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case HELM_INDEX:
        return content().putIndex(getAssetPath(context), content, assetKind);
      case HELM_PACKAGE:
        return content().putComponent(getAssetPath(context), content, assetKind);
      default:
        throw new IllegalStateException("Received an invalid AssetKind of type: " + assetKind.name());
    }
  }

  private String getAssetPath(@Nonnull final Context context) {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case HELM_INDEX:
        return INDEX_YAML;
      case HELM_PACKAGE:
        TokenMatcher.State matcherState = helmPathUtils.matcherState(context);
        return helmPathUtils.contentFilePath(matcherState, true);
      default:
        throw new IllegalStateException("Received an invalid AssetKind of type: " + assetKind.name());
    }
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case HELM_INDEX:
        return INDEX_YAML;
      case HELM_PACKAGE:
        TokenMatcher.State matcherState = helmPathUtils.matcherState(context);
        Optional<Content> indexOpt = content().getAsset(INDEX_YAML);
        if (!indexOpt.isPresent()) {
          log.error("index.yml file is absent in repository: " + getRepository().getName());
          return null;
        }
        return helmPathUtils.contentFileUrl(matcherState, indexOpt.get(), true);
      default:
        throw new IllegalStateException("Received an invalid AssetKind of type: " + assetKind.name());
    }
  }

  private HelmContentFacet content() {
    return getRepository().facet(HelmContentFacet.class);
  }
}
