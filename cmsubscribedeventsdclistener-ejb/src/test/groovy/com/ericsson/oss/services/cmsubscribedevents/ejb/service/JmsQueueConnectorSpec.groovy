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
import com.ericsson.oss.itpf.sdk.eventbus.ChannelLocator
import com.ericsson.oss.itpf.sdk.eventbus.Channel
import javax.jms.JMSException
import org.mockito.Mockito
import org.slf4j.Logger
import spock.lang.Specification
import javax.jms.Destination

import javax.jms.Connection
import javax.jms.ConnectionFactory
import javax.jms.MessageConsumer
import javax.jms.Session
import javax.naming.InitialContext

/**
 * This class will test the JmsQueueConnector.
 */
class JmsQueueConnectorSpec extends Specification {

    @ObjectUnderTest
    JmsQueueConnector jmsQueueConnector = new JmsQueueConnector()

    Connection connection = Mock(Connection)
    InitialContext context = Mock(InitialContext)
    ConnectionFactory connectionFactory = Mock(ConnectionFactory)
    Session session = Mock(Session)
    MessageConsumer messageConsumer = Mock(MessageConsumer)
    Destination destination = Mock(Destination)

    def "should throw IllegalStateException when JMS connection factory is not found"() {
        given: "ensure connectionFactory is null"
            jmsQueueConnector.connectionFactory = null

        when: "initialiseConnectionFactory is invoked"
            jmsQueueConnector.initialiseConnectionFactory()

        then: "IllegalStateException is thrown"
            def ex = thrown(IllegalStateException)
            ex.message.contains(jmsQueueConnector.CONNECTION_FACTORY_JNDI_NAME)
    }

    def "when JMS connection factory is found, no exception is thrown"() {
        given: "lookup return the connectionFactory"
            jmsQueueConnector.context = context
            jmsQueueConnector.connectionFactory = connectionFactory
            context.lookup(jmsQueueConnector.CONNECTION_FACTORY_JNDI_NAME) >> connectionFactory

        when: "initialiseConnectionFactory is invoked"
            jmsQueueConnector.initialiseConnectionFactory()

        then: "no exception is thrown"
            notThrown(IllegalStateException)
    }

    def "should close connection, session and consumer when stopListening() is called"() {
        given: "required mocks are defined"
            jmsQueueConnector.sessions.add(session)
            jmsQueueConnector.consumers.add(messageConsumer)
            jmsQueueConnector.connection = connection

        when: "stopListening is invoked"
            jmsQueueConnector.stopListening()

        then: "consumers, session and connection should be closed"
            1 * messageConsumer.close()
            1 * session.close()
            1 * messageConsumer.setMessageListener(null)
            1 * connection.close()
    }

    def "if JMSException happens during session closing, then JmsAdapterException is thrown"() {
        given: "required mocks are defined"
            Session session = Mockito.mock(Session)
            jmsQueueConnector.sessions.add(session)
            jmsQueueConnector.consumers.add(messageConsumer)
            jmsQueueConnector.connection = connection
            jmsQueueConnector.logger = Mock(Logger)
            Mockito.when(session.close()).thenThrow(new JMSException(_ as String))

        when: "stopListening is invoked"
            jmsQueueConnector.stopListening()

        then: "JmsAdapterException is thrown"
            thrown(JmsAdapterException)
    }

    def "if JMSException happens during connection closing, then JmsAdapterException is thrown"() {
        given: "required mocks are defined"
            Connection connection = Mockito.mock(Connection)
            jmsQueueConnector.sessions.add(session)
            jmsQueueConnector.consumers.add(messageConsumer)
            jmsQueueConnector.connection = connection
            jmsQueueConnector.logger = Mock(Logger)
            Mockito.when(connection.close()).thenThrow(new JMSException(_ as String))

        when: "stopListening is invoked"
            jmsQueueConnector.stopListening()

        then: "JmsAdapterException is thrown"
            thrown(JmsAdapterException)
    }

