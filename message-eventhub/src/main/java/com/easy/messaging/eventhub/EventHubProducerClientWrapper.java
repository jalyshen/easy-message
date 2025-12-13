package com.easy.messaging.eventhub;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.easy.messaging.core.message.Message;
import com.easy.messaging.core.producer.MessageProducer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * 针对单个EventHub实例的发送包装器
 */
public class EventHubProducerClientWrapper implements MessageProducer {

    private final EventHubProducerClient client;
    public EventHubProducerClientWrapper(EventHubProducerClient client) {
        this.client = client;
    }
    @Override
    public void send(String destination, Message message) {
        // 将 Core Message 转换为 Azure EventData
        byte[] body = message.getPayload() != null ? message.getPayload().getBytes(StandardCharsets.UTF_8) : new byte[0];
        EventData eventData = new EventData(body);

        // 设置 Headers
        if (message.getHeaders() != null) {
            message.getHeaders().forEach((k, v) -> eventData.getProperties().put(k, v));
        }

        // 处理 PartitionKey (如果 Header 里有)
        // 这里的逻辑可以根据你的 Message 常量 PARTITION_ID 来定
        // SendOptions options = new SendOptions();
        // options.setPartitionKey(...)

        // 发送 (同步发送，Azure SDK 实际上是异步的但这里我们 block 等待结果或直接 fire-and-forget)
        // 这里简化为发送单个事件。生产环境通常使用 createBatch。
        client.send(Collections.singletonList(eventData));
    }
}
