package com.easy.messaging.core.producer;

import com.easy.messaging.core.message.Message;

public interface MessageProducer {

    /**
     * Send a message
     * @param destination the destination channel
     * @param message the message to doSend
     * @see Message
     */
    void send(String destination, Message message);
}
