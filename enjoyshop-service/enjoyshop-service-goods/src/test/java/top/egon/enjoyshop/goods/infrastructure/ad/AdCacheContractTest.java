package top.egon.enjoyshop.goods.infrastructure.ad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.client.RedisException;
import org.slf4j.MDC;
import top.egon.cola.component.common.cache.codec.EgonColaCacheCodecs;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.cache.Cache;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;
import top.egon.cola.component.common.cache.core.EgonColaTwoLevelCacheManager;
import top.egon.cola.component.common.cache.port.EgonColaTwoLevelCachePort;
import top.egon.cola.component.common.core.cache.EgonColaCachePort;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.enjoyshop.goods.application.ad.AdApplicationConverter;
import top.egon.enjoyshop.goods.application.ad.AdAuthorizationComponent;
import top.egon.enjoyshop.goods.application.ad.AdManageImpl;
import top.egon.enjoyshop.goods.application.ad.AdPageQuery;
import top.egon.enjoyshop.goods.application.ad.AdPublicResult;
import top.egon.enjoyshop.goods.application.ad.AdPublicResultConverter;
import top.egon.enjoyshop.goods.application.ad.AdReadQuery;
import top.egon.enjoyshop.goods.application.ad.AdResultConverter;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.common.validation.AdValidationGroup;
import top.egon.enjoyshop.goods.common.validation.ValidAdWindow;
import top.egon.enjoyshop.goods.domain.ad.AdBO;
import top.egon.enjoyshop.goods.domain.ad.AdDomainQuery;
import top.egon.enjoyshop.goods.domain.ad.AdDomainService;
import top.egon.enjoyshop.goods.domain.ad.AdEligibilitySpecification;
import top.egon.enjoyshop.goods.domain.ad.AdStatusEnum;
import top.egon.enjoyshop.goods.infrastructure.ad.converter.AdPersistenceConverter;
import top.egon.enjoyshop.goods.infrastructure.ad.dao.AdDAO;
import top.egon.enjoyshop.goods.infrastructure.ad.po.AdPO;
import top.egon.enjoyshop.goods.infrastructure.ad.repo.AdRepository;
import top.egon.enjoyshop.goods.infrastructure.config.AdConfiguration;
import top.egon.enjoyshop.goods.infrastructure.config.AdProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Contract tests for the candidate-cache boundary, primary refill and commit-time invalidation SPI. */
class AdCacheContractTest {

    private static final String REGION = "AdCandidates";
    private static final String POSITION = "web_index_lb";
    private static final long TENANT_ID = 41L;
    private static final Instant NOW = Instant.parse("2026-10-10T00:00:00Z");

    @AfterEach
    void clearThreadState() {
        MDC.clear();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void cacheHitBypassesLoaderAndDao() throws Exception {
        MDC.put("tenantId", Long.toString(TENANT_ID));
        AdDAO dao = mock(AdDAO.class);
        EgonColaCachePort cache = mock(EgonColaCachePort.class);
        RecordingTransactionTemplate transaction = new RecordingTransactionTemplate();
        AdPO cached = candidate(1L);
        String payload = cacheObjectMapper().writeValueAsString(List.of(cached));
        ObjectMapper restrictedCodecMapper = EgonColaCacheCodecs.valueMapper();
        String encodedCacheValue = restrictedCodecMapper.writeValueAsString(payload);
        assertThat(restrictedCodecMapper.readValue(encodedCacheValue, Object.class)).isEqualTo(payload);
        when(cache.get(eq(REGION), eq("41:" + POSITION), any()))
                .thenReturn(payload);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            AdRepository repository = repository(dao, cache, transaction, validation(factory));
            assertThat(repository.cachedCandidates(query())).extracting(AdPO::getId).containsExactly(1L);
        }

        assertThat(transaction.calls).isZero();
        verify(dao, never()).selectCandidates(any(), any(), anyInt());
        verify(cache).get(eq(REGION), eq("41:" + POSITION), any());
    }

