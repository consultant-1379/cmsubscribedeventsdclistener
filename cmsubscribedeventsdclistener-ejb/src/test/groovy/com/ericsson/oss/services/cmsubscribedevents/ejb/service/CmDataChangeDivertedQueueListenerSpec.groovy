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
package com.ericsson.oss.services.cmsubscribedevents.ejb.service

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent
import com.ericsson.oss.mediation.notifications.ComEcimNodeNotification
import com.ericsson.oss.mediation.notifications.ComEcimNodeMultipleNotification
import com.ericsson.oss.mediation.notifications.ComEcimNodeNotificationListType
import com.ericsson.oss.mediation.network.api.notifications.NodeNotification
import com.ericsson.oss.services.cmsubscribedevents.api.SubscribedEventsProcessor

import javax.jms.BytesMessage
import javax.jms.ObjectMessage
import spock.lang.Specification
import spock.lang.Unroll
import org.slf4j.Logger

/**
 * This class will test the listener for CmDataChangeDivertedQueue (CmDataChangeDivertedQueueListener).
 */
@SuppressWarnings("GroovyAccessibility")
class CmDataChangeDivertedQueueListenerSpec extends Specification {

    @ObjectUnderTest
    CmDataChangeDivertedQueueListener cmDataChangeDivertedQueueListener = new CmDataChangeDivertedQueueListener()

    @Unroll
    def 'when DpsDataChangedEvent listened, corresponding log is logged and eventsProcessor called'() {
        given: 'DpsDataChangedEvent is created'
            def fdn = "NetworkElement=LTE04dg2ERBS00035,CmFunction=1"
            def attributeChangedEvent = createAttributeChangeEvent(fdn)
            def objectMessage = Mock(ObjectMessage)
            objectMessage.getObject() >> attributeChangedEvent

        and: 'Mock for logger is set'
            cmDataChangeDivertedQueueListener.logger = Mock(Logger)

        and: 'Mock for eventsProcessor is set'
            cmDataChangeDivertedQueueListener.eventsProcessor = Mock(SubscribedEventsProcessor)

        when: 'Received the DpsAttributeChangedEvent in listener'
            cmDataChangeDivertedQueueListener.onMessage(objectMessage)

        then: 'The corresponding debug logs are logged'
            1 * cmDataChangeDivertedQueueListener.logger.debug('DPS Data Change Event received in CmDataChangeDivertedQueue for MO FDN {}', fdn)
            1 * cmDataChangeDivertedQueueListener.eventsProcessor.processEvent(attributeChangedEvent);
    }

    @Unroll
    def 'when ComEcimNodeNotification event listened, corresponding log is logged and eventsProcessor called'() {
        given: 'ComEcimNodeNotification event is created'
            def dn = "SubNetwork=sub1,ManagedElement=LTE04dg2ERBS00035"
            def time = new Date().toString()
            def comEcimNodeNotification = new ComEcimNodeNotification(dn, 3L, time, 1L, false)
            comEcimNodeNotification.setDn(dn)
            def objectMessage = Mock(ObjectMessage)
            objectMessage.getObject() >> comEcimNodeNotification

        and: 'Mock for logger is set'
            cmDataChangeDivertedQueueListener.logger = Mock(Logger)

        and: 'Mock for eventsProcessor is set'
            cmDataChangeDivertedQueueListener.eventsProcessor = Mock(SubscribedEventsProcessor)

        when: 'Received the ComEcimNodeNotification event in listener'
            cmDataChangeDivertedQueueListener.onMessage(objectMessage)

        then: 'The corresponding debug logs are logged'
            1 * cmDataChangeDivertedQueueListener.logger.debug('ComEcim Node Notification Event received in CmDataChangeDivertedQueue for MO FDN {} at DateTime {}', dn, time)
            1 * cmDataChangeDivertedQueueListener.eventsProcessor.processEvent(comEcimNodeNotification);
    }

    @Unroll
    def 'when NodeNotification event listened, corresponding log is logged and eventsProcessor called'() {
        given: 'NodeNotification event is created'
            def fdn = "MeContext=LTE02ERBS00006"
            def time = new Date()
            def nodeNotification = new NodeNotification()
            nodeNotification.setFdn(fdn)
            nodeNotification.setCreationTimestamp(time)
            def objectMessage = Mock(ObjectMessage)
            objectMessage.getObject() >> nodeNotification

        and: 'Mock for logger is set'
            cmDataChangeDivertedQueueListener.logger = Mock(Logger)

        and: 'Mock for eventsProcessor is set'
            cmDataChangeDivertedQueueListener.eventsProcessor = Mock(SubscribedEventsProcessor)

        when: 'Received the NodeNotification event in listener'
            cmDataChangeDivertedQueueListener.onMessage(objectMessage)

        then: 'The corresponding debug logs are logged'
            1 * cmDataChangeDivertedQueueListener.logger.debug('CPP Notification Event received in CmDataChangeDivertedQueue for MO FDN {} at DateTime {}', fdn, time)
            1 * cmDataChangeDivertedQueueListener.eventsProcessor.processEvent(nodeNotification);
    }

