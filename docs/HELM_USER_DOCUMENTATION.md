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
## Helm Repositories

### Introduction

[Helm](https://helm.sh/) is a package management format used for Helm Charts, which are effectively recipes for Kubernetes. 

### Proxying Helm Repositories

You can set up an Helm proxy repository to access a remote repository location, for example to proxy the stable charts 
at [https://kubernetes-charts.storage.googleapis.com/](https://kubernetes-charts.storage.googleapis.com/)

To proxy a Helm repository, you simply create a new 'helm (proxy)' as documented in 
[Repository Management](https://help.sonatype.com/display/NXRM3/Configuration#Configuration-RepositoryManagement) in
details. Minimal configuration steps are:

- Define 'Name'
- Define URL for 'Remote storage' e.g. [https://kubernetes-charts.storage.googleapis.com/](https://kubernetes-charts.storage.googleapis.com/)
- Select a 'Blob store' for 'Storage'

### Configuring Helm 

Configuring Helm to use Nexus Repository is fairly easy! Once you have Helm up and running you'll want to run a command similar to the following:

```
helm repo add nexusrepo http://localhost:8081/repository/helm-proxy/
```

Replace `nexusrepo` with what you'd like the repo to be called, and the url with what the full url to your proxy repository is.

From that point you can install helm charts using a command similar to the following:

`helm install nexusrepo/mongodb`

If everything went smoothly, the command above will install the latest version of `mongodb`.

### Browsing Helm Repository Packages

You can browse Helm repositories in the user interface inspecting the components and assets and their details, as
described in [Browsing Repositories and Repository Groups](https://help.sonatype.com/display/NXRM3/Browsing+Repositories+and+Repository+Groups).
