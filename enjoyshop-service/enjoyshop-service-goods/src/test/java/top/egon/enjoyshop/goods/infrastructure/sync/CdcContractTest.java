package top.egon.enjoyshop.goods.infrastructure.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.aop.ProxyMethodInvocation;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.enjoyshop.goods.domain.sync.AdCacheInvalidationEvent;
import top.egon.enjoyshop.goods.domain.sync.CdcChangeStrategy;
import top.egon.enjoyshop.goods.domain.sync.CdcOperationEnum;
import top.egon.enjoyshop.goods.domain.sync.CdcRecordBO;
import top.egon.enjoyshop.goods.domain.sync.CdcSourceCursorBO;
import top.egon.enjoyshop.goods.domain.sync.ProductRefreshBO;
import top.egon.enjoyshop.goods.domain.sync.ProductRefreshRequestedEvent;
import top.egon.enjoyshop.goods.infrastructure.config.CdcSourceProperties;
import top.egon.enjoyshop.goods.infrastructure.config.CdcRabbitConfiguration;
import top.egon.enjoyshop.goods.infrastructure.sync.AdInvalidationEventConverterImpl;
import top.egon.enjoyshop.goods.infrastructure.ad.repo.AdRepository;
import top.egon.enjoyshop.goods.adapter.sync.AdCacheInvalidationConsumer;
import top.egon.enjoyshop.goods.adapter.sync.CdcRawConsumer;
import top.egon.enjoyshop.goods.infrastructure.sync.ProductRefreshEventConverterImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Contracts for committed-row normalization and the future current-state refresh protocol. */
class CdcContractTest {

    private static final long TENANT_ID = 41L;
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-10T01:00:00Z");

    @AfterEach
    void clearThreadContext() {
        MDC.clear();
    }

