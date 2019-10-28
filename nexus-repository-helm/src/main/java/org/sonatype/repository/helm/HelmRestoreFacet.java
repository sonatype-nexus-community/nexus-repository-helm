package org.sonatype.repository.helm;

import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Query;

/**
 * @since 1.1.next
 */
@Exposed
public interface HelmRestoreFacet extends Facet
{
  void restore(final AssetBlob assetBlob, final String path) throws IOException;

  boolean assetExists(final String path);

  boolean componentRequired(final String name);

  Query getComponentQuery(final HelmAttributes attributes);

  HelmAttributes extractComponentAttributesFromArchive(final InputStream is) throws IOException;
}
