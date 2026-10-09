package top.egon.enjoyshop.goods.infrastructure.goods.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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
import top.egon.enjoyshop.goods.common.validation.ProductValidationGroup;
import top.egon.enjoyshop.goods.domain.goods.CatalogReferenceBO;
import top.egon.enjoyshop.goods.domain.goods.ProductBO;
import top.egon.enjoyshop.goods.domain.goods.ProductLifecycleComponent;
import top.egon.enjoyshop.goods.domain.goods.ProductActionEnum;
import top.egon.enjoyshop.goods.domain.goods.ProductDomainQuery;
import top.egon.enjoyshop.goods.domain.goods.ProductDomainService;
import top.egon.enjoyshop.goods.domain.goods.ProductStateBO;
import top.egon.enjoyshop.goods.domain.goods.SkuBO;
import top.egon.enjoyshop.goods.domain.goods.SpecBO;
import top.egon.enjoyshop.goods.domain.goods.SpuBO;
import top.egon.enjoyshop.goods.infrastructure.goods.po.CategoryBrandPO;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SkuPO;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SpuPO;
import top.egon.enjoyshop.goods.infrastructure.goods.repo.CategoryBrandRepository;
import top.egon.enjoyshop.goods.infrastructure.goods.repo.SkuRepository;
import top.egon.enjoyshop.goods.infrastructure.goods.repo.SpuRepository;

@Service("productDomainService")
@Validated
@Slf4j
@RequiredArgsConstructor
public class ProductDomainServiceImpl implements ProductDomainService {
    @Qualifier("spuRepository")
    private final SpuRepository spuRepository;
    @Qualifier("skuRepository")
    private final SkuRepository skuRepository;
    @Qualifier("categoryBrandRepository")
    private final CategoryBrandRepository categoryBrandRepository;
    @Qualifier("productSpuPersistenceConverterImpl")
    private final ProductSpuPersistenceConverter spuConverter;
    @Qualifier("productSkuPersistenceConverterImpl")
    private final ProductSkuPersistenceConverter skuConverter;
    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validation;

    @Qualifier("productLifecycleComponent")
    private final ProductLifecycleComponent lifecycle;

    @Override
    @Transactional
    @Validated(ProductValidationGroup.Create.class)
    public ProductBO create(ProductBO value) {
        validation.validate(value, ProductValidationGroup.Create.class);
        CatalogReferenceBO references = references(value.getSpu());
        validateCatalog(value.getSpu(), references);
        ensureAssociation(references);
        SpuBO input = value.getSpu();
        input.setStatus("0").setIsMarketable(false).setSaleNum(0).setCommentNum(0);
        SpuPO parent = spuConverter.toSource(input);
        if (!spuRepository.save(parent)) {
            throw versionConflict();
        }
        List<SkuPO> written = new ArrayList<>();
        for (SkuBO child : value.getSkuList()) {
            prepareSku(child, input, parent.getId(), references);
            child.setSaleNum(0).setCommentNum(0);
            SkuPO row = skuConverter.toSource(child);
            if (!skuRepository.save(row)) {
                throw versionConflict();
            }
            written.add(row);
        }
        log.info("created product id={} sku_count={}", parent.getId(), written.size());
        return aggregate(parent, written);
    }

