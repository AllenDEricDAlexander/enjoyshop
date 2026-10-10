package top.egon.enjoyshop.goods.infrastructure.ad.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.RedisException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.cache.EgonColaCachePort;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.common.validation.AdValidationGroup;
import top.egon.enjoyshop.goods.domain.ad.AdDomainQuery;
import top.egon.enjoyshop.goods.infrastructure.ad.dao.AdDAO;
import top.egon.enjoyshop.goods.infrastructure.ad.po.AdPO;
import top.egon.enjoyshop.goods.infrastructure.config.AdProperties;

@Slf4j
@Validated
@Repository("adRepository")
@RequiredArgsConstructor
public class AdRepository extends EgonColaRepository<AdDAO, AdPO> {

    private static final String AD_CANDIDATES_CACHE = "AdCandidates";
    private static final String AD_PROPERTIES_BEAN =
            "app.ad-top.egon.enjoyshop.goods.infrastructure.config.AdProperties";

    @Getter
    @Qualifier("adDAO")
    private final AdDAO baseMapper;

    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Qualifier("egonColaCachePort")
    private final ObjectProvider<EgonColaCachePort> cachePortProvider;

    @Qualifier("adPrimaryReadTransactionTemplate")
    private final TransactionTemplate primaryReadTransactionTemplate;

    @Qualifier(AD_PROPERTIES_BEAN)
    private final AdProperties adProperties;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validation;

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Validated(AdValidationGroup.Detail.class)
    public AdPO selectByLifecycle(@Valid @NotNull AdDomainQuery query) {
        return baseMapper.selectByLifecycle(query, requireTenant(query));
    }

    @Validated(AdValidationGroup.Detail.class)
    public AdPO lockByLifecycle(@Valid @NotNull AdDomainQuery query) {
        return baseMapper.lockByLifecycle(query, requireTenant(query));
    }

    @Validated(AdValidationGroup.Page.class)
    public List<AdPO> selectPage(@Valid @NotNull AdDomainQuery query) {
        return baseMapper.selectPage(query, requireTenant(query), query.getPage().pageSize(), query.getPage().offset());
    }

    @Validated(AdValidationGroup.Page.class)
    public long countPage(@Valid @NotNull AdDomainQuery query) {
        return baseMapper.countPage(query, requireTenant(query));
    }

    @Validated(AdValidationGroup.PublicRead.class)
    public List<AdPO> selectCandidates(@Valid @NotNull AdDomainQuery query, @Min(1) int limit) {
        return baseMapper.selectCandidates(query, requireTenant(query), limit);
    }

    @Validated(AdValidationGroup.PublicRead.class)
    public List<AdPO> cachedCandidates(@Valid @NotNull AdDomainQuery query) {
        Long tenantId = requireTenant(query);
        EgonColaCachePort cache = cachePortProvider.getIfAvailable();
        if (cache == null) {
            return loadCandidates(query, tenantId);
        }
        String key = tenantId + ":" + query.getPosition();
        Object value;
        try {
            value = cache.get(AD_CANDIDATES_CACHE, key,
                    () -> encodeCandidates(loadCandidates(query, tenantId)));
        } catch (Cache.ValueRetrievalException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtimeFailure) {
                throw runtimeFailure;
            }
            throw new CommonException(GoodsErrorStatus.AD_DEPENDENCY_UNAVAILABLE, failure);
        } catch (RedisException failure) {
            log.warn("advertisement candidate cache read failed; using bounded PRIMARY loader");
            return loadCandidates(query, tenantId);
        } catch (DataAccessException failure) {
            throw new CommonException(GoodsErrorStatus.AD_DEPENDENCY_UNAVAILABLE, failure);
        }
        return cachedCandidates(value);
    }

    public void registerAdvertisementEviction(String... positions) {
        evictPositionKeys(positions == null ? List.of() : Arrays.asList(positions));
    }

    public void evictCandidates(Collection<String> positions) {
        evictPositionKeys(positions);
    }

    private List<AdPO> loadCandidates(AdDomainQuery query, Long tenantId) {
        List<AdPO> rows = primaryReadTransactionTemplate.execute(status ->
                baseMapper.selectCandidates(query, tenantId, adProperties.getCandidateLimit() + 1));
        if (rows == null) {
            throw new CommonException(GoodsErrorStatus.AD_DEPENDENCY_UNAVAILABLE);
        }
        return validatePersistedRows(rows);
    }

    private List<AdPO> validatePersistedRows(List<AdPO> rows) {
        if (rows.size() > adProperties.getCandidateLimit()) {
            throw new CommonException(GoodsErrorStatus.AD_DATA_LIMIT);
        }
        try {
            for (AdPO row : rows) {
                if (row == null) {
                    throw invalidCachedData();
                }
                validation.validate(row,
                        top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups.Persisted.class);
            }
        } catch (ConstraintViolationException invalidStoredRow) {
            log.warn("advertisement candidate data failed persisted validation");
            throw new CommonException(GoodsErrorStatus.AD_DATA_INVALID, invalidStoredRow);
        }
        return List.copyOf(rows);
    }

    private String encodeCandidates(List<AdPO> rows) {
        try {
            return objectMapper.writeValueAsString(rows);
        } catch (JsonProcessingException failure) {
            throw new CommonException(GoodsErrorStatus.AD_DATA_INVALID, failure);
        }
    }

    private List<AdPO> cachedCandidates(Object value) {
        if (!(value instanceof String payload)) {
            throw invalidCachedData();
        }
        try {
            List<AdPO> rows = objectMapper.readValue(payload, new TypeReference<>() { });
            if (rows == null) {
                throw invalidCachedData();
            }
            return validatePersistedRows(rows);
        } catch (JsonProcessingException failure) {
            throw new CommonException(GoodsErrorStatus.AD_DATA_INVALID, failure);
        }
    }

    private void evictPositionKeys(Collection<String> positions) {
        if (positions == null || positions.isEmpty()) {
            return;
        }
        EgonColaCachePort cache = cachePortProvider.getIfAvailable();
        if (cache == null) {
            return;
        }
        long tenantId = EgonColaTenantIdProvider.currentTenantId();
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String position : positions) {
            if (position != null) {
                keys.add(tenantId + ":" + position);
            }
        }
        if (!keys.isEmpty()) {
            cache.registerEvictionAfterCommit(AD_CANDIDATES_CACHE, keys, List.of());
        }
    }

    private static CommonException invalidCachedData() {
        return new CommonException(GoodsErrorStatus.AD_DATA_INVALID);
    }

    private static Long requireTenant(AdDomainQuery query) {
        Long currentTenant = EgonColaTenantIdProvider.currentTenantId();
        if (!Objects.equals(currentTenant, query.getTenantId())) {
            throw new CommonException(GoodsErrorStatus.AD_FORBIDDEN);
        }
        return currentTenant;
    }
}
