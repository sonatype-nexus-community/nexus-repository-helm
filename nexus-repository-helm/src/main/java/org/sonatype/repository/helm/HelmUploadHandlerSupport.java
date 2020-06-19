package org.sonatype.repository.helm;

import java.io.IOException;
import java.util.Set;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadHandlerSupport;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.repository.helm.internal.util.HelmAttributeParser;

import static org.sonatype.repository.helm.internal.HelmFormat.NAME;

public class HelmUploadHandlerSupport
    extends UploadHandlerSupport
{
  protected final HelmAttributeParser helmPackageParser;

  protected final ContentPermissionChecker contentPermissionChecker;

  protected final VariableResolverAdapter variableResolverAdapter;

  protected UploadDefinition definition;

  public HelmUploadHandlerSupport(
      final ContentPermissionChecker contentPermissionChecker,
      final HelmAttributeParser helmPackageParser,
      final VariableResolverAdapter variableResolverAdapter,
      final Set<UploadDefinitionExtension> uploadDefinitionExtensions)
  {
    super(uploadDefinitionExtensions);
    this.contentPermissionChecker = contentPermissionChecker;
    this.variableResolverAdapter = variableResolverAdapter;
    this.helmPackageParser = helmPackageParser;
  }

  @Override
  public UploadResponse handle(
      final Repository repository, final ComponentUpload upload) throws IOException
  {
    return null;
  }

  @Override
  public UploadDefinition getDefinition() {
    if (definition == null) {
      definition = getDefinition(NAME, false);
    }
    return definition;
  }

  @Override
  public VariableResolverAdapter getVariableResolverAdapter() {
    return variableResolverAdapter;
  }

  @Override
  public ContentPermissionChecker contentPermissionChecker() {
    return contentPermissionChecker;
  }

  @Override
  public boolean supportsExportImport() {
    return true;
  }
}