    @Test
    void primaryMissUsesBoundedReadTransactionAndCacheDisabledFallsBack() throws Exception {
        MDC.put("tenantId", Long.toString(TENANT_ID));
        AdDAO dao = mock(AdDAO.class);
        EgonColaCachePort cache = mock(EgonColaCachePort.class);
        RecordingTransactionTemplate transaction = new RecordingTransactionTemplate();
        AdPO row = candidate(2L);
        when(cache.get(eq(REGION), eq("41:" + POSITION), any())).thenAnswer(invocation -> {
            Object payload = ((Supplier<?>) invocation.getArgument(2)).get();
            assertThat(payload).isInstanceOf(String.class);
            return payload;
        });
        when(dao.selectCandidates(any(), eq(TENANT_ID), eq(201))).thenAnswer(invocation -> {
            assertThat(transaction.active).isTrue();
            assertThat(((AdDomainQuery) invocation.getArgument(0)).getTenantId()).isEqualTo(TENANT_ID);
            return List.of(row);
        });

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            AdRepository cachedRepository = repository(dao, cache, transaction, validation(factory));
            assertThat(cachedRepository.cachedCandidates(query())).extracting(AdPO::getId).containsExactly(2L);

            transaction.reset();
            AdRepository uncachedRepository = repository(dao, null, transaction, validation(factory));
            assertThat(uncachedRepository.cachedCandidates(query())).extracting(AdPO::getId).containsExactly(2L);
        }

