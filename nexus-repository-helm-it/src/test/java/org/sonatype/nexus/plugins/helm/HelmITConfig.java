package org.sonatype.nexus.plugins.helm;

import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import org.ops4j.pax.exam.Option;

import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.nexusFeature;

public class HelmITConfig
{
  public static Option[] configureHelmBase() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-helm"),
        systemProperty("nexus-exclude-features").value("nexus-cma-community")
    );
  }
}
