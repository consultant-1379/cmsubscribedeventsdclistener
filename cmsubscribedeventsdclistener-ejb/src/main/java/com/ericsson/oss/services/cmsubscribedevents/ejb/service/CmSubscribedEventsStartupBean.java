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
package com.ericsson.oss.services.cmsubscribedevents.ejb.service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

@Singleton
@Startup
public class CmSubscribedEventsStartupBean {

    @Inject
    private JmsQueueConnector jmsQueueConnector;

    @PostConstruct
    public void init() {
        jmsQueueConnector.startListening();
    }

    @PreDestroy
    public void cleanup() {
        jmsQueueConnector.stopListening();
    }

}