        assertThat(transaction.calls).isEqualTo(1);
        assertThat(transaction.active).isFalse();
        verify(cache).get(eq(REGION), eq("41:" + POSITION), any());
        verify(dao, org.mockito.Mockito.times(2)).selectCandidates(any(), eq(TENANT_ID), eq(201));
    }

    @Test
    void redisReadExceptionFallsBackToPrimaryButInvalidCachedValueFailsClosed() throws Exception {
        MDC.put("tenantId", Long.toString(TENANT_ID));
        AdDAO dao = mock(AdDAO.class);
        EgonColaCachePort cache = mock(EgonColaCachePort.class);
        RecordingTransactionTemplate transaction = new RecordingTransactionTemplate();
        AdPO row = candidate(3L);
        when(cache.get(eq(REGION), eq("41:" + POSITION), any()))
                .thenThrow(new RedisException("cache unavailable"));
        when(dao.selectCandidates(any(), eq(TENANT_ID), eq(201))).thenReturn(List.of(row));

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            AdRepository repository = repository(dao, cache, transaction, validation(factory));
            assertThat(repository.cachedCandidates(query())).containsExactly(row);
            when(cache.get(eq(REGION), eq("41:" + POSITION), any())).thenReturn(List.of("wrong-type"));
            assertThatThrownBy(() -> repository.cachedCandidates(query()))
                    .isInstanceOf(CommonException.class)
                    .extracting(failure -> ((CommonException) failure).getCode())
                    .isEqualTo(GoodsErrorStatus.AD_DATA_INVALID.getCode());
        }
        assertThat(transaction.calls).isEqualTo(1);
    }

    @Test
    void candidateOverflowAndInvalidPersistedRowsReturnDataErrors() throws Exception {
        MDC.put("tenantId", Long.toString(TENANT_ID));
        AdDAO dao = mock(AdDAO.class);
        EgonColaCachePort cache = mock(EgonColaCachePort.class);
        RecordingTransactionTemplate transaction = new RecordingTransactionTemplate();
        when(cache.get(eq(REGION), eq("41:" + POSITION), any())).thenAnswer(invocation ->
                ((Supplier<?>) invocation.getArgument(2)).get());
        when(dao.selectCandidates(any(), eq(TENANT_ID), eq(201))).thenReturn(
                java.util.stream.LongStream.rangeClosed(1, 201).mapToObj(AdCacheContractTest::candidate).toList());

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            AdRepository repository = repository(dao, cache, transaction, validation(factory));
            assertThatThrownBy(() -> repository.cachedCandidates(query()))
                    .isInstanceOf(CommonException.class)
                    .extracting(failure -> ((CommonException) failure).getCode())
                    .isEqualTo(GoodsErrorStatus.AD_DATA_LIMIT.getCode());

            when(dao.selectCandidates(any(), eq(TENANT_ID), eq(201))).thenReturn(List.of(candidate(301L).setName(null)));
            assertThatThrownBy(() -> repository.cachedCandidates(query()))
                    .isInstanceOf(CommonException.class)
                    .extracting(failure -> ((CommonException) failure).getCode())
                    .isEqualTo(GoodsErrorStatus.AD_DATA_INVALID.getCode());
        }
        assertThat(transaction.calls).isEqualTo(2);
    }

    @Test
    void writesRegisterDistinctTenantScopedOldAndNewPositionKeys() {
        MDC.put("tenantId", Long.toString(TENANT_ID));
        EgonColaCachePort cache = mock(EgonColaCachePort.class);
        AdRepository repository = repository(mock(AdDAO.class), cache,
                new RecordingTransactionTemplate(), noOpValidation());

        repository.registerAdvertisementEviction(POSITION, "web_index_mobile", POSITION);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Collection<String>> keys = org.mockito.ArgumentCaptor.forClass(Collection.class);
        verify(cache).registerEvictionAfterCommit(eq(REGION), keys.capture(), eq(List.of()));
        assertThat(keys.getValue()).containsExactly("41:web_index_lb", "41:web_index_mobile");
    }

    @Test
    void cacheEvictionFailureIsSwallowedByTheExistingAfterCommitPort() {
        MDC.put("tenantId", Long.toString(TENANT_ID));
        EgonColaTwoLevelCacheManager manager = mock(EgonColaTwoLevelCacheManager.class);
        doThrow(new RedisException("L2 unavailable"))
                .when(manager).applyLocalEviction(eq(REGION), eq(List.of("41:" + POSITION)));
        EgonColaTwoLevelCachePort port = new EgonColaTwoLevelCachePort(manager, new EgonColaCacheProperties());

        TransactionSynchronizationManager.initSynchronization();
        port.registerEvictionAfterCommit(REGION, List.of("41:" + POSITION), List.of());
        var synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertThatCode(() -> synchronizations.forEach(value -> value.afterCommit())).doesNotThrowAnyException();
        verify(manager).applyLocalEviction(REGION, List.of("41:" + POSITION));
        verify(manager, never()).publish(anyString(), any(), anyList());
    }

    @Test
    void primaryTemplateUsesNewReadOnlyRepeatableReadWithTheConfiguredTimeout() {
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        TransactionTemplate template = new AdConfiguration().adPrimaryReadTransactionTemplate(
                manager, AdProperties.builder().loadTimeout(Duration.ofSeconds(2)).build());

        assertThat(template.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(template.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        assertThat(template.isReadOnly()).isTrue();
        assertThat(template.getTimeout()).isEqualTo(2);
    }

    @Test
    void publicCandidatesUseOneUtcClockOutsideAnOuterTransactionAndKeepWindowSemantics() {
        Instant now = NOW;
        CountingClock clock = new CountingClock(now);
        AdDomainService domain = mock(AdDomainService.class);
        AdApplicationConverter applicationConverter = mock(AdApplicationConverter.class);
        AdPublicResultConverter publicConverter = mock(AdPublicResultConverter.class);
        AdDomainQuery domainQuery = AdDomainQuery.builder().tenantId(TENANT_ID).position(POSITION).limit(20).build();
        when(applicationConverter.toDomainQuery(any(AdReadQuery.class))).thenReturn(domainQuery);
        List<AdBO> candidates = List.of(
                AdBO.builder().id(1L).name("current").status(AdStatusEnum.ENABLED).startAt(now).endAt(now.plusSeconds(1)).build(),
                AdBO.builder().id(2L).name("future").status(AdStatusEnum.ENABLED).startAt(now.plusSeconds(1)).endAt(now.plusSeconds(2)).build(),
                AdBO.builder().id(3L).name("ended").status(AdStatusEnum.ENABLED).startAt(now.minusSeconds(1)).endAt(now).build(),
                AdBO.builder().id(4L).name("disabled").status(AdStatusEnum.DISABLED).startAt(now).endAt(now.plusSeconds(2)).build());
        when(domain.publishedCandidates(domainQuery)).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return candidates;
        });
        when(publicConverter.toTarget(any(AdBO.class))).thenAnswer(invocation -> {
            AdBO candidate = invocation.getArgument(0);
            return new AdPublicResult().setId(candidate.getId().toString()).setName(candidate.getName());
        });
        AdManageImpl manage = new AdManageImpl(mock(AdAuthorizationComponent.class), domain,
                applicationConverter, mock(AdResultConverter.class), publicConverter,
                noOpValidation(), clock, new AdEligibilitySpecification());

        List<AdPublicResult> published = manage.published(new AdReadQuery().setPosition(POSITION).setLimit(20));

        assertThat(clock.calls.get()).isEqualTo(1);
        assertThat(published).extracting(AdPublicResult::getName).containsExactly("current");
    }

    @Test
    void publicReadValidatesStoredWindowRows() {
        ValidAdWindow window = AdBO.class.getAnnotation(ValidAdWindow.class);
        assertThat(window.groups()).contains(AdValidationGroup.PublicRead.class);

        MDC.put("tenantId", Long.toString(TENANT_ID));
        AdRepository repository = mock(AdRepository.class);
        AdPersistenceConverter converter = mock(AdPersistenceConverter.class);
        AdDomainQuery query = AdDomainQuery.builder().position(POSITION).limit(20).build();
        AdBO invalidCandidate = AdBO.builder().id(99L).status(AdStatusEnum.ENABLED)
                .startAt(NOW).endAt(NOW.minusNanos(1_000)).build();
        AdPO cachedRow = candidate(99L);
        when(repository.cachedCandidates(any())).thenReturn(List.of(cachedRow));
        when(converter.toTarget(cachedRow)).thenReturn(invalidCandidate);
        Validator validator = mock(Validator.class);
        @SuppressWarnings("unchecked")
        ConstraintViolation<AdBO> violation = mock(ConstraintViolation.class);
        doAnswer(invocation -> {
            Object value = invocation.getArgument(0);
            return value == invalidCandidate ? Set.of(violation) : Set.of();
        }).when(validator).validate(any(), any(Class[].class));
        var service = new top.egon.enjoyshop.goods.infrastructure.ad.service.impl.AdDomainServiceImpl(
                repository, converter, new ValidationUtils(validator));

        assertThatThrownBy(() -> service.publishedCandidates(query))
                .isInstanceOf(CommonException.class)
                .extracting(failure -> ((CommonException) failure).getCode())
                .isEqualTo(GoodsErrorStatus.AD_DATA_INVALID.getCode());
    }

    @Test
    void publicReadRejectsMissingPositionBeforeRepositoryAccess() {
        MDC.put("tenantId", Long.toString(TENANT_ID));
        AdRepository repository = mock(AdRepository.class);
        Validator validator = mock(Validator.class);
        @SuppressWarnings("unchecked")
        ConstraintViolation<AdDomainQuery> violation = mock(ConstraintViolation.class);
        AdDomainQuery query = AdDomainQuery.builder().limit(20).build();
        doAnswer(invocation -> invocation.getArgument(0) == query ? Set.of(violation) : Set.of())
                .when(validator).validate(any(), any(Class[].class));
        var service = new top.egon.enjoyshop.goods.infrastructure.ad.service.impl.AdDomainServiceImpl(
                repository, mock(AdPersistenceConverter.class), new ValidationUtils(validator));

        assertThatThrownBy(() -> service.publishedCandidates(query))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        org.mockito.Mockito.verifyNoInteractions(repository);
    }

    private static AdRepository repository(AdDAO dao, EgonColaCachePort cache,
            RecordingTransactionTemplate transaction, ValidationUtils validation) {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        if (cache != null) {
            factory.addBean("egonColaCachePort", cache);
        }
        ObjectProvider<EgonColaCachePort> cacheProvider = factory.getBeanProvider(EgonColaCachePort.class);
        return new AdRepository(dao, mock(EgonColaMybatisPlusProperties.class), cacheProvider,
                transaction, AdProperties.builder().build(), validation, cacheObjectMapper());
    }

    private static ObjectMapper cacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private static AdDomainQuery query() {
        return AdDomainQuery.builder().tenantId(TENANT_ID).position(POSITION).limit(20).build();
    }

    private static AdPO candidate(long id) {
        AdPO row = new AdPO();
        row.setId(id);
        row.setTenantId(TENANT_ID);
        row.setCreateUserId("editor");
        row.setCreateTime(NOW);
        row.setUpdateUserId("editor");
        row.setUpdateTime(NOW);
        row.setVersion(0L);
        row.setName("current");
        row.setPosition(POSITION);
        row.setImageUrl("https://images.example.test/products/ad.png");
        row.setTargetUrl("/events/spring");
        row.setStatus(AdStatusEnum.ENABLED);
        row.setStartTime(NOW);
        row.setEndTime(NOW.plusSeconds(60));
        row.setSortOrder(1);
        return row;
    }

    private static ValidationUtils validation(ValidatorFactory factory) {
        return new ValidationUtils(factory.getValidator());
    }

    private static ValidationUtils noOpValidation() {
        Validator validator = mock(Validator.class);
        doReturn(Set.of()).when(validator).validate(any(), any(Class[].class));
        return new ValidationUtils(validator);
    }

    private static final class RecordingTransactionTemplate extends TransactionTemplate {
        private boolean active;
        private int calls;

        private RecordingTransactionTemplate() {
            super(mock(PlatformTransactionManager.class));
        }

        @Override
        public <T> T execute(TransactionCallback<T> action) {
            calls++;
            active = true;
            try {
                return action.doInTransaction(mock(TransactionStatus.class));
            } finally {
                active = false;
            }
        }

        private void reset() {
            calls = 0;
            active = false;
        }
    }

    private static final class CountingClock extends Clock {
        private final Instant now;
        private final AtomicInteger calls = new AtomicInteger();

        private CountingClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            calls.incrementAndGet();
            return now;
        }
    }
}
