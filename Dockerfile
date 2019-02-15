FROM maven:3-jdk-8-alpine AS build
LABEL maintainer devops@cuebiq.com
ARG NEXUS_VERSION=3.15.2
ARG NEXUS_BUILD=01

COPY . /nexus-repository-helm/
RUN cd /nexus-repository-helm/; sed -i "s/3.15.2-01/${NEXUS_VERSION}-${NEXUS_BUILD}/g" pom.xml; \
    mvn clean package -Dmaven.test.skip=true;




FROM quay.io/pires/docker-jre:8u191
ENV NEXUS_VERSION 3.15.2-01
ENV NEXUS_DOWNLOAD_URL "https://download.sonatype.com/nexus/3"
ENV NEXUS_TARBALL_URL "${NEXUS_DOWNLOAD_URL}/nexus-${NEXUS_VERSION}-unix.tar.gz"
ENV NEXUS_TARBALL_ASC_URL "${NEXUS_DOWNLOAD_URL}/nexus-${NEXUS_VERSION}-unix.tar.gz.asc"
ENV GPG_KEY 0374CF2E8DD1BDFD

ENV SONATYPE_DIR /opt/sonatype
ENV NEXUS_HOME "${SONATYPE_DIR}/nexus"
ENV NEXUS_DATA /nexus-data
ENV NEXUS_CONTEXT ''
ENV SONATYPE_WORK ${SONATYPE_DIR}/sonatype-work
ARG HELM_VERSION=0.0.8


# Install nexus
RUN apk add --no-cache --update bash ca-certificates runit su-exec util-linux
RUN apk add --no-cache -t .build-deps wget gnupg openssl \
  && cd /tmp \
  && echo "===> Installing Nexus ${NEXUS_VERSION}..." \
  && wget -O nexus.tar.gz $NEXUS_TARBALL_URL; \
  wget -O nexus.tar.gz.asc $NEXUS_TARBALL_ASC_URL; \
    export GNUPGHOME="$(mktemp -d)"; \
    gpg --keyserver ha.pool.sks-keyservers.net --recv-keys $GPG_KEY; \
    gpg --batch --verify nexus.tar.gz.asc nexus.tar.gz; \
    rm -r $GNUPGHOME nexus.tar.gz.asc; \
  tar -xf nexus.tar.gz \
  && mkdir -p $SONATYPE_DIR \
  && mv nexus-$NEXUS_VERSION $NEXUS_HOME \
  && cd $NEXUS_HOME \
  && ls -las \
  && adduser -h $NEXUS_DATA -DH -s /sbin/nologin nexus \
  && chown -R nexus:nexus $NEXUS_HOME \
  && rm -rf /tmp/* \
  && apk del --purge .build-deps

# Configure nexus
RUN sed \
    -e '/^nexus-context/ s:$:${NEXUS_CONTEXT}:' \
    -i ${NEXUS_HOME}/etc/nexus-default.properties \
  && sed \
    -e '/^-Xms/d' \
    -e '/^-Xmx/d' \
    -i ${NEXUS_HOME}/bin/nexus.vmoptions

RUN mkdir -p ${NEXUS_DATA}/etc ${NEXUS_DATA}/log ${NEXUS_DATA}/tmp ${SONATYPE_WORK} \
  && ln -s ${NEXUS_DATA} ${SONATYPE_WORK}/nexus3 \
  && chown -R nexus:nexus ${NEXUS_DATA}

# Replace logback configuration
COPY logback.xml ${NEXUS_HOME}/etc/logback/logback.xml
COPY logback-access.xml ${NEXUS_HOME}/etc/logback/logback-access.xml

# Copy runnable script
COPY run /etc/service/nexus/run

VOLUME ${NEXUS_DATA}

EXPOSE 8081
WORKDIR ${NEXUS_HOME}
ENV INSTALL4J_ADD_VM_PARAMS="-Xms1200m -Xmx1200m"
COPY --from=build /nexus-repository-helm/target/nexus-repository-helm-${HELM_VERSION}.jar ${SONATYPE_DIR}/nexus/deploy
CMD ["/sbin/runsvdir", "-P", "/etc/service"]
