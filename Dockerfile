ARG NEXUS_VERSION=3.19.0

FROM maven:3-jdk-8-alpine AS build
ARG NEXUS_VERSION=3.19.0
ARG NEXUS_BUILD=01

COPY nexus-repository-helm /nexus-repository-helm/
RUN cd /nexus-repository-helm/; sed -i "s/nexus-repository-base/nexus-plugins/g" pom.xml; \
    sed -i "s/0.0.13/${NEXUS_VERSION}-${NEXUS_BUILD}/g" pom.xml; \
    mvn clean package;

FROM sonatype/nexus3:$NEXUS_VERSION
ARG NEXUS_VERSION=3.19.0
ARG NEXUS_BUILD=01
ARG COMP_VERSION=1.18
ARG TARGET_DIR=/opt/sonatype/nexus/system/org/sonatype/nexus/plugins/nexus-repository-helm/${NEXUS_VERSION}-${NEXUS_BUILD}/
USER root
RUN mkdir -p ${TARGET_DIR}; \
    sed -i 's@nexus-repository-maven</feature>@nexus-repository-maven</feature>\n        <feature prerequisite="false" dependency="false">nexus-repository-helm</feature>@g' /opt/sonatype/nexus/system/org/sonatype/nexus/assemblies/nexus-core-feature/${NEXUS_VERSION}-${NEXUS_BUILD}/nexus-core-feature-${NEXUS_VERSION}-${NEXUS_BUILD}-features.xml; \
    sed -i 's@<feature name="nexus-repository-maven"@<feature name="nexus-repository-helm" description="org.sonatype.nexus.plugins:nexus-repository-helm" version="'"${NEXUS_VERSION}-${NEXUS_BUILD}"'">\n        <details>org.sonatype.nexus.plugins:nexus-repository-helm</details>\n        <bundle>mvn:org.sonatype.nexus.plugins/nexus-repository-helm/'"${NEXUS_VERSION}-${NEXUS_BUILD}"'</bundle>\n        <bundle>mvn:org.apache.commons/commons-compress/'"${COMP_VERSION}"'</bundle>\n   </feature>\n    <feature name="nexus-repository-maven"@g' /opt/sonatype/nexus/system/org/sonatype/nexus/assemblies/nexus-core-feature/${NEXUS_VERSION}-${NEXUS_BUILD}/nexus-core-feature-${NEXUS_VERSION}-${NEXUS_BUILD}-features.xml;
COPY --from=build /nexus-repository-helm/target/nexus-repository-helm-${NEXUS_VERSION}-${NEXUS_BUILD}.jar ${TARGET_DIR}
USER nexus
