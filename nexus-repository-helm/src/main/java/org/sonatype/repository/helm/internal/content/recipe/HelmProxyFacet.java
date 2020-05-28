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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.repository.helm.internal.AssetKind;
import org.sonatype.repository.helm.internal.content.HelmContentFacet;
import org.sonatype.repository.helm.internal.util.HelmPathUtils;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * @since 1.0.9
 */
@Named
public class HelmProxyFacet
    extends ProxyFacetSupport
{
  private final HelmPathUtils helmPathUtils;

  private static final String INDEX_YAML = "index.yaml";

  @Inject
  public HelmProxyFacet(final HelmPathUtils helmPathUtils)
  {
    this.helmPathUtils = checkNotNull(helmPathUtils);
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
  }

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    super.doValidate(configuration);
  }

  @Nullable
  @Override
  protected Content getCachedContent(final Context context)  throws IOException {
    return content()
        .getAsset(getUrl(context))
        .map(Content::new)
        .orElse(null);
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case HELM_INDEX:
        return new Content(content().putIndex(getUrl(context), content, assetKind));
      case HELM_PACKAGE:
        return new Content(content().putComponent(getUrl(context), content, assetKind));
      default:
        throw new IllegalStateException("Received an invalid AssetKind of type: " + assetKind.name());
    }
  }

  @Override
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo)
      throws IOException
  {
    log.debug("Not implemented yet");
    //caching will be worked on in - NEXUS-23605
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case HELM_INDEX:
        return INDEX_YAML;
      case HELM_PACKAGE:
        TokenMatcher.State matcherState = helmPathUtils.matcherState(context);
        return helmPathUtils.filename(matcherState);
      default:
        throw new IllegalStateException("Received an invalid AssetKind of type: " + assetKind.name());
    }
  }

  private HelmContentFacet content() {
    return getRepository().facet(HelmContentFacet.class);
  }
}
