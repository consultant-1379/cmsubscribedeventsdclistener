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
package com.ericsson.oss.services.cmsubscribedevents.ejb.service;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.mediation.network.api.notifications.NodeNotification;
import com.ericsson.oss.mediation.notifications.ComEcimNodeNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.cmsubscribedevents.api.SubscribedEventsProcessor;

/**
 * This is a {@link MessageListener}, which listens to CmDataChangeDivertedQueue.
 */
@ApplicationScoped
public class CmDataChangeDivertedQueueListener implements MessageListener {

    private static final int ACKNOWLEDGEMENT_COUNT = 2000;
    private static Logger logger = LoggerFactory.getLogger(CmDataChangeDivertedQueueListener.class);
    private AtomicLong eventCounterForAcknowledgement = new AtomicLong();
    private AtomicLong totalEventCounter = new AtomicLong();

    @EServiceRef
    private SubscribedEventsProcessor eventsProcessor;

    @Override
    public void onMessage(final Message message) {

        if (message == null) {
            logger.error("Received null cm data change event");
        } else {
            eventCounterForAcknowledgement.incrementAndGet();
            processObject(message);
            acknowledgeMessage(message);
        }
    }

    private void processObject(final Message message) {
        try {
            Serializable object = getMessageObject(message);
            if (object != null) {
                if (object instanceof NodeNotification) {
                    processCppNotification(object);
                } else if (object instanceof ComEcimNodeNotification) {
                    processComEcimNotification(object);
                } else if (object instanceof DpsDataChangedEvent) {
                    processDpsNotification(object);
                } else {
                    logger.info("Unexpected event {} with properties {}", message, message.getPropertyNames());
                }
            } else {
                logger.error("MessageObject is null");
            }
        } catch (final Exception exception) {
            logger.error("Exception :: {} while processing message :: {}", exception, message);
        }
    }

    private void acknowledgeMessage(final Message message) {
        try {
            if (eventCounterForAcknowledgement.get() >= ACKNOWLEDGEMENT_COUNT) {
                message.acknowledge();
                logger.info("Acknowledgement sent at eventCounterForAcknowledgement size {}", eventCounterForAcknowledgement.shortValue());
                eventCounterForAcknowledgement.set(0);
            }
        } catch (final Exception exception) {
            logger.error("Exception :: {} caught while sending acknowledgement", exception.getMessage());

        }
    }

    private Serializable getMessageObject(final Message message) {
        Serializable object = null;
        try {
            if (message instanceof ObjectMessage) {
                object = ((ObjectMessage) message).getObject();
                logger.debug("Object Message {} and totalEventCounter is {}", object, totalEventCounter.incrementAndGet());
            }
        } catch (final JMSException exc) {
            logger.error("Exception while extracting JMS message {}. Details: {}", message, exc.getMessage());
        }
        return object;
    }

    private void processCppNotification(Serializable object) {
        final NodeNotification cppNotification = (NodeNotification) object;
        logger.debug("CPP Notification Event received in CmDataChangeDivertedQueue for MO FDN {} at DateTime {}", cppNotification.getFdn(),
            cppNotification.getCreationTimestamp());
        eventsProcessor.processEvent(cppNotification);
    }

    private void processComEcimNotification(Serializable object) {
        final ComEcimNodeNotification cEcimNotification = (ComEcimNodeNotification) object;
        logger.debug("ComEcim Node Notification Event received in CmDataChangeDivertedQueue for MO FDN {} at DateTime {}", cEcimNotification.getDn(),
            cEcimNotification.getTimestamp());
        eventsProcessor.processEvent(cEcimNotification);
    }

    private void processDpsNotification(Serializable object) {
        final DpsDataChangedEvent dpsNotification = (DpsDataChangedEvent) object;
        logger.debug("DPS Data Change Event received in CmDataChangeDivertedQueue for MO FDN {}", dpsNotification.getFdn());
        eventsProcessor.processEvent(dpsNotification);
    }
}