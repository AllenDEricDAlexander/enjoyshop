package top.egon.enjoyshop.goods.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.enjoyshop.goods.domain.sync.CdcChangeStrategy;
import top.egon.enjoyshop.goods.infrastructure.sync.AdCdcChangeStrategy;
import top.egon.enjoyshop.goods.infrastructure.sync.ProductCdcChangeStrategy;

/** Separate Rabbit transport configuration for the PostgreSQL CDC bridge and semantic queues. */
@Slf4j
@Configuration(value = "cdcRabbitConfiguration", proxyBeanMethods = false)
@EnableRabbit
@EnableConfigurationProperties(CdcSourceProperties.class)
public class CdcRabbitConfiguration {

    private static final String CDC_ENABLED = "app.cdc.enabled";
    private static final String RAW_EXCHANGE = "enjoyshop.cdc.raw.v1";
    private static final String SYNC_EXCHANGE = "enjoyshop.sync.v1";
    private static final String DEAD_LETTER_EXCHANGE = "enjoyshop.cdc.dead.v1";
    private static final String UNROUTED_EXCHANGE = "enjoyshop.cdc.unrouted.v1";
    private static final String RAW_QUEUE = "enjoyshop.cdc.normalize.v1";
    private static final String AD_QUEUE = "enjoyshop.ad.invalidate.v1";
    private static final String SEARCH_QUEUE = "enjoyshop.search.refresh.v1";
    private static final String RAW_DEAD_QUEUE = "enjoyshop.cdc.normalize.dead.v1";
    private static final String AD_DEAD_QUEUE = "enjoyshop.ad.invalidate.dead.v1";
    private static final String SEARCH_DEAD_QUEUE = "enjoyshop.search.refresh.dead.v1";
    private static final String UNROUTED_QUEUE = "enjoyshop.cdc.unrouted.dead.v1";

    @Bean("cdcUnroutedExchange")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public TopicExchange cdcUnroutedExchange() {
        return new TopicExchange(UNROUTED_EXCHANGE, true, false);
    }

    @Bean("cdcRawExchange")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public TopicExchange cdcRawExchange() {
        return new TopicExchange(RAW_EXCHANGE, true, false,
                Map.of("alternate-exchange", UNROUTED_EXCHANGE));
    }

    @Bean("cdcSyncExchange")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public TopicExchange cdcSyncExchange() {
        return new TopicExchange(SYNC_EXCHANGE, true, false);
    }

    @Bean("cdcDeadLetterExchange")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public DirectExchange cdcDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean("cdcRawQueue")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public Queue cdcRawQueue() {
        return QueueBuilder.durable(RAW_QUEUE).quorum().singleActiveConsumer()
                .deadLetterExchange(DEAD_LETTER_EXCHANGE).deadLetterRoutingKey(RAW_DEAD_QUEUE).build();
    }

    @Bean("cdcAdQueue")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public Queue cdcAdQueue() {
        return QueueBuilder.durable(AD_QUEUE).quorum()
                .deadLetterExchange(DEAD_LETTER_EXCHANGE).deadLetterRoutingKey(AD_DEAD_QUEUE).build();
    }

    @Bean("cdcSearchQueue")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public Queue cdcSearchQueue() {
        return QueueBuilder.durable(SEARCH_QUEUE).quorum()
                .deadLetterExchange(DEAD_LETTER_EXCHANGE).deadLetterRoutingKey(SEARCH_DEAD_QUEUE).build();
    }

    @Bean("cdcRawDeadLetterQueue")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public Queue cdcRawDeadLetterQueue() {
        return quorumQueue(RAW_DEAD_QUEUE);
    }

    @Bean("cdcAdDeadLetterQueue")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public Queue cdcAdDeadLetterQueue() {
        return quorumQueue(AD_DEAD_QUEUE);
    }

    @Bean("cdcSearchDeadLetterQueue")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public Queue cdcSearchDeadLetterQueue() {
        return quorumQueue(SEARCH_DEAD_QUEUE);
    }

    @Bean("cdcUnroutedQueue")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public Queue cdcUnroutedQueue() {
        return quorumQueue(UNROUTED_QUEUE);
    }

