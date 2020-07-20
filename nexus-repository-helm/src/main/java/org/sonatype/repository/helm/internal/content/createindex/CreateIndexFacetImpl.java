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
package org.sonatype.repository.helm.internal.content.createindex;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.repository.helm.internal.content.HelmContentFacet;
import org.sonatype.repository.helm.internal.content.recipe.HelmHostedFacet;
import org.sonatype.repository.helm.internal.createindex.CreateIndexFacet;
import org.sonatype.repository.helm.internal.createindex.HelmIndexInvalidationEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.repository.helm.internal.AssetKind.HELM_INDEX;

/**
 * Facet for rebuilding Helm index.yaml files
 *
 * @since 1.0.11
 */
@Named
public class CreateIndexFacetImpl
    extends FacetSupport
    implements CreateIndexFacet, Asynchronous
{
  private static final String INDEX_YAML = "/index.yaml";

  private final EventManager eventManager;

  private final long interval;

  private final AtomicBoolean acceptingEvents = new AtomicBoolean(true);

  //Prevents the same event from being fired multiple times
  private final AtomicBoolean eventFired = new AtomicBoolean(false);

  private CreateIndexService createIndexService;

  @Inject
  public CreateIndexFacetImpl(
      final EventManager eventManager,
      final CreateIndexService createIndexService,
      @Named("${nexus.helm.createrepo.interval:-1000}") final long interval)
  {
    this.eventManager = checkNotNull(eventManager);
    this.interval = interval;
    this.createIndexService = checkNotNull(createIndexService);
  }

  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(RepositoryCreatedEvent createdEvent) {
    // at repository creation time, create index.yaml with empty entries
    log.debug("Initializing index.yaml for hosted repository {}", getRepository().getName());
    invalidateIndex();
  }

  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(RepositoryDeletedEvent deletedEvent) {
    log.debug("Deleting index.yaml for hosted repository {}", getRepository().getName());
    deleteIndexYaml();
  }

  @Subscribe
  public void on(final HelmIndexInvalidationEvent event) {
    if (shouldProcess(event)) {
      acceptingEvents.set(false);
      maybeWait(event);

      log.info("Rebuilding Helm index for repository {}", getRepository().getName());

      try {
        acceptingEvents.set(true);
        eventFired.set(false);

        updateIndexYaml(createIndexService.buildIndexYaml(getRepository()));
      }
      finally {
        log.info("Finished rebuilding Helm index for repository {}", getRepository().getName());
      }
    }
  }

  protected void updateIndexYaml(final Content indexYaml) {
    if (indexYaml == null) {
      deleteIndexYaml();
    }
    else {
      createIndexYaml(indexYaml);
    }
  }

  private void createIndexYaml(final Content indexYaml) {
    Repository repository = getRepository();
    HelmContentFacet helmFacet = repository.facet(HelmContentFacet.class);
    helmFacet.putIndex(INDEX_YAML, indexYaml, HELM_INDEX);
  }

  private void deleteIndexYaml() {
    log.debug("Empty index.yaml returned, proceeding to delete asset");
    HelmHostedFacet hosted = getRepository().facet(HelmHostedFacet.class);
    boolean result = hosted.delete(INDEX_YAML);
    if (result) {
      log.info("Deleted index.yaml because of empty asset list");
    }
    else {
      log.warn("Unable to delete index.yaml asset");
    }
  }

  private boolean shouldProcess(final HelmIndexInvalidationEvent event) {
    return getRepository().getName().equals(event.getRepositoryName());
  }

  private void maybeWait(final HelmIndexInvalidationEvent event) {
    if (event.isWaitBeforeRebuild()) {
      try {
        Thread.sleep(interval);
      }
      catch (InterruptedException e) { // NOSONAR
        log.warn("Helm invalidation thread interrupted, proceeding with invalidation");
      }
    }
  }

  /**
   * We are accepting that the boolean values can change in the middle of this if statement. For example if
   * acceptingEvents is true and eventFired is false but before we check eventFired acceptingEvents is toggled, we will
   * add one additional process request which will be handled synchronously and not cause any issues.
   */
  @Override
  public synchronized void invalidateIndex() {
    if (acceptingEvents.get() && !eventFired.get()) {
      log.info("Scheduling rebuild of Helm metadata to start in {} seconds", interval / 1000);

      // Prevent another event being fired if one is already queued up
      eventFired.set(true);
      eventManager.post(new HelmIndexInvalidationEvent(getRepository().getName(), true));
    }
  }
}
