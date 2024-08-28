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

/**
 * User defined exception, can be thrown in case of exception with JMS connections/sessions.
 */
public class JmsAdapterException extends RuntimeException {

    private static final long serialVersionUID = -5934930026538299168L;

    /**
     * Instantiates a new jms adapter exception with specific message and {@link Throwable}.
     *
     * @param message
     *     the exception message
     * @param exception
     *     the Throwable exception
     */
    public JmsAdapterException(final String message, final Throwable exception) {
        super(message, exception);
    }

}
