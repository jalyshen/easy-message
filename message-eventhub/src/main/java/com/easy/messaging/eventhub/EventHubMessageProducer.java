package com.easy.messaging.eventhub;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import com.easy.messaging.core.message.Message;
import com.easy.messaging.core.producer.MessageProducer;

public class EventHubMessageProducer implements MessageProducer {

    private final EventHubProducerAsyncClient client;

    public EventHubMessageProducer(EventHubProducerAsyncClient client) {
        this.client = client;
    }

    @Override
    public void send(String destination, Message message) {
        EventData event = new EventData(message.getPayload());
        message.getHeaders().forEach(event.getProperties()::put);
        client.createBatch().flatMap(batch -> {
            batch.tryAdd(event);
            return client.send(batch);
        }).subscribe();
    }
}
