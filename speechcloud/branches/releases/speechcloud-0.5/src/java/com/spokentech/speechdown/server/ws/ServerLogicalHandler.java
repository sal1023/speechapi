/*
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.spokentech.speechdown.server.ws;


import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.LogicalMessage;

public class ServerLogicalHandler implements LogicalHandler<LogicalMessageContext>
{

    public boolean handleMessage(LogicalMessageContext context)
    {
        boolean direction= ((Boolean)context.get(LogicalMessageContext.MESSAGE_OUTBOUND_PROPERTY)).booleanValue();
        if (direction) {
            System.out.println("direction = outbound");
        } else {
            System.out.println("direction = inbound");
        }
        displayHeaders(context);
        return true;
    }

    public void close(MessageContext context)
    {
    }

    public boolean handleFault(LogicalMessageContext context)
    {
        return true;
    }

    public void displayHeaders(MessageContext context) {
       System.out.println("HTTP Headers =|"+ context.get(MessageContext.HTTP_REQUEST_HEADERS)+"|");
       return;
    }
}