    @Override
    @Transactional
    @Validated(ProductValidationGroup.Update.class)
    public ProductBO update(ProductBO value) {
        validation.validate(value, ProductValidationGroup.Update.class);
        requireParent(spuRepository.selectByLifecycle(query(value.getSpu().getId(), false)));
        CatalogReferenceBO references = references(value.getSpu());
        validateCatalog(value.getSpu(), references);
        SpuPO parent = spuRepository.lockByLifecycle(query(value.getSpu().getId(), false));
        requireParent(parent);
        requireVersion(parent, value.getExpectedVersion());
        ProductStateBO editable = lifecycle.evaluate(ProductActionEnum.EDIT, state(parent, false, true));
        if (!Objects.equals(parent.getSn(), value.getSpu().getSn())) {
            throw stateConflict();
        }
        Map<Long, SkuPO> current = new HashMap<>();
        skuRepository.selectBySpuLifecycle(parent.getId(), false).forEach(row -> current.put(row.getId(), row));
        List<SkuPO> written = new ArrayList<>();
        for (SkuBO child : value.getSkuList()) {
            prepareSku(child, value.getSpu(), parent.getId(), references);
            if (child.getId() == null) {
                child.setSaleNum(0).setCommentNum(0);
                SkuPO row = skuConverter.toSource(child);
                if (!skuRepository.save(row)) {
                    throw versionConflict();
                }
                written.add(row);
            } else {
                SkuPO row = current.remove(child.getId());
                if (row == null || !Objects.equals(row.getSn(), child.getSn())
                        || !Objects.equals(row.getSpuId(), parent.getId()) || !Objects.equals(row.getTenantId(), parent.getTenantId())) {
                    throw stateConflict();
                }
                skuConverter.mergeForUpdate(child, row);
                if (!skuRepository.updateById(row)) {
                    throw versionConflict();
                }
                written.add(row);
            }
        }
        for (SkuPO removed : current.values()) {
            removed.setDeletionKind("E");
            if (!skuRepository.removeById(removed)) {
                throw versionConflict();
            }
        }
        ensureAssociation(references);
        value.getSpu().setStatus(editable.getStatus()).setIsMarketable(editable.getMarketable());
        spuConverter.mergeForUpdate(value.getSpu(), parent);
        if (!spuRepository.updateById(parent)) {
            throw versionConflict();
        }
        written.sort(java.util.Comparator.comparing(SkuPO::getId));
        log.info("updated product id={} version={}", parent.getId(), parent.getVersion());
        return aggregate(parent, written);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    @Validated(ProductValidationGroup.Detail.class)
    public ProductBO detail(ProductDomainQuery value) {
        validation.validate(value, ProductValidationGroup.Detail.class);
        SpuPO parent = spuRepository.selectByLifecycle(value);
        requireParent(parent);
        return aggregate(parent, skuRepository.selectBySpuLifecycle(parent.getId(), value.getDeleted()));
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    @Validated(ProductValidationGroup.Page.class)
    public PageResultRecord<SpuBO> page(ProductDomainQuery value) {
        validation.validate(value, ProductValidationGroup.Page.class);
        long count = spuRepository.countPage(value);
        return PageResultRecord.success(spuConverter.toTargetList(spuRepository.selectPage(value)), count,
                value.getPage().pageNo(), value.getPage().pageSize());
    }

    private CatalogReferenceBO references(SpuBO value) {
        CatalogReferenceBO result = spuRepository.loadCatalogReferences(value);
        if (result == null) {
            throw stateConflict();
        }
        return result;
    }

    private void ensureAssociation(CatalogReferenceBO value) {
        if (categoryBrandRepository.selectActiveByEndpoints(value.getCategory3Id(), value.getBrandId()) == null
                && !categoryBrandRepository.save(CategoryBrandPO.builder()
                        .categoryId(value.getCategory3Id()).brandId(value.getBrandId()).build())) {
            throw versionConflict();
        }
    }

    private static void validateCatalog(SpuBO value, CatalogReferenceBO references) {
        Map<String, List<String>> allowed = new LinkedHashMap<>();
        for (SpecBO definition : references.getOrderedSpecs()) {
            if (allowed.putIfAbsent(definition.getName(), definition.getOptions()) != null) {
                throw stateConflict();
            }
        }
        for (Map.Entry<String, List<String>> entry : value.getSpecItems().entrySet()) {
            if (!allowed.containsKey(entry.getKey()) || !allowed.get(entry.getKey()).containsAll(entry.getValue())) {
                throw stateConflict();
            }
        }
        for (Map.Entry<String, String> entry : value.getParaItems().entrySet()) {
            List<String> options = references.getParaOptions().get(entry.getKey());
            if (options == null || (!options.isEmpty() && !options.contains(entry.getValue()))) {
                throw stateConflict();
            }
        }
    }

    private static void prepareSku(SkuBO child, SpuBO parent, Long parentId, CatalogReferenceBO references) {
        String suffix = references.getOrderedSpecs().stream().map(SpecBO::getName)
                .map(child.getSpec()::get).filter(Objects::nonNull).collect(Collectors.joining(" "));
        child.setName(parent.getName() + (suffix.isEmpty() ? "" : " " + suffix))
                .setSpuId(parentId).setCategoryId(references.getCategory3Id())
                .setCategoryName(references.getCategoryName()).setBrandName(references.getBrandName());
    }

    private ProductBO aggregate(SpuPO parent, List<SkuPO> children) {
        return ProductBO.builder().spu(spuConverter.toTarget(parent)).skuList(skuConverter.toTargetList(children)).build();
    }

    private static ProductDomainQuery query(Long id, boolean deleted) {
        return ProductDomainQuery.builder().id(id).deleted(deleted).build();
    }

    private static void requireParent(SpuPO parent) {
        if (parent == null) {
            throw new CommonException(610202, "PRODUCT_NOT_FOUND", "Product does not exist or is not visible");
        }
    }

    private static void requireVersion(SpuPO parent, Long expected) {
        if (!Objects.equals(parent.getVersion(), expected)) {
            throw versionConflict();
        }
    }

    private static CommonException versionConflict() {
        return new CommonException(610203, "PRODUCT_VERSION_CONFLICT", "Product version changed");
    }

    private static CommonException stateConflict() {
        return new CommonException(610204, "PRODUCT_STATE_CONFLICT", "Product state or catalog does not allow this operation");
    }

    @Override
    @Transactional
    public ProductStateBO audit(Long id, Long version) {
        return changeSalesState(id, version, ProductActionEnum.AUDIT, true);
    }

    @Override
    @Transactional
    public ProductStateBO pull(Long id, Long version) {
        return changeSalesState(id, version, ProductActionEnum.PULL, false);
    }

    @Override
    @Transactional
    public ProductStateBO put(Long id, Long version) {
        return changeSalesState(id, version, ProductActionEnum.PUT, true);
    }

    private ProductStateBO changeSalesState(Long id, Long version, ProductActionEnum action, boolean checkReferences) {
        if (checkReferences) {
            SpuPO snapshot = spuRepository.selectByLifecycle(query(id, false));
            requireParent(snapshot);
            SpuBO value = spuConverter.toTarget(snapshot);
            validateCatalog(value, references(value));
        }
        SpuPO parent = spuRepository.lockByLifecycle(query(id, false));
        requireParent(parent);
        requireVersion(parent, version);
        boolean activeSku = skuRepository.selectBySpuLifecycle(id, false).stream().anyMatch(row -> "1".equals(row.getStatus()));
        ProductStateBO target = lifecycle.evaluate(action, state(parent, activeSku, true));
        if (!Objects.equals(parent.getStatus(), target.getStatus()) || !Objects.equals(parent.getIsMarketable(), target.getMarketable())) {
            parent.setStatus(target.getStatus()).setIsMarketable(target.getMarketable());
            if (!spuRepository.updateById(parent)) {
                throw versionConflict();
            }
        }
        return state(parent, activeSku, true);
    }

    @Override
    @Transactional
    public boolean delete(Long id, Long version) {
        SpuPO parent = lockIncludingDeleted(id);
        requireParent(parent);
        requireVersion(parent, version);
        lifecycle.evaluate(ProductActionEnum.DELETE, state(parent, false, true));
        if (parent.getDeletedAt() != null) {
            return true;
        }
        for (SkuPO child : skuRepository.selectBySpuLifecycle(id, false)) {
            child.setDeletionKind("P");
            if (!skuRepository.removeById(child)) {
                throw versionConflict();
            }
        }
        if (!spuRepository.removeById(parent)) {
            throw versionConflict();
        }
        return true;
    }

    @Override
    @Transactional
    public ProductStateBO restore(Long id, Long version) {
        SpuPO parent = lockIncludingDeleted(id);
        requireParent(parent);
        requireVersion(parent, version);
        lifecycle.evaluate(ProductActionEnum.RESTORE, state(parent, false, true));
        List<SkuPO> children = skuRepository.selectBySpuLifecycle(id, true);
        if (!spuRepository.restoreById(parent)) {
            throw versionConflict();
        }
        for (SkuPO child : children) {
            if (!skuRepository.restoreById(child)) {
                throw versionConflict();
            }
            child.setDeletionKind(null);
        }
        return state(parent, children.stream().anyMatch(row -> "1".equals(row.getStatus())), true);
    }

    @Override
    @Transactional
    public boolean purge(Long id, Long version) {
        SpuPO parent = lockIncludingDeleted(id);
        if (parent == null) {
            return true;
        }
        requireVersion(parent, version);
        lifecycle.evaluate(ProductActionEnum.PURGE, state(parent, false, true));
        int count = skuRepository.selectAllBySpuForPurge(id).size();
        if (skuRepository.purgeAllBySpu(id, parent.getVersion(), parent.getDeletedAt()) != count
                || spuRepository.purgeDeletedById(id, parent.getVersion(), parent.getDeletedAt()) != 1) {
            throw versionConflict();
        }
        return true;
    }

    private SpuPO lockIncludingDeleted(Long id) {
        SpuPO parent = spuRepository.lockByLifecycle(query(id, false));
        return parent == null ? spuRepository.lockByLifecycle(query(id, true)) : parent;
    }

    private static ProductStateBO state(SpuPO value, boolean activeSku, boolean referencesValid) {
        return ProductStateBO.builder().id(value.getId()).version(value.getVersion()).status(value.getStatus())
                .marketable(value.getIsMarketable()).deleted(value.getDeletedAt() != null)
                .hasActiveSku(activeSku).referencesValid(referencesValid).build();
    }
}
