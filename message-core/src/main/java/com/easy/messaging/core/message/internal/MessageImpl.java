package com.easy.messaging.core.message.internal;

import com.easy.messaging.core.message.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MessageImpl implements Message {

    private String payload;
    private Map<String, String> headers;

    public MessageImpl() {
    }

    public MessageImpl(String payload, Map<String, String> headers) {
        this.payload = payload;
        this.headers = headers;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Message {");
        sb.append("payload='").append(payload != null ? payload : "null").append("', ");
        sb.append("headers=");
        if (headers == null) {
            sb.append("null");
        } else {
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            sb.append("}");
        }
        sb.append("}");
        return sb.toString();
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public Optional<String> getHeader(String name) {
        return Optional.ofNullable(headers.get(name));
    }

    @Override
    public String getRequiredHeader(String name) {
        String s = headers.get(name);
        if (s == null)
            throw new RuntimeException("No such header: " + name + " in this message " + this);
        else
            return s;
    }

    @Override
    public boolean hasHeader(String name) {
        return headers.containsKey(name);
    }

    @Override
    public String getId() {
        return getRequiredHeader(Message.ID);
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }


    @Override
    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    public void setHeader(String name, String value) {
        if (headers == null)
            headers = new HashMap<>();
        headers.put(name, value);
    }

    @Override
    public void removeHeader(String key) {
        headers.remove(key);
    }
}
