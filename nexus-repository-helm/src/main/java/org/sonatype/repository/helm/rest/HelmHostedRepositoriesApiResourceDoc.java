/*
 * Copyright (c) 2008-present Sonatype, Inc.
 *
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.repository.helm.rest;

import javax.ws.rs.core.Response;

import org.sonatype.repository.helm.api.HelmHostedRepositoryApiRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import static org.sonatype.nexus.rest.ApiDocConstants.API_REPOSITORY_MANAGEMENT;
import static org.sonatype.nexus.rest.ApiDocConstants.AUTHENTICATION_REQUIRED;
import static org.sonatype.nexus.rest.ApiDocConstants.INSUFFICIENT_PERMISSIONS;
import static org.sonatype.nexus.rest.ApiDocConstants.REPOSITORY_CREATED;
import static org.sonatype.nexus.rest.ApiDocConstants.REPOSITORY_UPDATED;

@Api(value = API_REPOSITORY_MANAGEMENT)
public interface HelmHostedRepositoriesApiResourceDoc
{
  @ApiOperation("Create Helm hosted repository")
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = REPOSITORY_CREATED),
      @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
      @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS)
  })
  Response createRepository(final HelmHostedRepositoryApiRequest request);

  @ApiOperation("Update Helm hosted repository")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = REPOSITORY_UPDATED),
      @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
      @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS)
  })
  Response updateRepository(
      final HelmHostedRepositoryApiRequest request,
      @ApiParam(value = "Name of the repository to update") final String repositoryName);
}
