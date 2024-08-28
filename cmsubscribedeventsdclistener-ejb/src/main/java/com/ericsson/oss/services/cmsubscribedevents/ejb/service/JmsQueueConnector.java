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

import com.ericsson.oss.itpf.sdk.eventbus.ChannelLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * This class manages all the configuration parameters required by JMS and the relative life cycle.
 */
@ApplicationScoped
public class JmsQueueConnector {

    private static Logger logger = LoggerFactory.getLogger(JmsQueueConnector.class);
    private static final String QUEUE_URI = "jms:/queue/CmDataChangeDivertedQueue";
    private static final Properties LOOKUP_PROPERTIES = new Properties();
    private static final String CONNECTION_FACTORY_JNDI_NAME = "java:/ConnectionFactory";
    private static final int DEFAULT_NUMBER_OF_DC_QUEUE_CONCURRENT_CONSUMERS = 1;
    private static final String CM_DATA_CHANGE_DIVERTED_QUEUE = "CmDataChangeDivertedQueue";

    private final List<Session> sessions = new LinkedList<>();
    private final List<MessageConsumer> consumers = new LinkedList<>();

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private InitialContext context;

    @Inject
    ChannelLocator channelLocator;

    @Inject
    CmDataChangeDivertedQueueListener cmDataChangeDivertedQueueListener;

    /***
     * Stops listening to messages.
     *
     */
    public void stopListening() {
        try {
            if (isActive()) {
                for (final MessageConsumer consumer : consumers) {
                    consumer.setMessageListener(null);
                    consumer.close();
                }
                final int size = consumers.size();
                consumers.clear();
                logger.info("stopped {} CmDataChangeDivertedQueue consumers", size);
                destroyQueueConnection();
            }
        } catch (final JMSException exception) {
            logger.error("Exception caught while destroying CmDataChangeDivertedQueue connection {}", exception.getMessage());
        }
    }

    private void destroyQueueConnection() {
        try {
            for (final Session session : sessions) {
                session.close();
            }
            final int size = sessions.size();
            sessions.clear();
            logger.info("Closed {} sessions of CmDataChangeDivertedQueue", size);

        } catch (final JMSException exception) {
            logger.error("Could not close CmDataChangeDivertedQueue sessions due to: {}", exception.getMessage());
            throw new JmsAdapterException("Could not close CmDataChangeDivertedQueue sessions.", exception);
        }
        try {
            connection.close();
        } catch (final JMSException exception) {
            logger.error("Could not close CmDataChangeDivertedQueue Connection due to: {}", exception.getMessage());
            throw new JmsAdapterException("Could not close CmDataChangeDivertedQueue Connection.", exception);
        }
    }

    /**
     * activates message listener - actually it registers bean instance as a message listener.
     *
     * @return true if activation is successful
     */
    public boolean startListening() {
        logger.info("trying to activate the message observer {}", this);
        if (isActive()) {
            logger.debug("{} is already active! returning now", this.getClass());
            return true;
        }
        try {
            startQueueConnection();
            consumers.addAll(createQueueConsumers(channelLocator.lookupChannel(QUEUE_URI).getChannelURI()));

            for (final MessageConsumer consumer : consumers) {
                consumer.setMessageListener(cmDataChangeDivertedQueueListener);
            }
            logger.info("Number of consumers registered are :: {} ", consumers.size());
            return true;
        } catch (final Exception exception) {
            logger.error("Listeners not started for CmDataChangeDivertedQueue due to exception :: {} ", exception.getMessage());
            return false;
        }
    }

    private boolean isActive() {
        return !consumers.isEmpty();
    }

    private void startQueueConnection() {
        initialiseConnectionFactory();
        try {
            connection = connectionFactory.createConnection();
            logger.info("Successfully created CmDataChangeDivertedQueueConnection. Will start it now...");
            connection.start();
        } catch (final JMSException jmsException) {
            logger.error("Exception while configuring JMS connection for CmDataChangeDivertedQueue due to: {}", jmsException.getMessage());
            throw new JmsAdapterException("Exception while configuring JMS connection for CmDataChangeDivertedQueue.", jmsException);
        }
    }

