package org.sonatype.repository.helm.internal.content.recipe;

import java.io.IOException;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.repository.helm.internal.AssetKind;
import org.sonatype.repository.helm.internal.content.HelmContentFacet;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.repository.helm.internal.AssetKind.HELM_PACKAGE;
import static org.sonatype.repository.helm.internal.AssetKind.HELM_PROVENANCE;

/**
 * {@link HelmHostedFacet implementation}
 *
 * @since 1.0.11
 */
@Named
public class HelmHostedFacetImpl
    extends FacetSupport
    implements HelmHostedFacet
{
  private HelmContentFacet helmContentFacet;

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    helmContentFacet = facet(HelmContentFacet.class);
  }

  @Nullable
  @Override
  public Content get(final String path) {
    checkNotNull(path);
    return helmContentFacet.getAsset(path).orElse(null);
  }

  @Override
  public void upload(
      final String path,
      final Payload payload,
      final AssetKind assetKind) throws IOException
  {
    // TODO: Write policy selector
    checkNotNull(path);
    if (assetKind != HELM_PACKAGE && assetKind != HELM_PROVENANCE) {
      throw new IllegalArgumentException("Unsupported assetKind: " + assetKind);
    }
    helmContentFacet.putComponent(path, new Content(payload), assetKind);
  }

  @Override
  public boolean delete(final String path) {
    checkNotNull(path);
    return helmContentFacet.delete(path);
  }
}
