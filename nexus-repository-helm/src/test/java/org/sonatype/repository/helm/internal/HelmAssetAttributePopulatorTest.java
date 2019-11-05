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

import java.util.HashMap;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.repository.helm.AttributesMapAdapter;
import org.sonatype.repository.helm.internal.database.HelmProperties;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;

public class HelmAssetAttributePopulatorTest
    extends TestSupport
{
  private AttributesMapAdapter underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new AttributesMapAdapter(AssetKind.HELM_PACKAGE, setUpHelmAttributes());
  }

  @Test
  public void testPopulateNestedAttributesMapFromHelmAttributes() throws Exception {
    AttributesMap nestedAttributesMap = underTest.toAttributeMap(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));

    assertThat(nestedAttributesMap.get(HelmProperties.ICON.getPropertyName()).toString(), is(equalTo("icon")));
    assertThat(nestedAttributesMap.get(HelmProperties.DESCRIPTION.getPropertyName()).toString(), is(equalTo("description")));
    assertThat(nestedAttributesMap.get(HelmProperties.NAME.getPropertyName()).toString(), is(equalTo("name")));
    assertThat(nestedAttributesMap.get(HelmProperties.VERSION.getPropertyName()).toString(), is(equalTo("1.0.0")));
    assertThat(nestedAttributesMap.get(HelmProperties.APP_VERSION.getPropertyName()).toString(), is(equalTo("0.0.1")));
    assertThat(nestedAttributesMap.get(HelmProperties.MAINTAINERS.getPropertyName()), is(notNullValue()));
    assertThat(nestedAttributesMap.get(HelmProperties.SOURCES.getPropertyName()), is(notNullValue()));
  }

  private AttributesMap setUpHelmAttributes() {
    AttributesMap helmAttributes = new AttributesMap();

    helmAttributes.set(HelmProperties.ICON.getPropertyName(), "icon");
    helmAttributes.set(HelmProperties.DESCRIPTION.getPropertyName(), "description");
    helmAttributes.set(HelmProperties.NAME.getPropertyName(), "name");
    helmAttributes.set(HelmProperties.VERSION.getPropertyName(), "1.0.0");
    helmAttributes.set(HelmProperties.APP_VERSION.getPropertyName(), "0.0.1");
    helmAttributes.set(HelmProperties.SOURCES.getPropertyName(), HelmListTestHelper.getSourcesList());
    helmAttributes.set(HelmProperties.MAINTAINERS.getPropertyName(), HelmListTestHelper.getMaintainersList());

    return helmAttributes;
  }
}
