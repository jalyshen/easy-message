package com.jaly.messaging.core.consumer;

import com.jaly.messaging.core.common.Message;

import java.util.function.Consumer;

public interface MessageHandler extends Consumer<Message> {
}
