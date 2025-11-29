package com.jaly.messaging.eventhub;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.jaly.messaging.core.common.Message;
import com.jaly.messaging.core.common.MessageImpl;
import com.jaly.messaging.core.consumer.MessageConsumer;
import com.jaly.messaging.core.consumer.MessageHandler;
import com.jaly.messaging.core.consumer.MessageSubscription;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EventHubMessageConsumer implements MessageConsumer {

    private final EventProcessorClient processorClient;

    public EventHubMessageConsumer(EventProcessorClient processorClient) {
        this.processorClient = processorClient;
        this.processorClient.start();
    }


    @Override
    public MessageSubscription subscribe(String subscriberId, Set<String> channels, MessageHandler handler) {
        //TODO:
        return null;
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public void close() {
        //TODO:
    }

    public static EventProcessorClient buildProcessor(String connectionString,
                                                      String eventHubName,
                                                      String consumerGroup,
                                                      BlobCheckpointStore checkpointStore,
                                                      MessageHandler handler) {
        return new EventProcessorClientBuilder()
                .connectionString(connectionString, eventHubName)
                .consumerGroup(consumerGroup)
                .processEvent(partitionEvent -> {
                    EventData eventData = partitionEvent.getEventData();
                    if (eventData != null) {
                        Message msg = toMessage(eventData);
                        try {
                            handler.handle(msg);
                            partitionEvent.updateCheckpoint();
                        } catch (Exception ex) {
                            ex.printStackTrace(); //TODO: update here
                        }
                    }
                })
                .processError(errorContext -> {
                    // 记录错误
                    errorContext.getThrowable().printStackTrace();
                })
                .checkpointStore(checkpointStore) // Must be：BlobCheckpointStore
                .buildEventProcessorClient();
    }

    private static Message toMessage(EventData eventData) {
        Map<String, String> headers = new HashMap<>();
        eventData.getProperties().forEach((k, v) -> headers.put(k, String.valueOf(v)));
        String payload = eventData.getBodyAsString();
        return new MessageImpl(payload, headers);
    }
}