    @Test
    void partialConfirmDoesNotAck() throws IOException {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ObjectMapper mapper = objectMapper();
            RabbitTemplate template = mock(RabbitTemplate.class);
            AtomicInteger sends = new AtomicInteger();
            CdcSourceProperties properties = sourceProperties().setConfirmTimeout(Duration.ofMillis(10));
            doAnswer(invocation -> {
                Message sent = invocation.getArgument(2);
                CorrelationData correlation = invocation.getArgument(3);
                int send = sends.getAndIncrement();
                int batch = send / 2;
                if (send % 2 == 0) {
                    correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
                } else if (batch == 0) {
                    correlation.setReturned(new ReturnedMessage(sent, 312, "NO_ROUTE", "enjoyshop.sync.v1",
                            "ad.invalidate.v1"));
                    correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
                } else if (batch == 1) {
                    correlation.getFuture().complete(new CorrelationData.Confirm(false, "nack"));
                }
                return null;
            }).when(template).send(eq("enjoyshop.sync.v1"), eq("ad.invalidate.v1"), any(Message.class),
                    any(CorrelationData.class));
            CdcEventPublisher publisher = publisher(template, mapper, properties, factory);
            CdcRawConsumer consumer = rawConsumer(mapper, properties, factory,
                    Map.of("ad", adStrategy(new ValidationUtils(factory.getValidator()))), publisher);
            Channel returned = mock(Channel.class);
            Channel nacked = mock(Channel.class);
            Channel timedOut = mock(Channel.class);

            assertThatThrownBy(() -> consumer.onMessage(rawMessage(mapper, "enjoyshop_cdc_master_e1",
                    41L, 42L, 88L), returned)).isInstanceOf(AmqpException.class);
            assertThatThrownBy(() -> consumer.onMessage(rawMessage(mapper, "enjoyshop_cdc_master_e1",
                    41L, 42L, 89L), nacked)).isInstanceOf(AmqpException.class);
            assertThatThrownBy(() -> consumer.onMessage(rawMessage(mapper, "enjoyshop_cdc_master_e1",
                    41L, 42L, 90L), timedOut)).isInstanceOf(AmqpException.class);

            verify(returned, never()).basicAck(88L, false);
            verify(nacked, never()).basicAck(89L, false);
            verify(timedOut, never()).basicAck(90L, false);
            verify(returned, never()).basicReject(88L, false);
            verify(nacked, never()).basicReject(89L, false);
            verify(timedOut, never()).basicReject(90L, false);
            verify(template, times(6)).send(eq("enjoyshop.sync.v1"), eq("ad.invalidate.v1"), any(Message.class),
                    any(CorrelationData.class));
        }
    }

    @Test
    void redeliveryStableIdentity() throws IOException {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ObjectMapper mapper = objectMapper();
            RabbitTemplate template = mock(RabbitTemplate.class);
            List<String> messageIds = new ArrayList<>();
            List<String> correlationIds = new ArrayList<>();
            doAnswer(invocation -> {
                Message sent = invocation.getArgument(2);
                CorrelationData correlation = invocation.getArgument(3);
                messageIds.add(sent.getMessageProperties().getMessageId());
                correlationIds.add(correlation.getId());
                correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
                return null;
            }).when(template).send(eq("enjoyshop.sync.v1"), eq("ad.invalidate.v1"), any(Message.class),
                    any(CorrelationData.class));
            CdcSourceProperties properties = sourceProperties();
            CdcEventPublisher publisher = publisher(template, mapper, properties, factory);
            CdcRawConsumer consumer = rawConsumer(mapper, properties, factory,
                    Map.of("ad", adStrategy(new ValidationUtils(factory.getValidator()))), publisher);
            Message firstDelivery = rawMessage(mapper, "enjoyshop_cdc_master_e1", 41L, 41L, 89L);
            Channel lostAck = mock(Channel.class);
            doThrow(new IOException("ack outcome unknown")).when(lostAck).basicAck(89L, false);

            assertThatThrownBy(() -> consumer.onMessage(firstDelivery, lostAck)).isInstanceOf(IOException.class);
            Channel redeliveryChannel = mock(Channel.class);
            consumer.onMessage(rawMessage(mapper, "enjoyshop_cdc_master_e1", 41L, 41L, 90L, true),
                    redeliveryChannel);

            verify(lostAck).basicAck(89L, false);
            verify(redeliveryChannel).basicAck(90L, false);
            assertThat(messageIds).hasSize(2).allMatch(messageIds.getFirst()::equals);
            assertThat(correlationIds).hasSize(2).doesNotHaveDuplicates();
        }
    }

    @Test
    void invalidSourceRejects() throws IOException {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ObjectMapper mapper = objectMapper();
            RabbitTemplate template = mock(RabbitTemplate.class);
            CdcSourceProperties properties = sourceProperties();
            CdcRawConsumer consumer = rawConsumer(mapper, properties, factory,
                    Map.of("ad", adStrategy(new ValidationUtils(factory.getValidator()))),
                    publisher(template, mapper, properties, factory));
            Channel channel = mock(Channel.class);

            consumer.onMessage(rawMessage(mapper, "unregistered-source", 41L, 42L, 90L), channel);

            verify(channel).basicReject(90L, false);
            verify(channel, never()).basicAck(90L, false);
            verifyNoInteractions(template);
        }
    }

    @Test
    void transactionControlsRequireKnownShapeAndFixedSourceRoute() throws IOException {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ObjectMapper mapper = objectMapper();
            CdcSourceProperties properties = sourceProperties();
            RabbitTemplate template = mock(RabbitTemplate.class);
            CdcRawConsumer consumer = rawConsumer(mapper, properties, factory, Map.of(),
                    publisher(template, mapper, properties, factory));

            Channel begin = mock(Channel.class);
            consumer.onMessage(controlMessage(mapper, "BEGIN", 101L, "master-data"), begin);
            verify(begin).basicAck(101L, false);

            Channel end = mock(Channel.class);
            consumer.onMessage(controlMessage(mapper, "END", 102L, "master-data"), end);
            verify(end).basicAck(102L, false);

            Channel heartbeat = mock(Channel.class);
            consumer.onMessage(controlMessage(mapper, "HEARTBEAT", 103L, "master-data"), heartbeat);
            verify(heartbeat).basicReject(103L, false);

            Channel untrustedRoute = mock(Channel.class);
            consumer.onMessage(controlMessage(mapper, "BEGIN", 104L, "other-source"), untrustedRoute);
            verify(untrustedRoute).basicReject(104L, false);
            verifyNoInteractions(template);
        }
    }

    @Test
    void rabbitTopologyUsesQuorumRoutesAndKeepsSearchConsumerAbsent() {
        CdcRabbitConfiguration configuration = new CdcRabbitConfiguration();
        var rawExchange = configuration.cdcRawExchange();
        var rawQueue = configuration.cdcRawQueue();
        var adQueue = configuration.cdcAdQueue();
        var searchQueue = configuration.cdcSearchQueue();

        assertThat(rawExchange.getArguments()).containsEntry("alternate-exchange", "enjoyshop.cdc.unrouted.v1");
        assertThat(rawQueue.isDurable()).isTrue();
        assertThat(rawQueue.getArguments()).containsEntry("x-queue-type", "quorum")
                .containsEntry("x-single-active-consumer", true);
        assertThat(adQueue.getArguments()).containsEntry("x-queue-type", "quorum");
        assertThat(searchQueue.getArguments()).containsEntry("x-queue-type", "quorum");
        assertThat(configuration.cdcRawBinding(rawQueue, rawExchange).getRoutingKey()).isEqualTo("master-data");
        assertThat(configuration.cdcAdBinding(adQueue, configuration.cdcSyncExchange()).getRoutingKey())
                .isEqualTo("ad.invalidate.v1");
        assertThat(configuration.cdcSearchBinding(searchQueue, configuration.cdcSyncExchange()).getRoutingKey())
                .isEqualTo("product.refresh.v1");
        assertThat(ProductRefreshRequestedEvent.class.getAnnotation(org.springframework.amqp.rabbit.annotation.RabbitListener.class))
                .isNull();

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ValidationUtils validation = new ValidationUtils(factory.getValidator());
            Map<String, CdcChangeStrategy> routes = configuration.cdcChangeStrategies(sourceProperties(),
                    adStrategy(validation), productStrategy(validation));
            assertThat(routes.keySet()).containsExactlyInAnyOrder("ad", "spu", "sku");
            assertThat(routes.get("ad").routingKey()).isEqualTo("ad.invalidate.v1");
            assertThat(routes.get("sku").routingKey()).isEqualTo("product.refresh.v1");
        }
    }

    @Test
    void publisherTemplateRequiresCorrelatedConfirmAndReturns() {
        CdcRabbitConfiguration configuration = new CdcRabbitConfiguration();
        RabbitProperties properties = new RabbitProperties();
        properties.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        properties.setPublisherReturns(true);
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        try {
            RabbitTemplate template = configuration.cdcRabbitTemplate(connectionFactory, properties);
            assertThat(template.isMandatoryFor(new Message(new byte[0], new MessageProperties()))).isTrue();
            properties.setPublisherReturns(false);
            assertThatThrownBy(() -> configuration.cdcRabbitTemplate(connectionFactory, properties))
                    .isInstanceOf(IllegalStateException.class);
        } finally {
            connectionFactory.destroy();
        }
    }

    @Test
    void cdcEnabledBeansWireWithoutConnectingToRabbit() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("cdc-test", Map.ofEntries(
                Map.entry("app.cdc.enabled", "true"),
                Map.entry("app.cdc.max-payload-bytes", "1048576"),
                Map.entry("app.cdc.confirm-timeout", "PT5S"),
                Map.entry("app.cdc.source-registry.master_data.source-name", "enjoyshop_cdc_master_e1"),
                Map.entry("app.cdc.source-registry.master_data.source-epoch", "e1"),
                Map.entry("app.cdc.source-registry.master_data.topic-prefix", "enjoyshop_cdc_master_e1"),
                Map.entry("app.cdc.source-registry.master_data.database", "goods_master"),
                Map.entry("app.cdc.source-registry.master_data.schema", "public"),
                Map.entry("app.cdc.source-registry.master_data.tables[0]", "ad"),
                Map.entry("app.cdc.source-registry.master_data.tables[1]", "spu"),
                Map.entry("app.cdc.source-registry.master_data.tables[2]", "sku"))));
        context.register(CdcRabbitConfiguration.class, CdcRabbitTestConfiguration.class);
        try (context) {
            context.refresh();
            assertThat(context.getBean("cdcRabbitTemplate")).isInstanceOf(RabbitTemplate.class);
            assertThat(context.getBean("cdcRabbitListenerContainerFactory"))
                    .isInstanceOf(SimpleRabbitListenerContainerFactory.class);
            @SuppressWarnings("unchecked")
            Map<String, CdcChangeStrategy> strategies = context.getBean("cdcChangeStrategies", Map.class);
            assertThat(strategies.keySet()).containsExactlyInAnyOrder("ad", "spu", "sku");
        }
    }

    @Test
    void adInvalidationValidatesScopeAndRestoresMdc() throws IOException {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ObjectMapper mapper = objectMapper();
            CdcSourceProperties properties = sourceProperties();
            AdRepository repository = mock(AdRepository.class);
            AdCacheInvalidationConsumer consumer = new AdCacheInvalidationConsumer(mapper, properties, repository,
                    new ValidationUtils(factory.getValidator()));
            AdCacheInvalidationEvent event = adInvalidationEvent("e1");
            MDC.put("traceId", "existing-trace");
            MDC.put("tenantId", "99");
            doAnswer(invocation -> {
                assertThat(MDC.get("tenantId")).isEqualTo("41");
                assertThat(MDC.get("traceId")).isEqualTo("existing-trace");
                return null;
            }).when(repository).evictCandidates(any());
            Channel channel = mock(Channel.class);

            consumer.onMessage(semanticMessage(mapper, event, 91L, "41"), channel);

            verify(repository).evictCandidates(List.of("web_index_lb", "web_index_mobile"));
            verify(channel).basicAck(91L, false);
            assertThat(MDC.get("tenantId")).isEqualTo("99");
            assertThat(MDC.get("traceId")).isEqualTo("existing-trace");

            Channel oldEpoch = mock(Channel.class);
            consumer.onMessage(semanticMessage(mapper, adInvalidationEvent("e0"), 92L, "41"), oldEpoch);
            verify(oldEpoch).basicReject(92L, false);

            Channel spoofedHeader = mock(Channel.class);
            consumer.onMessage(semanticMessage(mapper, event, 93L, "42"), spoofedHeader);
            verify(spoofedHeader).basicReject(93L, false);
            verify(spoofedHeader, never()).basicAck(93L, false);
            verify(repository, times(1)).evictCandidates(any());
        }
    }

    @Test
    void manualRejectAfterFiniteRetries() throws Throwable {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        SimpleRabbitListenerContainerFactoryConfigurer configurer =
                new SimpleRabbitListenerContainerFactoryConfigurer(new RabbitProperties());
        SimpleRabbitListenerContainerFactory factory = new CdcRabbitConfiguration()
                .cdcRabbitListenerContainerFactory(connectionFactory, configurer);
        assertThat(ReflectionTestUtils.getField(factory, "acknowledgeMode"))
                .isEqualTo(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        assertThat(ReflectionTestUtils.getField(factory, "prefetchCount")).isEqualTo(32);
        Object requeueRejected = ReflectionTestUtils.invokeMethod(factory, "getDefaultRequeueRejected");
        assertThat(requeueRejected).isEqualTo(false);
        assertThat(factory.getAdviceChain()).hasSize(1);
        RetryOperationsInterceptor retry = (RetryOperationsInterceptor) factory.getAdviceChain()[0];
        ProxyMethodInvocation invocation = mock(ProxyMethodInvocation.class);
        Message message = new Message(new byte[0], new MessageProperties());
        Channel channel = mock(Channel.class);
        when(invocation.getMethod()).thenReturn(ListenerMethodFixture.class
                .getMethod("onMessage", Channel.class, Message.class));
        when(invocation.getArguments()).thenReturn(new Object[]{channel, message});
        when(invocation.invocableClone()).thenReturn(invocation);
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(ignored -> {
            attempts.incrementAndGet();
            throw new AmqpException("temporary broker delivery failure");
        }).when(invocation).proceed();

        assertThatThrownBy(() -> retry.invoke(invocation))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .satisfies(failure -> assertThat(((AmqpRejectAndDontRequeueException) failure).isRejectManual()).isTrue());
        assertThat(attempts).hasValue(3);
        connectionFactory.destroy();
    }

    @Test
    void normalizesAdOldNew() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ValidationUtils validation = new ValidationUtils(factory.getValidator());
            AdCdcChangeStrategy strategy = adStrategy(validation);
            assertThat(strategy.supports("master_data", "public", "ad")).isTrue();
            assertThat(strategy.supports("master_data", "public", "spu")).isFalse();
            assertThat(strategy.routingKey()).isEqualTo("ad.invalidate.v1");
            CdcRecordBO moved = record("ad", CdcOperationEnum.UPDATE, 9001L)
                    .setBeforeRowId(9001L).setBeforeTenantId(TENANT_ID).setBeforePosition("web_index_mobile")
                    .setAfterRowId(9001L).setAfterTenantId(TENANT_ID).setAfterPosition("web_index_lb");

            List<? extends CdcChangeStrategy.SyncEvent> events = strategy.normalize(moved);

            assertThat(events).hasSize(1);
            AdCacheInvalidationEvent event = (AdCacheInvalidationEvent) events.getFirst();
            assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(event.getPositions()).containsExactly("web_index_lb", "web_index_mobile");
            assertThat(event.getSourceLsn()).isEqualTo("0/016B6A80");
            assertThat(event.getOccurredAt()).isEqualTo(OCCURRED_AT);

            CdcRecordBO tenantMove = record("ad", CdcOperationEnum.UPDATE, 9001L)
                    .setBeforeRowId(9001L).setBeforeTenantId(41L).setBeforePosition("web_index_lb")
                    .setAfterRowId(9001L).setAfterTenantId(42L).setAfterPosition("web_index_lb");
            List<? extends CdcChangeStrategy.SyncEvent> split = strategy.normalize(tenantMove);
            assertThat(split).extracting(CdcChangeStrategy.SyncEvent::getTenantId).containsExactly(41L, 42L);
            assertThat(split).allSatisfy(value -> assertThat(((AdCacheInvalidationEvent) value).getPositions())
                    .containsExactly("web_index_lb"));

            CdcRecordBO deleted = record("ad", CdcOperationEnum.DELETE, 9001L)
                    .setBeforeRowId(9001L).setBeforeTenantId(TENANT_ID).setBeforePosition("web_index_mobile");
            AdCacheInvalidationEvent deletion = (AdCacheInvalidationEvent) strategy.normalize(deleted).getFirst();
            assertThat(deletion.getPositions()).containsExactly("web_index_mobile");

            CdcRecordBO created = record("ad", CdcOperationEnum.CREATE, 9002L)
                    .setAfterRowId(9002L).setAfterTenantId(TENANT_ID).setAfterPosition("web_index_lb");
            assertThat(((AdCacheInvalidationEvent) strategy.normalize(created).getFirst()).getOperation())
                    .isEqualTo(CdcOperationEnum.CREATE);
            CdcRecordBO snapshot = record("ad", CdcOperationEnum.SNAPSHOT, 9003L)
                    .setAfterRowId(9003L).setAfterTenantId(TENANT_ID).setAfterPosition("web_index_lb");
            snapshot.getCursor().setSourceLsn(null).setSourceTxId(null).setSourceRecordOrder(null);
            AdCacheInvalidationEvent snapshotEvent = (AdCacheInvalidationEvent) strategy.normalize(snapshot).getFirst();
            assertThat(snapshotEvent.getOperation()).isEqualTo(CdcOperationEnum.SNAPSHOT);
            assertThat(snapshotEvent.getSourceLsn()).isNull();
        }
    }

    @Test
    void normalizesSkuRoots() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ValidationUtils validation = new ValidationUtils(factory.getValidator());
            ProductCdcChangeStrategy strategy = productStrategy(validation);
            assertThat(strategy.supports("master_data", "public", "spu")).isTrue();
            assertThat(strategy.supports("master_data", "public", "sku")).isTrue();
            assertThat(strategy.supports("master_data", "public", "ad")).isFalse();
            assertThat(strategy.routingKey()).isEqualTo("product.refresh.v1");
            CdcRecordBO movedSku = record("sku", CdcOperationEnum.UPDATE, 7001L)
                    .setBeforeRowId(7001L).setBeforeTenantId(TENANT_ID).setBeforeSpuId(101L)
                    .setAfterRowId(7001L).setAfterTenantId(TENANT_ID).setAfterSpuId(102L);

            List<? extends CdcChangeStrategy.SyncEvent> events = strategy.normalize(movedSku);

            assertThat(events).hasSize(1);
            ProductRefreshRequestedEvent event = (ProductRefreshRequestedEvent) events.getFirst();
            assertThat(event.getSpuIds()).containsExactly(101L, 102L);
            assertThat(event.getOperation()).isEqualTo(CdcOperationEnum.UPDATE);
            assertThat(event.getSourceTable()).isEqualTo("sku");

            CdcRecordBO spu = record("spu", CdcOperationEnum.DELETE, 101L)
                    .setBeforeRowId(101L).setBeforeTenantId(TENANT_ID);
            ProductRefreshRequestedEvent deletedSpu = (ProductRefreshRequestedEvent) strategy.normalize(spu).getFirst();
            assertThat(deletedSpu.getSpuIds()).containsExactly(101L);
            assertThat(deletedSpu.getOperation()).isEqualTo(CdcOperationEnum.DELETE);

            CdcRecordBO tenantMove = record("sku", CdcOperationEnum.UPDATE, 7001L)
                    .setBeforeRowId(7001L).setBeforeTenantId(41L).setBeforeSpuId(101L)
                    .setAfterRowId(7001L).setAfterTenantId(42L).setAfterSpuId(102L);
            assertThat(strategy.normalize(tenantMove))
                    .extracting(CdcChangeStrategy.SyncEvent::getTenantId)
                    .containsExactly(41L, 42L);
        }
    }

    @Test
    void stableEventIdentity() {
        ObjectMapper mapper = objectMapper();
        CdcEventIdentityComponent identity = new CdcEventIdentityComponent(mapper);
        CdcRecordBO source = record("ad", CdcOperationEnum.UPDATE, 9001L)
                .setBeforeRowId(9001L).setBeforeTenantId(41L).setBeforePosition("web_index_lb")
                .setAfterRowId(9001L).setAfterTenantId(41L).setAfterPosition("web_index_mobile");

        String eventId = identity.eventId(source, 41L, "ad.invalidate.v1");

        assertThat(eventId).hasSize(64).matches("[a-f0-9]{64}");
        assertThat(identity.eventId(source, 41L, "ad.invalidate.v1")).isEqualTo(eventId);
        assertThat(identity.eventId(source, 42L, "ad.invalidate.v1")).isNotEqualTo(eventId);
        assertThat(identity.eventId(source, 41L, "product.refresh.v1")).isNotEqualTo(eventId);
        assertThat(identity.eventId(source.setFactFingerprint("b".repeat(64)), 41L, "ad.invalidate.v1"))
                .isNotEqualTo(eventId);
        CdcRecordBO snapshot = record("ad", CdcOperationEnum.SNAPSHOT, 9001L)
                .setAfterRowId(9001L).setAfterTenantId(41L).setAfterPosition("web_index_lb");
        snapshot.getCursor().setSourceLsn(null).setSourceTxId(null).setSourceRecordOrder(null);
        CdcRecordBO zeroLsnSnapshot = record("ad", CdcOperationEnum.SNAPSHOT, 9001L)
                .setAfterRowId(9001L).setAfterTenantId(41L).setAfterPosition("web_index_lb");
        zeroLsnSnapshot.getCursor().setSourceTxId(null).setSourceRecordOrder(null);
        assertThat(identity.eventId(snapshot, 41L, "ad.invalidate.v1"))
                .isNotEqualTo(identity.eventId(zeroLsnSnapshot, 41L, "ad.invalidate.v1"));
        assertThat(CdcSourceCursorBO.builder().sourceLsn(BigInteger.valueOf(23_816_832L)).build()
                .getSourceLsnText()).isEqualTo("0/016B6A80");
        assertThat(CdcSourceCursorBO.builder().sourceLsn(new BigInteger("18446744073709551615")).build()
                .getSourceLsnText()).isEqualTo("FFFFFFFF/FFFFFFFF");
    }

    @Test
    void rejectsPoison() {
        assertThatThrownBy(() -> CdcOperationEnum.fromCode("t"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CdcOperationEnum.fromCode("?"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(CdcOperationEnum.values()).extracting(CdcOperationEnum::getCode)
                .containsExactly("c", "u", "d", "r");

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ValidationUtils validation = new ValidationUtils(factory.getValidator());
            validation.validate(new CdcSourceProperties());
            CdcRecordBO updateWithoutBefore = record("ad", CdcOperationEnum.UPDATE, 9001L)
                    .setAfterRowId(9001L).setAfterTenantId(TENANT_ID).setAfterPosition("web_index_lb");
            assertThatThrownBy(() -> validation.validate(updateWithoutBefore))
                    .isInstanceOf(ConstraintViolationException.class);
            assertThatThrownBy(() -> validation.validate(record("ad", CdcOperationEnum.CREATE, 9001L)
                    .setAfterRowId(9002L).setAfterTenantId(TENANT_ID).setAfterPosition("web_index_lb")))
                    .isInstanceOf(ConstraintViolationException.class);
            assertThatThrownBy(() -> validation.validate(record("spu", CdcOperationEnum.CREATE, 101L)
                    .setAfterRowId(101L).setAfterTenantId(TENANT_ID).setAfterSpuId(102L)))
                    .isInstanceOf(ConstraintViolationException.class);
            assertThatThrownBy(() -> validation.validate(CdcSourceCursorBO.builder()
                    .sourceAlias("master_data").sourceEpoch("e1").schema("public").table("ad")
                    .operation(CdcOperationEnum.CREATE).rowPk(1L)
                    .sourceLsn(new BigInteger("18446744073709551616")).sourceTsMs(0L).build()))
                    .isInstanceOf(ConstraintViolationException.class);
            validation.validate(CdcSourceCursorBO.builder()
                    .sourceAlias("master_data").sourceEpoch("e1").schema("public").table("ad")
                    .operation(CdcOperationEnum.CREATE).rowPk(1L).sourceLsn(BigInteger.ZERO)
                    .sourceTxId("17").sourceTsMs(0L).build());

            CdcSourceProperties properties = new CdcSourceProperties().setEnabled(true).setSourceRegistry(Map.of());
            assertThatThrownBy(() -> validation.validate(properties))
                    .isInstanceOf(ConstraintViolationException.class);
            CdcSourceProperties.SourceRegistration registration = new CdcSourceProperties.SourceRegistration(
                    "enjoyshop_cdc_master_e1", "e1", "enjoyshop_cdc_master_e1", "goods_master", "public",
                    Set.of("ad", "spu", "sku"));
            validation.validate(properties.setSourceRegistry(Map.of("master_data", registration)));
            assertThatThrownBy(() -> productStrategy(validation).normalize(
                    record("teaching", CdcOperationEnum.CREATE, 1L)
                            .setAfterRowId(1L).setAfterTenantId(TENANT_ID)))
                    .isInstanceOf(ConstraintViolationException.class);
            CdcRecordBO unregisteredSource = record("ad", CdcOperationEnum.CREATE, 1L)
                    .setAfterRowId(1L).setAfterTenantId(TENANT_ID).setAfterPosition("web_index_lb");
            unregisteredSource.getCursor().setSourceAlias("other");
            assertThatThrownBy(() -> adStrategy(validation).normalize(unregisteredSource))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void refreshProtocolIsCurrentState() throws Exception {
        ObjectMapper mapper = objectMapper();
        CdcEventIdentityComponent identity = new CdcEventIdentityComponent(mapper);
        CdcRecordBO source = record("sku", CdcOperationEnum.UPDATE, 7001L)
                .setBeforeRowId(7001L).setBeforeTenantId(TENANT_ID).setBeforeSpuId(101L)
                .setAfterRowId(7001L).setAfterTenantId(TENANT_ID).setAfterSpuId(102L);
        List<Long> mutableRoots = new ArrayList<>(List.of(101L, 102L));
        ProductRefreshBO refresh = ProductRefreshBO.builder()
                .eventId(identity.eventId(source, TENANT_ID, "product.refresh.v1"))
                .tenantId(TENANT_ID)
                .cursor(source.getCursor())
                .spuIds(mutableRoots)
                .build();
        mutableRoots.clear();
        ProductRefreshRequestedEvent event = new ProductRefreshEventConverterImpl().toTarget(refresh);
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            new ValidationUtils(factory.getValidator()).validate(event);
        }

        JsonNode json = mapper.readTree(mapper.writeValueAsBytes(event));
        assertThat(json.path("schemaVersion").intValue()).isEqualTo(1);
        assertThat(json.path("tenantId").textValue()).isEqualTo("41");
        assertThat(json.path("spuIds").isArray()).isTrue();
        assertThat(json.path("spuIds").get(0).textValue()).isEqualTo("101");
        assertThat(json.path("spuIds").get(1).textValue()).isEqualTo("102");
        assertThat(json.path("operation").textValue()).isEqualTo("u");
        assertThat(json.path("sourceLsn").textValue()).isEqualTo("0/016B6A80");
        assertThat(json.path("sourceEpoch").textValue()).isEqualTo("e1");
        assertThat(json.path("sourceTxId").textValue()).isEqualTo("17");
        assertThat(json.path("sourceRecordOrder").textValue()).isEqualTo("1");
        assertThat(json.path("occurredAt").textValue()).isEqualTo("2026-10-10T01:00:00Z");
        assertThat(json.has("before")).isFalse();
        assertThat(json.has("after")).isFalse();
        assertThat(json.has("status")).isFalse();
        assertThat(json.has("spuIdsCanonical")).isFalse();
        assertThatThrownBy(() -> event.getSpuIds().add(103L)).isInstanceOf(UnsupportedOperationException.class);

        JsonNode withUnknown = mapper.readTree(mapper.writeValueAsBytes(event));
        ((ObjectNode) withUnknown).put("marketable", true);
        assertThatThrownBy(() -> mapper.treeToValue(withUnknown, ProductRefreshRequestedEvent.class))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);

        String fingerprint = identity.contentFingerprint(event);
        ProductRefreshRequestedEvent conflictingPayload = new ProductRefreshEventConverterImpl().toTarget(
                ProductRefreshBO.builder().eventId(refresh.getEventId()).tenantId(TENANT_ID)
                        .cursor(source.getCursor()).spuIds(List.of(101L)).build());
        assertThat(conflictingPayload.getEventId()).isEqualTo(event.getEventId());
        assertThat(identity.contentFingerprint(conflictingPayload)).isNotEqualTo(fingerprint);
        assertThat(refresh.getSpuIds()).containsExactly(101L, 102L);
    }

    private static AdCdcChangeStrategy adStrategy(ValidationUtils validation) {
        return new AdCdcChangeStrategy(new CdcEventIdentityComponent(objectMapper()),
                new AdInvalidationEventConverterImpl(), validation);
    }

    private static ProductCdcChangeStrategy productStrategy(ValidationUtils validation) {
        return new ProductCdcChangeStrategy(new CdcEventIdentityComponent(objectMapper()),
                new ProductRefreshEventConverterImpl(), validation);
    }

    private static CdcSourceProperties sourceProperties() {
        return new CdcSourceProperties().setEnabled(true).setMaxPayloadBytes(1_048_576)
                .setConfirmTimeout(Duration.ofSeconds(5))
                .setSourceRegistry(Map.of("master_data", new CdcSourceProperties.SourceRegistration(
                        "enjoyshop_cdc_master_e1", "e1", "enjoyshop_cdc_master_e1", "goods_master", "public",
                        Set.of("ad", "spu", "sku"))));
    }

    private static CdcEventPublisher publisher(RabbitTemplate rabbitTemplate, ObjectMapper mapper,
            CdcSourceProperties properties, ValidatorFactory factory) {
        return new CdcEventPublisher(rabbitTemplate, mapper, properties,
                new ValidationUtils(factory.getValidator()));
    }

    private static CdcRawConsumer rawConsumer(ObjectMapper mapper, CdcSourceProperties properties,
            ValidatorFactory factory, Map<String, CdcChangeStrategy> strategies, CdcEventPublisher publisher) {
        return new CdcRawConsumer(mapper, properties, strategies, publisher,
                new ValidationUtils(factory.getValidator()));
    }

    private static Message rawMessage(ObjectMapper mapper, String sourceName, long beforeTenant,
            long afterTenant, long deliveryTag) throws IOException {
        return rawMessage(mapper, sourceName, beforeTenant, afterTenant, deliveryTag, false);
    }

    private static Message rawMessage(ObjectMapper mapper, String sourceName, long beforeTenant,
            long afterTenant, long deliveryTag, boolean reverseKeys) throws IOException {
        ObjectNode envelope = mapper.createObjectNode();
        ObjectNode before = mapper.createObjectNode();
        ObjectNode after = mapper.createObjectNode();
        if (reverseKeys) {
            before.put("position", "web_index_mobile");
            before.put("tenant_id", beforeTenant);
            before.put("id", 9001L);
            after.put("position", "web_index_lb");
            after.put("tenant_id", afterTenant);
            after.put("id", 9001L);
        } else {
            before.put("id", 9001L);
            before.put("tenant_id", beforeTenant);
            before.put("position", "web_index_mobile");
            after.put("id", 9001L);
            after.put("tenant_id", afterTenant);
            after.put("position", "web_index_lb");
        }
        ObjectNode source = mapper.createObjectNode();
        source.put("name", sourceName);
        source.put("db", "goods_master");
        source.put("schema", "public");
        source.put("table", "ad");
        source.put("lsn", 23_816_832L);
        source.put("txId", 17L);
        source.put("ts_ms", OCCURRED_AT.toEpochMilli());
        source.put("snapshot", false);
        envelope.set("source", source);
        ObjectNode transaction = mapper.createObjectNode();
        transaction.put("id", "17:23816832");
        transaction.put("total_order", 1);
        transaction.put("data_collection_order", 1);
        if (reverseKeys) {
            envelope.set("transaction", transaction);
            envelope.put("op", "u");
            envelope.set("after", after);
            envelope.set("source", source);
            envelope.set("before", before);
        } else {
            envelope.set("before", before);
            envelope.set("after", after);
            envelope.set("source", source);
            envelope.put("op", "u");
            envelope.set("transaction", transaction);
        }
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setReceivedExchange("enjoyshop.cdc.raw.v1");
        properties.setReceivedRoutingKey("master-data");
        properties.setDeliveryTag(deliveryTag);
        properties.setMessageId("raw-delivery-" + deliveryTag);
        return new Message(mapper.writeValueAsBytes(envelope), properties);
    }

    private static Message controlMessage(ObjectMapper mapper, String status, long deliveryTag, String routingKey)
            throws IOException {
        ObjectNode control = mapper.createObjectNode();
        control.put("status", status);
        control.put("id", "17:23816832");
        if ("END".equals(status)) {
            control.put("event_count", 1);
            ObjectNode collection = mapper.createObjectNode();
            collection.put("data_collection", "goods_master.public.ad");
            collection.put("event_count", 1);
            control.set("data_collections", mapper.createArrayNode().add(collection));
        }
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setReceivedExchange("enjoyshop.cdc.raw.v1");
        properties.setReceivedRoutingKey(routingKey);
        properties.setDeliveryTag(deliveryTag);
        return new Message(mapper.writeValueAsBytes(control), properties);
    }

    private static AdCacheInvalidationEvent adInvalidationEvent(String epoch) {
        return AdCacheInvalidationEvent.builder()
                .eventId("c".repeat(64))
                .schemaVersion(1)
                .tenantId(TENANT_ID)
                .sourceAlias("master_data")
                .sourceEpoch(epoch)
                .sourceLsn("0/016B6A80")
                .sourceTxId("17")
                .sourceRecordOrder("1")
                .occurredAt(OCCURRED_AT)
                .sourceTable("ad")
                .operation(CdcOperationEnum.UPDATE)
                .positions(List.of("web_index_lb", "web_index_mobile"))
                .build();
    }

    private static Message semanticMessage(ObjectMapper mapper, AdCacheInvalidationEvent event,
            long deliveryTag, String tenantHeader) throws IOException {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setReceivedExchange("enjoyshop.sync.v1");
        properties.setReceivedRoutingKey("ad.invalidate.v1");
        properties.setDeliveryTag(deliveryTag);
        properties.setMessageId(event.getEventId());
        properties.setHeader("schemaVersion", 1);
        properties.setHeader("tenantId", tenantHeader);
        properties.setHeader("sourceAlias", event.getSourceAlias());
        properties.setHeader("sourceEpoch", event.getSourceEpoch());
        properties.setHeader("sourceTable", event.getSourceTable());
        properties.setHeader("route", "ad.invalidate.v1");
        return new Message(mapper.writeValueAsBytes(event), properties);
    }

    public static final class ListenerMethodFixture {

        public void onMessage(Channel channel, Message message) {
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CdcRabbitTestConfiguration {

        @Bean("rabbitConnectionFactory")
        ConnectionFactory rabbitConnectionFactory() {
            CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
            connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
            connectionFactory.setPublisherReturns(true);
            return connectionFactory;
        }

        @Bean("spring.rabbitmq-org.springframework.boot.autoconfigure.amqp.RabbitProperties")
        RabbitProperties rabbitProperties() {
            RabbitProperties properties = new RabbitProperties();
            properties.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
            properties.setPublisherReturns(true);
            return properties;
        }

        @Bean("simpleRabbitListenerContainerFactoryConfigurer")
        SimpleRabbitListenerContainerFactoryConfigurer listenerFactoryConfigurer(
                @Qualifier("spring.rabbitmq-org.springframework.boot.autoconfigure.amqp.RabbitProperties")
                RabbitProperties properties) {
            return new SimpleRabbitListenerContainerFactoryConfigurer(properties);
        }

        @Bean("jacksonObjectMapper")
        ObjectMapper jacksonObjectMapper() {
            return objectMapper();
        }

        @Bean("egonColaValidationUtils")
        ValidationUtils validationUtils() {
            return new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());
        }

        @Bean("cdcEventIdentityComponent")
        CdcEventIdentityComponent cdcEventIdentityComponent(
                @Qualifier("jacksonObjectMapper") ObjectMapper mapper) {
            return new CdcEventIdentityComponent(mapper);
        }

        @Bean("adInvalidationEventConverterImpl")
        AdInvalidationEventConverterImpl adInvalidationEventConverter() {
            return new AdInvalidationEventConverterImpl();
        }

        @Bean("productRefreshEventConverterImpl")
        ProductRefreshEventConverterImpl productRefreshEventConverter() {
            return new ProductRefreshEventConverterImpl();
        }

        @Bean("adCdcChangeStrategy")
        AdCdcChangeStrategy adCdcChangeStrategy(
                @Qualifier("cdcEventIdentityComponent") CdcEventIdentityComponent identity,
                @Qualifier("adInvalidationEventConverterImpl") AdInvalidationEventConverterImpl converter,
                @Qualifier("egonColaValidationUtils") ValidationUtils validation) {
            return new AdCdcChangeStrategy(identity, converter, validation);
        }

        @Bean("productCdcChangeStrategy")
        ProductCdcChangeStrategy productCdcChangeStrategy(
                @Qualifier("cdcEventIdentityComponent") CdcEventIdentityComponent identity,
                @Qualifier("productRefreshEventConverterImpl") ProductRefreshEventConverterImpl converter,
                @Qualifier("egonColaValidationUtils") ValidationUtils validation) {
            return new ProductCdcChangeStrategy(identity, converter, validation);
        }
    }

    private static CdcRecordBO record(String table, CdcOperationEnum operation, Long rowPk) {
        return CdcRecordBO.builder()
                .cursor(CdcSourceCursorBO.builder()
                        .sourceAlias("master_data")
                        .sourceEpoch("e1")
                        .schema("public")
                        .table(table)
                        .operation(operation)
                        .rowPk(rowPk)
                        .sourceLsn(BigInteger.valueOf(23_816_832L))
                        .sourceTxId("17")
                        .sourceRecordOrder("1")
                        .sourceTsMs(OCCURRED_AT.toEpochMilli())
                        .build())
                .factFingerprint("a".repeat(64))
                .build();
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
