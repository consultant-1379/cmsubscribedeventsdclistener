/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.presentation.itpf.server.cmsubscribedevents.test;

import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for EAR Deployment for the Arquillian environment.
 */
public final class Artifact {
    private static final Logger logger = LoggerFactory.getLogger(Artifact.class);
    private static final String GROUP_ID_CM_SUBSCRIBED_EVENTS = "com.ericsson.oss.services.cmsubscribedevents";
    private static final String ARTIFACT_ID_CM_SUBSCRIBED_EVENTS_DC_LISTENER_EJB = "cmsubscribedeventsdclistener-ejb";
    private static final String GROUP_ID_XNIO = "org.jboss.xnio";
    private static final String ARTIFACT_ID_XNIO_API = "xnio-api";

    private Artifact() {
    }

    static void addEarRequiredlibraries(final EnterpriseArchive archive) {
        logger.debug("Adding libs to ear: {0}", archive);
        archive.addAsLibraries(resolveAsFiles(GROUP_ID_CM_SUBSCRIBED_EVENTS, ARTIFACT_ID_CM_SUBSCRIBED_EVENTS_DC_LISTENER_EJB));
        archive.addAsLibraries(resolveAsFiles(GROUP_ID_XNIO, ARTIFACT_ID_XNIO_API));
    }

    static Archive<?> createJavaArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "cmsubscribedeventsdclistener-ejb-test.jar").addPackage(Artifact.class.getPackage());
        logger.debug("Creating JAR: {0}", archive);
        return archive;
    }

    private static File[] resolveAsFiles(final String groupId, final String artifactId) {
        return Maven.resolver().loadPomFromFile("pom.xml").resolve(groupId + ":" + artifactId).withTransitivity().asFile();
    }

    /**
     * This method copies the ears required for tests which are copied in ${jboss.home}/earsForTests folder into ${jboss.home}/standalone/deployments folder.
     * This is to avoid issues such as 'ConnectionFactory' not available if we try to directly put the ears under ${jboss.home}/standalone/deployments folder
     * using DPS Maven plugin before JBoss is started, because JBoss will be trying to deploy them even before JBoss is fully started.
     *
     */
    static void deployRequiredArtifacts() {
        final String jbossHome = System.getProperty("jboss.home");
        final Path source = Paths.get(jbossHome + File.separator + "earsForTests");
        if (Files.exists(source)) {
            final Path destination = Paths.get(jbossHome + File.separator + "standalone" + File.separator + "deployments");
            try {
                // Take advantage of JBoss deployment-scanner by simply copying the artifacts into the deployments folder.
                FileUtils.copyDirectory(source.toFile(), destination.toFile());
                // Wait for the artifacts to deploy before proceeding.
                Thread.sleep(10_000);
            } catch (final Exception e) {
                throw new RuntimeException("Failed to deploy required artifacts.", e);
            }
        }
    }

}