    @Unroll
    def 'when null event is received in queue listener, corresponding error is logged'() {
        given: 'Mock for logger is set'
            cmDataChangeDivertedQueueListener.logger = Mock(Logger)

        when: 'Received null event in listener'
            cmDataChangeDivertedQueueListener.onMessage(null)

        then: 'The corresponding error is logged'
            1 * cmDataChangeDivertedQueueListener.logger.error('Received null cm data change event')
    }

    @Unroll
    def 'when null MessageObject is listened, corresponding error is logged'() {
        given: 'null MessageObject is returned'
            def objectMessage = Mock(ObjectMessage)
            objectMessage.getObject() >> null

        and: 'Mock for logger is set'
            cmDataChangeDivertedQueueListener.logger = Mock(Logger)

        when: 'Received the event in listener'
            cmDataChangeDivertedQueueListener.onMessage(objectMessage)

        then: 'The corresponding debug logs are logged'
            1 * cmDataChangeDivertedQueueListener.logger.debug("Object Message {} and totalEventCounter is {}", null, _ as Long)
            1 * cmDataChangeDivertedQueueListener.logger.error('MessageObject is null')
    }

    @Unroll
    def 'when message is not an instance of ObjectMessage, returns null'() {
        given: 'null MessageObject is returned'
            def byteMessage = Mock(BytesMessage)

        and: 'Mock for logger is set'
            cmDataChangeDivertedQueueListener.logger = Mock(Logger)

        when: 'Received the event in listener'
            cmDataChangeDivertedQueueListener.onMessage(byteMessage)

        then: 'The corresponding debug logs are logged'
            0 * cmDataChangeDivertedQueueListener.logger.debug("Object Message {} and totalEventCounter is {}, ", _ as BytesMessage, _ as Long)
            1 * cmDataChangeDivertedQueueListener.logger.error('MessageObject is null')
    }

    @Unroll
    def 'when any other event type than NodeNotification, ComEcimNodeNotification or DpsDataChangedEvent is received in listener, that is logged and not processed'() {
        given: 'null MessageObject is returned'
            def dn = "SubNetwork=sub1,ManagedElement=LTE04dg2ERBS00035"
            def comEcimNodeMultipleNotification = new ComEcimNodeMultipleNotification(dn, 3L, ComEcimNodeNotificationListType.SYNC_LIST, 1L)
            def objectMessage = Mock(ObjectMessage)
            objectMessage.getObject() >> comEcimNodeMultipleNotification

        and: 'Mock for logger is set'
            cmDataChangeDivertedQueueListener.logger = Mock(Logger)

        when: 'Received the event in listener'
            cmDataChangeDivertedQueueListener.onMessage(objectMessage)

        then: 'The corresponding debug logs are logged'
            1 * cmDataChangeDivertedQueueListener.logger.info('Unexpected event {} with properties {}', objectMessage, objectMessage.getPropertyNames())
    }

    @Unroll
    def 'Upon receiving 2000 events, they are acknowledged'() {
        given: 'DpsDataChangedEvent is created'
            def fdn = "NetworkElement=LTE04dg2ERBS00035,CmFunction=1"
            def attributeChangedEvent = createAttributeChangeEvent(fdn)
            def objectMessage = Mock(ObjectMessage)
            objectMessage.getObject() >> attributeChangedEvent

        and: 'Mock for logger is set'
            cmDataChangeDivertedQueueListener.logger = Mock(Logger)

        when: 'Received 2000 DpsAttributeChangedEvent in listener'
            for (int i = 0; i < eventCount; i++) {
                cmDataChangeDivertedQueueListener.onMessage(objectMessage)
            }

        then: 'The acknowledge message is called'
            result * objectMessage.acknowledge()

        where:
            eventCount | result
            1999       | 0
            2000       | 1
    }

    def createAttributeChangeEvent(String fdn) {
        def oldValue = "{UNSYNCHRONIZED}"
        def newValue = "{PENDING}"
        Collection<AttributeChangeData> attributeChangeData = [
            new AttributeChangeData('syncStatus', oldValue, newValue, null, null)
        ]
        return new DpsAttributeChangedEvent(namespace: "OSS_NE_CM_DEF", type: "CmFunction", version: "1.0.1", poId: 57010, fdn: "${fdn}", bucketName: "Live", changedAttributes: attributeChangeData)

    }
}