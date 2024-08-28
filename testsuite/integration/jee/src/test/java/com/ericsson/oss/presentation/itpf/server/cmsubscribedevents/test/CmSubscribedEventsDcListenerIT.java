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

import static com.ericsson.oss.presentation.itpf.server.cmsubscribedevents.test.Artifact.addEarRequiredlibraries;
import static com.ericsson.oss.presentation.itpf.server.cmsubscribedevents.test.Artifact.deployRequiredArtifacts;
import static com.ericsson.oss.presentation.itpf.server.cmsubscribedevents.test.Artifact.createJavaArchive;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.sdk.eventbus.Channel;
import com.ericsson.oss.itpf.sdk.eventbus.annotation.Endpoint;
import com.ericsson.oss.mediation.network.api.notifications.NodeNotification;
import com.ericsson.oss.mediation.notifications.ComEcimNodeNotification;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Integration tests for testing the cmsubscribed events received in CmDataChangeDivertedQueue and it's processing via Arquillian.
 */
@RunWith(Arquillian.class)
public final class CmSubscribedEventsDcListenerIT {

    private static final Logger logger = LoggerFactory.getLogger(CmSubscribedEventsDcListenerIT.class);

    @Inject
    @Endpoint("jms:/queue/CmDataChangeDivertedQueue")
    private Channel cmDataChangeDivertedQueue;

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(final Description description) {
            logger.info("*******************************");
            logger.info("Starting test: {}()", description.getMethodName());
        }

        @Override
        protected void finished(final Description description) {
            logger.info("Ending test: {}()", description.getMethodName());
            logger.info("*******************************");
        }
    };

    @Deployment(name = "CMSubscribedEventsNbiTestEar")
    public static Archive<?> createTestArchive() {
        logger.info("Creating deployment: cmsubscribedeventsdclistenerTestEar=");
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "CMsubscribedeventsdclistenerTestEar.ear");
        addEarRequiredlibraries(ear);
        ear.addAsLibrary(createJavaArchive());
        //JBoss has started, so we can now deploy the required EARs.
        deployRequiredArtifacts();
        return ear;
    }

    @Test
    @InSequence(1)
    public void nodeNotificationEventTest() throws InterruptedException {

        NodeNotification nodeNotification = createNodeNotification();

        cmDataChangeDivertedQueue.send(nodeNotification);
        TimeUnit.SECONDS.sleep(5);

        //TODO assert with the processing of NodeNotificationEvent notification in upcoming stories.
    }


    @Test
    @InSequence(2)
    public void comEcimNodeNotificationEventTest() throws InterruptedException {

        ComEcimNodeNotification comEcimNodeNotification = createComEcimNodeNotification();

        cmDataChangeDivertedQueue.send(comEcimNodeNotification);
        TimeUnit.SECONDS.sleep(5);

        //TODO assert with the processing of ComEcimNodeNotification event in upcoming stories.
    }

    @Test
    @InSequence(3)
    public void dpsDataChangedNotificationEventTest() throws InterruptedException {

        DpsAttributeChangedEvent attributeChangedEvent = createDpsAttributeChangedEvent();

        cmDataChangeDivertedQueue.send(attributeChangedEvent);
        TimeUnit.SECONDS.sleep(5);

        //TODO assert with the processing of DpsDataChangedEvent notification in upcoming stories.
    }

    private NodeNotification createNodeNotification() {
        String fdn = "MeContext=LTE02ERBS00006";
        Date time = new Date();
        NodeNotification nodeNotification = new NodeNotification();
        nodeNotification.setFdn(fdn);
        nodeNotification.setCreationTimestamp(time);
        return nodeNotification;
    }

    private ComEcimNodeNotification createComEcimNodeNotification() {
        String dn = "SubNetwork=sub1,ManagedElement=LTE04dg2ERBS00035";
        String time = new Date().toString();
        ComEcimNodeNotification comEcimNodeNotification = new ComEcimNodeNotification(dn, 3L, time, 1L, false);
        comEcimNodeNotification.setDn(dn);
        return comEcimNodeNotification;
    }

    private DpsAttributeChangedEvent createDpsAttributeChangedEvent() {
        String fdn = "NetworkElement=LTE04dg2ERBS00035,CmFunction=1";
        String oldValue = "{UNSYNCHRONIZED}";
        String newValue = "{PENDING}";
        List<AttributeChangeData> attributeChangeData = new ArrayList<>();
        attributeChangeData.add(new AttributeChangeData("syncStatus", oldValue, newValue, null, null));
        return new DpsAttributeChangedEvent("OSS_NE_CM_DEF", "CmFunction", "1.0.1", Long.valueOf(57010), fdn,  "Live", attributeChangeData);
    }

}