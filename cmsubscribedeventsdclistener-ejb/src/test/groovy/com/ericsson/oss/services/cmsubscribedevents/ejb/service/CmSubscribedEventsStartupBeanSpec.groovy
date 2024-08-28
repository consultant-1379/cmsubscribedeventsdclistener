/*------------------------------------------------------------------------------
*******************************************************************************
* COPYRIGHT Ericsson 2023
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
import spock.lang.Specification
import spock.lang.Unroll

import javax.inject.Inject

/**
 * This class tests the CmSubscribedEventsStartupBean.
 */
class CmSubscribedEventsStartupBeanSpec extends Specification {

    @ObjectUnderTest
    CmSubscribedEventsStartupBean cmSubscribedEventsStartupBean = new CmSubscribedEventsStartupBean()

    @Inject
    JmsQueueConnector jmsQueueConnector = Mock(JmsQueueConnector)

    @Unroll
    def 'when postConstruct and preDestroy methods are invoked from startup bean, jmsQueueConnector start listening and stop listening respectively'() {
        given:
            cmSubscribedEventsStartupBean.jmsQueueConnector = jmsQueueConnector

        when: 'init() is getting called'
            cmSubscribedEventsStartupBean.init()

        then: 'JmsQueueConnector startListening() is invoked'
            1 * jmsQueueConnector.startListening()

        when: 'cleanup() is getting called'
            cmSubscribedEventsStartupBean.cleanup()

        then: 'JmsQueueConnector stopListening() is invoked'
            1 * jmsQueueConnector.stopListening()
    }
}