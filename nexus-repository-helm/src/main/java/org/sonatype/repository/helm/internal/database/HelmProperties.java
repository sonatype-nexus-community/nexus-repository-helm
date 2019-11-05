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
package org.sonatype.repository.helm.internal.database;

import java.util.Arrays;
import java.util.Optional;

import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * Database property names for Helm asset attributes
 *
 * @since 0.0.2
 */
public enum HelmProperties
{
  DESCRIPTION("description"),
  ENGINE("engine"),
  HOME("home"),
  ICON("icon"),
  APP_VERSION("appVersion"),
  API_VERSION("apiVersion"),
  KEYWORDS("keywords"),
  MAINTAINERS("maintainers"),
  NAME("name"),
  SOURCES("sources"),
  VERSION("version"),
  ASSET_KIND(P_ASSET_KIND);

  private String propertyName;

  HelmProperties(final String type) {
    this.propertyName = type;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public static Optional<HelmProperties> findByPropertyName(String propertyName) {
    return Arrays.stream(HelmProperties.values())
        .filter(properties -> propertyName.equals(properties.getPropertyName()))
        .findAny();
  }
}
