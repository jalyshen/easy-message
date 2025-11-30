package com.easy.messaging.core.scanning;

import com.easy.messaging.core.config.EasyMessagingProperties;
import com.easy.messaging.core.consumer.MessageConsumer;
import com.easy.messaging.core.listener.MessageListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ListenerScanner implements ApplicationContextAware, InitializingBean {

    private static final Logger logger = Logger.getLogger(ListenerScanner.class.getName());

    // here will inject the real implementation
    private final MessageConsumer consumer;

    // get the destinations
    private final EasyMessagingProperties props;

    private ApplicationContext applicationContext;

    public ListenerScanner(MessageConsumer consumer, EasyMessagingProperties props) {
        this.consumer = consumer;
        this.props = props;
    }

    @Override
    public void afterPropertiesSet() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);

        // scan all beans
        for (Object bean : beans.values()) {
            for (Method method : bean.getClass().getMethods()) {
                MessageListener ann = method.getAnnotation(MessageListener.class);
                if (ann == null) continue;

                String logicalDest = ann.destination();
                String realTopic = props.resolveDestination(logicalDest);

                String subscriberId = ann.subscriberId();
                if (subscriberId.isEmpty()) {
                    subscriberId = bean.getClass().getSimpleName() + "_" + method.getName();
                }

                // register consumers
                consumer.subscribe(subscriberId, Set.of(realTopic), msg -> {
                    try {
                        method.invoke(bean, msg);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "register consumer failed", e);
                    }
                });
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