    private void initialiseConnectionFactory() {
        if (connectionFactory == null) {
            LOOKUP_PROPERTIES.putAll(System.getProperties());
            LOOKUP_PROPERTIES.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            LOOKUP_PROPERTIES.put("jboss.naming.client.ejb.context", "true");

            try {
                context = new InitialContext(LOOKUP_PROPERTIES);
                logger.info("Looking up [{}] from initial context", CONNECTION_FACTORY_JNDI_NAME);
                logger.trace("JNDI properties are {}", LOOKUP_PROPERTIES);

                connectionFactory = (ConnectionFactory) context.lookup(CONNECTION_FACTORY_JNDI_NAME);
            } catch (final NamingException namingException) {
                logger.error("Could not find JMS connection by name {} due to: {}", CONNECTION_FACTORY_JNDI_NAME, namingException);
                throw new IllegalStateException("Could not find JMS connection by name [" + CONNECTION_FACTORY_JNDI_NAME + "]", namingException);
            }
        } else {
            logger.info("connection factory is already created");
        }
    }

    private List<MessageConsumer> createQueueConsumers(final String destinationUri) {
        final List<MessageConsumer> messageConsumers = new LinkedList<>();

        final Integer configuredNumberOfQueueConcurrentConsumers = fetchConfiguredNumberOfQueueConcurrentConsumers(CM_DATA_CHANGE_DIVERTED_QUEUE,
            DEFAULT_NUMBER_OF_DC_QUEUE_CONCURRENT_CONSUMERS);

        try {
            final Destination destination = (Destination) context.lookup(destinationUri);
            logger.debug("Successfully found JMS destination for {} is :: {} ", destinationUri, destination);

            for (int i = 0; i < configuredNumberOfQueueConcurrentConsumers; i++) {
                final Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
                sessions.add(session);
                final MessageConsumer consumer = session.createConsumer(destination);
                messageConsumers.add(consumer);
            }
            logger.info("Successfully created consumer for destination {}.", destinationUri);
        } catch (final JMSException | NamingException exception) {
            logger.error("Could not create JMS Consumer for CmDataChangeDivertedQueue due to: {}", exception.getMessage());
            throw new JmsAdapterException("Could not create JMS Consumer for CmDataChangeDivertedQueue", exception);
        }
        return messageConsumers;
    }

    private Integer fetchConfiguredNumberOfQueueConcurrentConsumers(final String queueName, final int defaultNumberOfConsumers) {
        Integer numberOfQueueConcurrentConsumers = defaultNumberOfConsumers;
        try {
            final Integer jvmPropertyValueForQueue = findJvmProperty(getNumberOfConsumersForQueueJvmPropertyName(queueName));
            if (jvmPropertyValueForQueue != null) {
                numberOfQueueConcurrentConsumers = jvmPropertyValueForQueue;
            }
            logger.info("Configuration parameter [{}] has null value. Will ignore this and use the default value [{}]",
                getNumberOfConsumersForQueueJvmPropertyName(queueName), defaultNumberOfConsumers);
        } catch (final Exception exception) {
            logger.error("Was not able to find configuration value for [{}] due to {}. Will use the default value [{}]",
                getNumberOfConsumersForQueueJvmPropertyName(queueName), exception, defaultNumberOfConsumers);
        }
        return numberOfQueueConcurrentConsumers;
    }

    private Integer findJvmProperty(final String propertyName) {
        final String value = System.getProperty(propertyName);
        if ((value).equals("")) {
            logger.info("No JVM property {} found", propertyName);
            return null;
        } else {
            logger.debug("Value {} found for JVM property {}", value, propertyName);
            return Integer.valueOf(value);
        }
    }

    private String getNumberOfConsumersForQueueJvmPropertyName(final String queueName) {
        return "sdk.eventbus.jms.concurrent.queue.listeners.number" + "." + queueName;
    }
}
