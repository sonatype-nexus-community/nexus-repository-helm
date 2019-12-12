/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.repository.helm.api;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;

import org.sonatype.nexus.repository.rest.api.AbstractRepositoriesApiResource;
import org.sonatype.nexus.repository.rest.api.AbstractRepositoryApiRequestToConfigurationConverter;
import org.sonatype.nexus.repository.rest.api.AuthorizingRepositoryManager;
import org.sonatype.nexus.repository.rest.api.RepositoriesApiResource;
import org.sonatype.nexus.rest.Resource;

@Named
@Singleton
@Path(RepositoriesApiResource.RESOURCE_URI + "/helm/hosted")
public class HelmHostedRepositoriesApiResource
    extends AbstractRepositoriesApiResource<HelmHostedRepositoryApiRequest>
    implements Resource, HelmHostedRepositoriesApiResourceDoc
{
  @Inject
  public HelmHostedRepositoriesApiResource(
      final AuthorizingRepositoryManager authorizingRepositoryManager,
      final AbstractRepositoryApiRequestToConfigurationConverter<HelmHostedRepositoryApiRequest> configurationAdapter)
  {
    super(authorizingRepositoryManager, configurationAdapter);
  }
}