    def "if JMSException happens during consumer closing, then corresponding error is logged"() {
        given: "required mocks are defined"
            jmsQueueConnector.sessions.add(session)
            MessageConsumer messageConsumer = Mockito.mock(MessageConsumer)
            jmsQueueConnector.consumers.add(messageConsumer)
            jmsQueueConnector.connection = connection
            jmsQueueConnector.logger = Mock(Logger)
            Mockito.when(messageConsumer.close()).thenThrow(new JMSException(_ as String))

        when: "stopListening is invoked"
            jmsQueueConnector.stopListening()

        then: "corresponding error is logged"
            jmsQueueConnector.logger.error("Exception caught while destroying CmDataChangeDivertedQueue connection {}", _ as String)
    }

    def "should do nothing if consumers are already empty when stopListening() is called"() {
        given: "required mocks are defined"
            jmsQueueConnector.sessions.add(session)
            jmsQueueConnector.connection = connection

        when: "stopListening is invoked"
            jmsQueueConnector.stopListening()

        then: "do nothing"
            0 * messageConsumer.close()
            0 * session.close()
            0 * messageConsumer.setMessageListener(null)
            0 * connection.close()
    }

    def "should return true when startListening() is invoked and connectionFactory is returned"() {
        given: "required mocks are defined"
            jmsQueueConnector.sessions.add(session)
            jmsQueueConnector.consumers.add(messageConsumer)
            jmsQueueConnector.connection = connection
            connectionFactory.createConnection() >> connection

        when: "startListening is invoked"
            def result = jmsQueueConnector.startListening()

        then: "return true"
            result == true
    }

    def "should return false when startListening() is called could not find connectionFactory"() {
        given: "ensure connection factory is null"
            jmsQueueConnector.connectionFactory = null

        when: "startListening is invoked"
            def result = jmsQueueConnector.startListening()

        then: "return false"
            result == false
    }

    def "when startListening() is called, connection should start, consumers are created and messageListener should be set"() {
        given: "required mocks are defined"
            jmsQueueConnector.sessions.add(session)
            jmsQueueConnector.context = context
            jmsQueueConnector.connectionFactory = connectionFactory
            context.lookup(jmsQueueConnector.CONNECTION_FACTORY_JNDI_NAME) >> connectionFactory
            jmsQueueConnector.connection = connection
            connectionFactory.createConnection() >> connection
            connection.createSession(false, Session.CLIENT_ACKNOWLEDGE) >> session
            context.lookup(_ as String) >> destination
            ChannelLocator channelLocator = Mock(ChannelLocator)
            jmsQueueConnector.channelLocator = channelLocator
            Channel channel = Mock(Channel)
            channelLocator.lookupChannel(jmsQueueConnector.QUEUE_URI) >> channel
            channel.getChannelURI() >> "CmDataChangeQueueUri"
            CmDataChangeDivertedQueueListener cmDataChangeDivertedQueueListener = Mock(CmDataChangeDivertedQueueListener)
            jmsQueueConnector.cmDataChangeDivertedQueueListener = cmDataChangeDivertedQueueListener
            session.createConsumer(destination) >> messageConsumer

        when: "startListening is invoked"
            def result = jmsQueueConnector.startListening()

        then: "connection is started and message listener is set"
            1 * connection.start()
            1 * messageConsumer.setMessageListener(cmDataChangeDivertedQueueListener)
            result == true
    }

    def "when JMSException is thrown during start connection, then corresponding error is logged and return false"() {
        given: "required mocks are defined"
            Connection connection = Mockito.mock(Connection)
            jmsQueueConnector.sessions.add(session)
            jmsQueueConnector.context = context
            jmsQueueConnector.connectionFactory = connectionFactory
            context.lookup(jmsQueueConnector.CONNECTION_FACTORY_JNDI_NAME) >> connectionFactory
            jmsQueueConnector.connection = connection
            connectionFactory.createConnection() >> connection
            jmsQueueConnector.logger = Mock(Logger)
            Mockito.when(connection.start()).thenThrow(new JMSException(_ as String))

        when: "startListening is invoked"
            def result = jmsQueueConnector.startListening()

        then: "corresponding error is logged and return false"
            jmsQueueConnector.logger.error("Exception while configuring JMS connection for CmDataChangeDivertedQueue due to: {}", _ as String)
            result == false
    }

