package com.jaly.messaging.core.producer;

import com.jaly.messaging.core.common.Message;

public interface MessageProducer {

    /**
     * Send a message
     * @param destination the destination channel
     * @param message the message to doSend
     * @see Message
     */
    void send(String destination, Message message);
}
