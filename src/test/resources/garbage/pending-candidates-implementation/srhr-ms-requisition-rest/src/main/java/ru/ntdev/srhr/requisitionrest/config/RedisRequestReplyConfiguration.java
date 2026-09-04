package ru.ntdev.srhr.requisitionrest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import ru.ntdev.srhr.requisitionrest.adapter.redis.PendingCandidatesRedisSubscriber;

@Configuration
public class RedisRequestReplyConfiguration {
    @Bean
    RedisMessageListenerContainer pendingCandidatesRedisListenerContainer(
            RedisConnectionFactory connectionFactory,
            PendingCandidatesRedisSubscriber subscriber,
            PendingCandidatesRequestReplyProperties properties) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(properties.notificationChannel()));
        return container;
    }
}
