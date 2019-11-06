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
import java.util.Optional;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.repository.helm.internal.AssetKind;
import org.sonatype.repository.helm.internal.database.HelmProperties;

/**
 * @since 1.0.next
 */
public class AttributesMapAdapter
{
  private Map<HelmProperties, Object> attributesEnumMap;
  private AttributesMap contentAttributes = null;
  private String contentType = null;

  public AttributesMapAdapter(final AssetKind assetKind, final Map<String, Object> attributesMap) {
    attributesEnumMap = new EnumMap<>(HelmProperties.class);
    attributesMap.forEach((key, value) -> {
      Optional<HelmProperties> propertyOpt = HelmProperties.findByPropertyName(key);
      if (value != null && propertyOpt.isPresent()) {
        attributesEnumMap.put(propertyOpt.get(), value);
      }
    });

    if (assetKind != null) {
      attributesEnumMap.put(HelmProperties.ASSET_KIND, assetKind);
    }
  }

  public void addAdditionalContent(final Payload payload) {
    if (payload instanceof Content) {
      contentAttributes = ((Content) payload).getAttributes();
      contentType = payload.getContentType();
    }
  }

  public void populate(final NestedAttributesMap attributesMap) {
    attributesEnumMap.forEach((helmProperties, o) -> {
      if (helmProperties == HelmProperties.ASSET_KIND){
        attributesMap.set(helmProperties.getPropertyName(), getAssetKind().name());
      }
      else {
        attributesMap.set(helmProperties.getPropertyName(), o);
      }
    });
  }

  public AttributesMap getContentAttributes() {
    return contentAttributes;
  }

  public void setContentAttributes(AttributesMap contentAttributes) {
    this.contentAttributes = contentAttributes;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
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

  public String getAppVersion() {
    return getValue(HelmProperties.APP_VERSION, String.class);
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
