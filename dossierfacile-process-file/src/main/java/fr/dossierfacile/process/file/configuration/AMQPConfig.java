package fr.dossierfacile.process.file.configuration;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;

@Configuration
public class AMQPConfig {
    @Value("${rabbitmq.exchange.file.process}")
    private String exchangeName;

    @Value("${rabbitmq.queue.file.process.tax}")
    private String queueName;

    @Value("${rabbitmq.queue.file.minify}")
    private String minifyQueueName;

    @Value("${rabbitmq.routing.key.file.process.tax}")
    private String routingKey;

    @Value("${rabbitmq.routing.key.file.minify}")
    private String minifyRoutingKey;

    @Value("${rabbitmq.prefetch}")
    private Integer prefetch;

    @Bean
    TopicExchange exchangeFileProcess() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    Queue queueFileProcessTax() {
        return new Queue(queueName, true);
    }

    @Bean
    Queue queueFileMinify() {
        return new Queue(minifyQueueName, true);
    }

    @Bean
    Binding bindingQueueProcessFilesOcrExchangeFileProcess(Queue queueFileProcessTax, TopicExchange exchangeFileProcess) {
        return BindingBuilder.bind(queueFileProcessTax).to(exchangeFileProcess).with(routingKey);
    }

    @Bean
    Binding bindingQueueProcessMinifyFileProcess(Queue queueFileMinify, TopicExchange exchangeFileProcess) {
        return BindingBuilder.bind(queueFileMinify).to(exchangeFileProcess).with(minifyRoutingKey);
    }

    // next step: use DLQ for unblocking retry instead of this blocking way - 3 retry - x5
    @Bean
    public SimpleRabbitListenerContainerFactory retryContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());

        Advice[] adviceChain = { RetryInterceptorBuilder.stateless()
                .backOffOptions(1000, 5.0, 15000)
                .maxAttempts(3)
                .recoverer( (r,t) -> new RejectAndDontRequeueRecoverer())
                .build() };
        factory.setAdviceChain(adviceChain);
        factory.setPrefetchCount(prefetch);
        return factory;
    }
}