    def "should create default consumers for jms destination CmDataChangeDiveretedQueue if value of jvm parameter for number consumers is empty"() {
        given: "ensure required mocks are defined and jvm parameter for number of consumers are configured as empty string"
            jmsQueueConnector.sessions.add(session)
            jmsQueueConnector.context = context
            jmsQueueConnector.connection = connection
            connection.createSession(false, Session.CLIENT_ACKNOWLEDGE) >> session
            context.lookup(_ as String) >> destination
            System.setProperty("sdk.eventbus.jms.concurrent.queue.listeners.number.CmDataChangeDivertedQueue", "")
            jmsQueueConnector.logger = Mock(Logger)

        when: "createQueueConsumers is invoked"
            List<MessageConsumer> consumersCreated = jmsQueueConnector.createQueueConsumers(_ as String)

        then: "corresponding information is logged and created default number of consumers (1) are created"
            1 * jmsQueueConnector.logger.info("Configuration parameter [{}] has null value. Will ignore this and use the default value [{}]",
                "sdk.eventbus.jms.concurrent.queue.listeners.number.CmDataChangeDivertedQueue", 1)
            1 * session.createConsumer(_ as Destination)
            consumersCreated.size() == 1
    }

    def "should log error and create default consumers for jms destination CmDataChangeDiveretedQueue if value of jvm parameter is a non integer string"() {
        given: "ensure required mocks are defined and jvm parameter for number of consumers for CmDataChangeDiveretedQueue is set as non integer string"
            jmsQueueConnector.sessions.add(session)
            jmsQueueConnector.context = context
            jmsQueueConnector.connection = connection
            connection.createSession(false, Session.CLIENT_ACKNOWLEDGE) >> session
            context.lookup(_ as String) >> destination
            System.setProperty("sdk.eventbus.jms.concurrent.queue.listeners.number.CmDataChangeDivertedQueue", "anotherString")
            jmsQueueConnector.logger = Mock(Logger)

        when: "createQueueConsumers is invoked"
            List<MessageConsumer> consumersCreated = jmsQueueConnector.createQueueConsumers(_ as String)

        then: "corresponding error is logged and default number of consumers (1) is created"
            1 * jmsQueueConnector.logger.error("Was not able to find configuration value for [{}] due to {}. Will use the default value [{}]",
                "sdk.eventbus.jms.concurrent.queue.listeners.number.CmDataChangeDivertedQueue", _ as NumberFormatException, 1)
            1 * session.createConsumer(_ as Destination)
            consumersCreated.size() == 1
    }

    def "should create default number of consumers if jvm parameter for number of consumers for CmDataChangeDivertedQueue is not defined"() {
        given: "ensure required mocks are defined and jvm parameter is not set"
            jmsQueueConnector.sessions.add(session)
            jmsQueueConnector.context = context
            jmsQueueConnector.connection = connection
            connection.createSession(false, Session.CLIENT_ACKNOWLEDGE) >> session
            context.lookup(_ as String) >> destination

        when: "createQueueConsumers is invoked"
            List<MessageConsumer> consumersCreated = jmsQueueConnector.createQueueConsumers(_ as String)

        then: "default number of consumers (1) is created"
            1 * session.createConsumer(_ as Destination)
            consumersCreated.size() == 1
    }

    def "should create consumers for jms destination CmDataChangeDivertedQueue as per defined jvm parameter"() {
        given: "ensure required mocks are defined and jvm parameter is set to 10"
            jmsQueueConnector.sessions.add(session)
            jmsQueueConnector.context = context
            jmsQueueConnector.connection = connection
            connection.createSession(false, Session.CLIENT_ACKNOWLEDGE) >> session
            context.lookup(_ as String) >> destination
            System.setProperty("sdk.eventbus.jms.concurrent.queue.listeners.number.CmDataChangeDivertedQueue", "10")

        when: "createQueueConsumers is invoked"
            List<MessageConsumer> consumersCreated = jmsQueueConnector.createQueueConsumers(_ as String)

        then: "10 consumers are created for CmDataChangeDivertedQueue"
            10 * session.createConsumer(_ as Destination)
            consumersCreated.size() == 10
    }

}