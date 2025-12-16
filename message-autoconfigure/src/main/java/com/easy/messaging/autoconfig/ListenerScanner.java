package com.easy.messaging.autoconfig;

import com.easy.messaging.core.listener.MessageListener;
import com.easy.messaging.eventhub.EventHubListenerContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * 扫描 Bean，解析 @MessageListener，并注册到 Container 中。
 * 修改为实现 SmartInitializingSingleton，确保在所有 Bean 初始化完成后再进行扫描，
 * 避免出现循环依赖错误。
 */
public class ListenerScanner implements ApplicationContextAware, SmartInitializingSingleton {

    private static final Logger logger = LoggerFactory.getLogger(ListenerScanner.class);

    private final EventHubListenerContainer container;

    private final MessagingProperties props;

    private ApplicationContext applicationContext;

    public ListenerScanner(EventHubListenerContainer container, MessagingProperties props) {
        this.container = container;
        this.props = props;
    }

    /**
     * 该方法会在所有单例 Bean 初始化完成后回调
     */
    @Override
    public void afterSingletonsInstantiated() {
        // 使用 getBeanDefinitionNames 配合 getBean 是为了确保扫描所有已经加载的 Bean
        // 此时所有 Bean 已经 Ready，不会引发循环依赖
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            // 这里可能会有某些 FactoryBean 还没初始化，加个 try-catch 比较稳妥
            try {
                Object bean = applicationContext.getBean(beanName);
                Class<?> targetClass = AopUtils.getTargetClass(bean);

                // 扫描类中的方法
                for (Method method : targetClass.getMethods()) {
                    // 使用 AnnotationUtils 以支持代理
                    MessageListener ann = AnnotationUtils.findAnnotation(method, MessageListener.class);
                    if (ann == null) continue;

                    String logicalDest = ann.destination();
                    // 1. 解析 Topic
                    String realTopic = props.resolveDestination(logicalDest);

                    // 2. 找到该 Topic 对应的 Connection String 配置
                    MessagingProperties.ConsumerInfo consumerInfo = props.findConsumerInfoByTopic(realTopic);

                    if (consumerInfo == null) {
                        logger.warn("Found @MessageListener for destination '{}' (mapped to '{}') but no Consumer configuration found for this EventHub.", logicalDest, realTopic);
                        continue;
                    }

                    String subscriberId = ann.subscriberId();
                    if (subscriberId.isEmpty()) {
                        subscriberId = props.getDefaults().getConsumer() != null ?
                                "easy-consumer-" + beanName : "default-consumer";
                    }

                    // 3. 注册到容器
                    try {
                        container.registerListener(
                                consumerInfo.getConnectionString(),
                                consumerInfo.getEventHubName(),
                                consumerInfo.getConsumerGroup(),
                                msg -> {
                                    try {
                                        method.invoke(bean, msg);
                                    } catch (Exception e) {
                                        throw new RuntimeException("Invocation failed", e);
                                    }
                                }
                        );
                        logger.info("Registered listener for destination [{}] -> topic [{}] on bean [{}]", logicalDest, realTopic, beanName);
                    } catch (Exception e) {
                        logger.error("Failed to register listener for bean {}", beanName, e);
                    }
                }
            } catch (Exception e) {
                // 某些 lazy-init 的 bean 或者特殊的 bean 可能会导致获取失败，忽略即可
                logger.trace("Skipping bean {} during listener scan due to error: {}", beanName, e.getMessage());
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
