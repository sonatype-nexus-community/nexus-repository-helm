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
package org.sonatype.repository.helm.internal.metadata;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Object for storing Helm specific attributes from Chart.yaml
 *
 * @since 0.0.1
 */
public final class HelmAttributes
{
  private String description;
  private String engine;
  private String home;
  private String icon;
  private String appVersion;
  private List<String> keywords;
  private List<Map<String, String>> maintainers;
  private String name;
  private List<String> sources;
  private String version;

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getEngine() {
    return engine;
  }

  public void setEngine(final String engine) {
    this.engine = engine;
  }

  public String getHome() {
    return home;
  }

  public void setHome(final String home) {
    this.home = home;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(final String icon) {
    this.icon = icon;
  }

  public List<String> getKeywords() {
    return keywords;
  }

  public void setKeywords(final List<String> keywords) {
    this.keywords = Collections.unmodifiableList(keywords);
  }

  public List<Map<String, String>> getMaintainers() {
    return maintainers;
  }

  public void setMaintainers(final List<Map<String, String>> maintainers) {
    this.maintainers = Collections.unmodifiableList(maintainers);
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public List<String> getSources() {
    return sources;
  }

  public void setSources(final List<String> sources) {
    this.sources = Collections.unmodifiableList(sources);
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  public String getAppVersion() {
    return appVersion;
  }

  public void setAppVersion(final String appVersion) {
    this.appVersion = appVersion;
  }
}
