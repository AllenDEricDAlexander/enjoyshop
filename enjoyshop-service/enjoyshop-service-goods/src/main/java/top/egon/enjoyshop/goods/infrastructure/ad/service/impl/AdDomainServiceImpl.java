package top.egon.enjoyshop.goods.infrastructure.ad.service.impl;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.common.validation.AdValidationGroup;
import top.egon.enjoyshop.goods.domain.ad.AdBO;
import top.egon.enjoyshop.goods.domain.ad.AdDomainQuery;
import top.egon.enjoyshop.goods.domain.ad.AdDomainService;
import top.egon.enjoyshop.goods.infrastructure.ad.converter.AdPersistenceConverter;
import top.egon.enjoyshop.goods.infrastructure.ad.po.AdPO;
import top.egon.enjoyshop.goods.infrastructure.ad.repo.AdRepository;
import top.egon.enjoyshop.goods.infrastructure.config.AdProperties;

@Service("adDomainService")
@Validated
@Slf4j
@RequiredArgsConstructor
public class AdDomainServiceImpl implements AdDomainService {

    @Qualifier("adRepository")
    private final AdRepository repository;

    @Qualifier("adPersistenceConverterImpl")
    private final AdPersistenceConverter converter;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validation;

    @Qualifier("app.ad-top.egon.enjoyshop.goods.infrastructure.config.AdProperties")
    private final AdProperties properties;

    @Override
    @Transactional
    @Validated(AdValidationGroup.Create.class)
    public AdBO create(@jakarta.validation.Valid AdBO value) {
        validation.validate(value, AdValidationGroup.Create.class);
        long id;
        try {
            id = SnowflakeIdGenerator.nextLongId();
        } catch (RuntimeException failure) {
            log.warn("advertisement id generation failed");
            throw new CommonException(GoodsErrorStatus.AD_ID_UNAVAILABLE, failure);
        }
        AdPO row = converter.toSource(value);
        row.setId(id);
        if (!repository.save(row)) {
            throw versionConflict();
        }
        log.info("created advertisement id={}", row.getId());
        return converter.toTarget(row);
    }

    @Override
    @Transactional
    @Validated(AdValidationGroup.Update.class)
    public AdBO replace(@jakarta.validation.Valid AdBO value) {
        validation.validate(value, AdValidationGroup.Update.class);
        AdDomainQuery query = lifecycleQuery(value.getId());
        AdPO current = repository.lockByLifecycle(query);
        if (current == null) {
            throw new CommonException(GoodsErrorStatus.AD_NOT_FOUND);
        }
        requireVersion(current, value.getVersion());
        converter.mergeForUpdate(value, current);
        if (!repository.updateById(current)) {
            throw versionConflict();
        }
        log.info("replaced advertisement id={} version={}", current.getId(), current.getVersion());
        return converter.toTarget(current);
    }

    @Override
    @Transactional
    @Validated(AdValidationGroup.Delete.class)
    public boolean delete(@jakarta.validation.Valid AdBO value) {
        validation.validate(value, AdValidationGroup.Delete.class);
        AdPO current = repository.lockByLifecycle(lifecycleQuery(value.getId()));
        if (current == null) {
            return true;
        }
        requireVersion(current, value.getVersion());
        if (!repository.removeById(current)) {
            throw versionConflict();
        }
        log.info("deleted advertisement id={} version={}", current.getId(), current.getVersion());
        return true;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    @Validated(AdValidationGroup.Detail.class)
    public AdBO detail(@jakarta.validation.Valid AdDomainQuery query) {
        AdDomainQuery scoped = bindTenant(query);
        validation.validate(scoped, AdValidationGroup.Detail.class);
        AdPO row = repository.selectByLifecycle(scoped);
        if (row == null) {
            throw new CommonException(GoodsErrorStatus.AD_NOT_FOUND);
        }
        return converter.toTarget(row);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    @Validated(AdValidationGroup.Page.class)
    public PageResultRecord<AdBO> page(@jakarta.validation.Valid AdDomainQuery query) {
        AdDomainQuery scoped = bindTenant(query);
        validation.validate(scoped, AdValidationGroup.Page.class);
        long total = repository.countPage(scoped);
        List<AdBO> records = repository.selectPage(scoped).stream().map(converter::toTarget).toList();
        return PageResultRecord.success(records, total, scoped.getPage().pageNo(), scoped.getPage().pageSize());
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    @Validated(AdValidationGroup.PublicRead.class)
    public List<AdBO> publishedCandidates(@jakarta.validation.Valid AdDomainQuery query) {
        AdDomainQuery scoped = bindTenant(query);
        validation.validate(scoped, AdValidationGroup.PublicRead.class);
        List<AdPO> rows = repository.selectCandidates(scoped, properties.getCandidateLimit() + 1);
        if (rows.size() > properties.getCandidateLimit()) {
            throw new CommonException(GoodsErrorStatus.AD_DATA_LIMIT);
        }
        return rows.stream().map(converter::toTarget).toList();
    }

    private static AdDomainQuery bindTenant(AdDomainQuery query) {
        Long trustedTenantId = EgonColaTenantIdProvider.currentTenantId();
        if (query.getTenantId() != null && !Objects.equals(query.getTenantId(), trustedTenantId)) {
            throw new CommonException(GoodsErrorStatus.AD_FORBIDDEN);
        }
        query.setTenantId(trustedTenantId);
        return query;
    }

    private static AdDomainQuery lifecycleQuery(Long id) {
        return AdDomainQuery.builder().id(id).tenantId(EgonColaTenantIdProvider.currentTenantId()).build();
    }

    private static void requireVersion(AdPO current, Long expected) {
        if (!Objects.equals(current.getVersion(), expected)) {
            throw versionConflict();
        }
    }

    private static CommonException versionConflict() {
        return new CommonException(GoodsErrorStatus.AD_VERSION_CONFLICT);
    }
}