    @Bean("cdcRawBinding")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public Binding cdcRawBinding(@Qualifier("cdcRawQueue") Queue queue,
            @Qualifier("cdcRawExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("master-data");
    }

    @Bean("cdcAdBinding")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public Binding cdcAdBinding(@Qualifier("cdcAdQueue") Queue queue,
            @Qualifier("cdcSyncExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("ad.invalidate.v1");
    }

    @Bean("cdcSearchBinding")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public Binding cdcSearchBinding(@Qualifier("cdcSearchQueue") Queue queue,
            @Qualifier("cdcSyncExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("product.refresh.v1");
    }

    @Bean("cdcRawDeadLetterBinding")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public Binding cdcRawDeadLetterBinding(@Qualifier("cdcRawDeadLetterQueue") Queue queue,
            @Qualifier("cdcDeadLetterExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RAW_DEAD_QUEUE);
    }

    @Bean("cdcAdDeadLetterBinding")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public Binding cdcAdDeadLetterBinding(@Qualifier("cdcAdDeadLetterQueue") Queue queue,
            @Qualifier("cdcDeadLetterExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(AD_DEAD_QUEUE);
    }

    @Bean("cdcSearchDeadLetterBinding")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public Binding cdcSearchDeadLetterBinding(@Qualifier("cdcSearchDeadLetterQueue") Queue queue,
            @Qualifier("cdcDeadLetterExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(SEARCH_DEAD_QUEUE);
    }

    @Bean("cdcUnroutedBinding")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public Binding cdcUnroutedBinding(@Qualifier("cdcUnroutedQueue") Queue queue,
            @Qualifier("cdcUnroutedExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("#");
    }

    @Bean("cdcRabbitTemplate")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public RabbitTemplate cdcRabbitTemplate(
            @Qualifier("rabbitConnectionFactory") ConnectionFactory connectionFactory,
            @Qualifier("spring.rabbitmq-org.springframework.boot.autoconfigure.amqp.RabbitProperties")
            RabbitProperties rabbitProperties) {
        if (rabbitProperties.getPublisherConfirmType() != ConfirmType.CORRELATED
                || !rabbitProperties.isPublisherReturns()) {
            throw new IllegalStateException("CDC requires correlated publisher confirms and returns");
        }
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        template.setReturnsCallback(returned -> log.warn(
                "CDC message returned exchange={} routingKey={} replyCode={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode()));
        return template;
    }

    @Bean("cdcRabbitListenerContainerFactory")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public SimpleRabbitListenerContainerFactory cdcRabbitListenerContainerFactory(
            @Qualifier("rabbitConnectionFactory") ConnectionFactory connectionFactory,
            @Qualifier("simpleRabbitListenerContainerFactoryConfigurer")
            SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(32);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1_000, 2.0, 10_000)
                .recoverer(cdcExhaustedRecoverer())
                .build());
        return factory;
    }

    @Bean("cdcChangeStrategies")
    @ConditionalOnProperty(name = CDC_ENABLED, havingValue = "true")
    public Map<String, CdcChangeStrategy> cdcChangeStrategies(
            @Qualifier("app.cdc-top.egon.enjoyshop.goods.infrastructure.config.CdcSourceProperties")
            CdcSourceProperties properties,
            @Qualifier("adCdcChangeStrategy") AdCdcChangeStrategy adStrategy,
            @Qualifier("productCdcChangeStrategy") ProductCdcChangeStrategy productStrategy) {
        LinkedHashMap<String, CdcChangeStrategy> registry = new LinkedHashMap<>();
        properties.getSourceRegistry().forEach((alias, source) -> {
            source.getTables().forEach(table -> {
                CdcChangeStrategy strategy = switch (table) {
                    case "ad" -> adStrategy;
                    case "spu", "sku" -> productStrategy;
                    default -> throw new IllegalStateException("CDC source table has no strategy: " + table);
                };
                if (!strategy.supports(alias, source.getSchema(), table)
                        || registry.putIfAbsent(table, strategy) != null) {
                    throw new IllegalStateException("CDC source registry has an invalid strategy route: " + table);
                }
            });
        });
        if (!registry.keySet().equals(java.util.Set.of("ad", "spu", "sku"))) {
            throw new IllegalStateException("CDC source registry must map ad, spu, and sku exactly once");
        }
        return Map.copyOf(registry);
    }

    private static Queue quorumQueue(String name) {
        return QueueBuilder.durable(name).quorum().build();
    }

    private static MessageRecoverer cdcExhaustedRecoverer() {
        return (message, cause) -> {
            log.warn("CDC transient delivery exhausted; rejecting to configured DLX messageId={}",
                    message.getMessageProperties().getMessageId());
            throw new AmqpRejectAndDontRequeueException("CDC transient delivery exhausted", true, cause);
        };
    }
}
