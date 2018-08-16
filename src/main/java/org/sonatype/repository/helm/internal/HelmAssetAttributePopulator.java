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
package org.sonatype.repository.helm.internal;

import java.time.LocalDateTime;
import java.util.Date;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.repository.helm.internal.metadata.HelmAttributes;

import static org.sonatype.repository.helm.internal.database.HelmProperties.*;

/**
 * @since 0.0.2
 */
@Named
@Singleton
public class HelmAssetAttributePopulator
    extends ComponentSupport
{
  public void populate(final NestedAttributesMap attributes, final HelmAttributes helmAttributes) {
    attributes.set(NAME, helmAttributes.getName());
    attributes.set(DESCRIPTION, helmAttributes.getDescription());
    attributes.set(VERSION, helmAttributes.getVersion());
    attributes.set(ICON, helmAttributes.getIcon());
    attributes.set(SOURCES, helmAttributes.getSources());
    attributes.set(MAINTAINERS, helmAttributes.getMaintainers());
    attributes.set(APP_VERSION, helmAttributes.getAppVersion());
  }
}
