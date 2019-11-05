/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2017-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.repository.helm;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.repository.helm.internal.AssetKind;
import org.sonatype.repository.helm.internal.database.HelmProperties;

/**
 * @since 1.0.next
 */
public class AttributesMapAdapter
{
  private Map<HelmProperties, Object> attributesEnumMap;

  public AttributesMapAdapter(AssetKind assetKind) {
    attributesEnumMap = new EnumMap<>(HelmProperties.class);
    attributesEnumMap.put(HelmProperties.ASSET_KIND, assetKind);
  }

  public AttributesMapAdapter(AssetKind assetKind, AttributesMap attributesMap)
  {
    attributesEnumMap = new EnumMap<>(HelmProperties.class);
    attributesEnumMap.put(HelmProperties.ASSET_KIND, assetKind);

    attributesMap.forEach(entry -> {
      HelmProperties property = HelmProperties.findByPropertyName(entry.getKey())
          .orElseThrow(() -> new IllegalArgumentException(entry.getKey() + " in AttributeMap"));
      attributesEnumMap.put(property, entry.getValue());
    });
  }

  public Map<HelmProperties, Object> getAttributesEnumMap() {
    return attributesEnumMap;
  }

  //public NestedAttributesMap toAttributeMap() {
  //  return new NestedAttributesMap(P_ATTRIBUTES, toAttributeMap(new NestedAttributesMap()).backing());
  //}

  public NestedAttributesMap toAttributeMap(NestedAttributesMap attributesMap) {
    if (getAssetKind() == AssetKind.HELM_INDEX){
      return null;
    }
    attributesEnumMap.forEach((helmProperties, o) -> {
      if (helmProperties == HelmProperties.ASSET_KIND){
        //attributesMap.set(helmProperties.getPropertyName(), getAssetKind().getCacheType());
      }
      else {
        attributesMap.set(helmProperties.getPropertyName(), o);
      }
    });

    return attributesMap;
  }

  public AssetKind getAssetKind() {
    return getValue(HelmProperties.ASSET_KIND, AssetKind.class);
  }

  public String getName() {
    return getValue(HelmProperties.NAME, String.class);
  }

  public String getVersion() {
    return getValue(HelmProperties.VERSION, String.class);
  }

  private String getValue(HelmProperties property){
    return getValue(property, String.class);
  }

  public String getDescription() {
    return getValue(HelmProperties.DESCRIPTION, String.class);
  }

  public String getIcon() {
    return getValue(HelmProperties.ICON, String.class);
  }

  public List<Map<String, String>> getMaintainers() {

    return getValue(HelmProperties.MAINTAINERS, List.class);
  }

  public List<String> getSources() {
    return getValue(HelmProperties.SOURCES, List.class);
  }

  private <T> T getValue(HelmProperties property, Class<T> tClass){
    return tClass.cast(attributesEnumMap.get(property));
  }
}
