<!--

    Sonatype Nexus (TM) Open Source Version
    Copyright (c) 2018-present Sonatype, Inc.
    All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.

    This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
    which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.

    Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
    of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
    Eclipse Foundation. All other trademarks are the property of their respective owners.

-->
# Nexus Repository Helm Format

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.sonatype.nexus.plugins/nexus-repository-helm/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.sonatype.nexus.plugins/nexus-repository-helm)

[![CircleCI](https://circleci.com/gh/sonatype-nexus-community/nexus-repository-helm.svg?style=shield)](https://circleci.com/gh/sonatype-nexus-community/nexus-repository-helm)

[![Join the chat at https://gitter.im/sonatype/nexus-developers](https://badges.gitter.im/sonatype/nexus-developers.svg)](https://gitter.im/sonatype/nexus-developers?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![DepShield Badge](https://depshield.sonatype.org/badges/sonatype-nexus-community/nexus-repository-helm/depshield.svg)](https://depshield.github.io)

> **Huzzah!** Helm is now part of Nexus Repository Manager. Version 3.21 includes the Helm plugin by default. 
>The plugin source code is now in [nexus-public](https://github.com/sonatype/nexus-public) in [nexus-repository-helm](https://github.com/sonatype/nexus-public/tree/master/plugins/nexus-repository-helm).

> **Filing issues:** Upgrade to the latest version of Nexus Repository Manager 3, to get the latest fixes and improvements, before filing any issues or feature requests at https://issues.sonatype.org/.

> **Upgrading:** If you are using a version prior to 3.21 and upgrade to a newer version you will not be able to install the community plugin. 
>No other changes are required and your existing data will remain intact.

# Table Of Contents
* [Release notes](https://help.sonatype.com/display/NXRM3/2020+Release+Notes#id-2020ReleaseNotes-RepositoryManager3.21.0)
* [Developing](#developing)
   * [Requirements](#requirements)
   * [Building](#building)
* [Using Helm with Nexus Repository Manger 3](#using-helm-with-nexus-repository-manager-3)
* [Installing the plugin](#installing-the-plugin)
   * [Permanent Install](#permanent-reinstall)
* [The Fine Print](#the-fine-print)
* [Getting Help](#getting-help)

## Developing

### Requirements

* [Apache Maven 3.3.3+](https://maven.apache.org/install.html)
* [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* Network access to https://repository.sonatype.org/content/groups/sonatype-public-grid

Also, there is a good amount of information available at [Bundle Development](https://help.sonatype.com/display/NXRM3/Bundle+Development)

### Building

To build the project and generate the bundle use Maven

    mvn clean package

If everything checks out, the bundle for Helm should be available in the `target` folder.

In the examples below, substitute `<helm_version>` with the current version of the helm format plugin.

#### Build with Docker

`docker build -t nexus-repository-helm:<helm_version> .`

#### Run as a Docker container

`docker run -d -p 8081:8081 --name nexus nexus-repository-helm:<helm_version>` 

For further information like how to persist volumes check out [the GitHub repo for our official image](https://github.com/sonatype/docker-nexus3).

After allowing some time to spin up, the application will be available from your browser at http://localhost:8081.

To read the generated admin password for your first login to the web UI, you can use the command below against the running docker container:

      docker exec -it nexus cat /nexus-data/admin.password && echo
      
  Once logged into the application UI as `admin` using the generated password, you may also want to 
  turn on "Enable anonymous access" when prompted by the setup wizard.     

## Using Helm With Nexus Repository Manager 3

[We have detailed instructions on how to get started here!](https://help.sonatype.com/repomanager3/formats/helm-repositories)

## Compatibility with Nexus Repository Manager 3 Versions

The table below outlines what version of Nexus Repository the plugin was built against

| Plugin Version        | Nexus Repository Version |
|-----------------------|--------------------------|
| v0.0.6                | 3.13.0-01                |
| v0.0.7                | 3.14.0-04                |
| v0.0.8                | 3.15.2-01                |
| v0.0.9                | 3.16.2-01                |
| v0.0.10               | 3.17.0-01                |
| v0.0.11               | 3.18.0-01                |
| v0.0.12               | 3.18.0-01                |
| v0.0.13               | 3.18.0-01                |
| v1.0.2 In product     | 3.21.0+                  |
All released versions can be found [here](https://github.com/sonatype-nexus-community/nexus-repository-helm/releases).

## Features Implemented In This Plugin 

| Feature | Implemented          |
|---------|----------------------|
| Proxy   | :heavy_check_mark:   |
| Hosted  | :heavy_check_mark:   |
| Group   |                      |
  
## Installing The Plugin
In Nexus Repository Manager 3.21+ `Helm` format is already included. So there is no need to install it. But if you want to reinstall the plugin with your improvements then following instructions will be useful. <br> <b>Note:</b> Using an unofficial version of the plugin is not supported by the Sonatype Support team.  

### Permanent Reinstall

* Copy the new bundle into `<nexus_dir>/system/org/sonatype/nexus/plugins/nexus-repository-helm/<helm_version>/nexus-repository-helm-<helm_version>.jar`
* Edit `<nexus_dir>/system/org/sonatype/nexus/assemblies/nexus-cma-feature/3.x.y/nexus-cma-feature-3.x.y-features.xml`  changing helm to your build version (examples, the actual lines surrounding may vary):

   ```
         <feature version="3.a.b">nexus-repository-p2</feature>
         <feature version="<helm_version>">nexus-repository-helm</feature>
         <feature version="3.x.y.xy">nexus-repository-raw</feature>
     </feature>
   ```
   And
   ```
    <feature name="nexus-repository-helm" description="org.sonatype.nexus.plugins:nexus-repository-helm" version="<helm_version>">
        <details>org.sonatype.nexus.plugins:nexus-repository-helm</details>
        <bundle>mvn:org.sonatype.nexus.plugins/nexus-repository-helm/<helm_version></bundle>
    </feature>
   ```
This will cause the plugin to be loaded and started with each startup of Nexus Repository Manager.

NOTE: The file location changed in version 3.21. For older versions, edit these files:
* If you are using OSS edition, make these mods in: `<nexus_dir>/system/com/sonatype/nexus/assemblies/nexus-oss-feature/3.x.y/nexus-oss-feature-3.x.y-features.xml`
* If you are using PRO edition, make these mods in: `<nexus_dir>/system/com/sonatype/nexus/assemblies/nexus-pro-feature/3.x.y/nexus-pro-feature-3.x.y-features.xml`

Additionally, prior to 3.21 the lines did not exist so they'd need to be added instead of edited.

## The Fine Print

Starting from version 3.21+ the `Helm` plugin is supported by Sonatype, but still is a contribution of ours
to the open source community (read: you!)

Phew, that was easier than I thought. Last but not least of all:

Have fun creating and using this plugin and the Nexus platform, we are glad to have you here!

## Getting help

Looking to contribute to our code but need some help? There's a few ways to get information:

* If using Nexus Repository Manager 3.21+ or later please file any issues at https://issues.sonatype.org/.
* Chat with us on [Gitter](https://gitter.im/sonatype/nexus-developers)
* Check out the [Nexus3](http://stackoverflow.com/questions/tagged/nexus3) tag on Stack Overflow
* Check out the [Nexus Repository User List](https://groups.google.com/a/glists.sonatype.com/forum/?hl=en#!forum/nexus-users)
