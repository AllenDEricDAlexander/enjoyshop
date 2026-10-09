package top.egon.enjoyshop.goods.application.goods;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.enjoyshop.goods.common.validation.ProductValidationGroup;
import top.egon.enjoyshop.goods.domain.goods.ProductDomainService;

@Service("productManage")
@Validated
@Slf4j
@RequiredArgsConstructor
public class ProductManageImpl implements ProductManage {
    @Qualifier("productAuthorizationComponent") private final ProductAuthorizationComponent authorization;
    @Qualifier("productDomainService") private final ProductDomainService domain;
    @Qualifier("productApplicationConverterImpl") private final ProductApplicationConverter commands;
    @Qualifier("productQueryConverterImpl") private final ProductQueryConverter results;
    @Qualifier("productSpuResultConverterImpl") private final ProductSpuResultConverter summaries;
    @Qualifier("egonColaValidationUtils") private final ValidationUtils validation;

    @Override
    public ProductResult create(ProductSaveCommand command) {
        authorization.requirePermission("enjoyshop:product:write");
        validation.validate(command, ProductValidationGroup.Create.class);
        return results.toTarget(domain.create(commands.toTarget(command)));
    }

    @Override
    public ProductResult update(Long spuId, ProductSaveCommand command) {
        authorization.requirePermission("enjoyshop:product:write");
        validation.validate(command, ProductValidationGroup.Update.class);
        return results.toTarget(domain.update(commands.toUpdateTarget(command, spuId)));
    }

    @Override
    public ProductResult detail(ProductIdQuery query) {
        authorization.requirePermission("enjoyshop:product:read");
        validation.validate(query, ProductValidationGroup.Detail.class);
        return results.toTarget(domain.detail(commands.toDomainQuery(query)));
    }

    @Override
    public PageResultRecord<ProductSummaryResult> page(ProductPageQuery query) {
        authorization.requirePermission("enjoyshop:product:read");
        validation.validate(query, ProductValidationGroup.Page.class);
        var page = domain.page(commands.toDomainQuery(query));
        return PageResultRecord.success(page.records().stream().map(summaries::toSummary).toList(),
                page.page().total(), page.page().pageNo(), page.page().pageSize());
    }

    @Override
    public ProductStateResult audit(ProductLifecycleCommand command) {
        authorization.requirePermission("enjoyshop:product:audit");
        validation.validate(command, ProductValidationGroup.StateCall.class);
        return results.toState(domain.audit(command.getSpuId(), command.getVersion()));
    }

    @Override
    public ProductStateResult pull(ProductLifecycleCommand command) {
        authorization.requirePermission("enjoyshop:product:write");
        validation.validate(command, ProductValidationGroup.StateCall.class);
        return results.toState(domain.pull(command.getSpuId(), command.getVersion()));
    }

    @Override
    public ProductStateResult put(ProductLifecycleCommand command) {
        authorization.requirePermission("enjoyshop:product:write");
        validation.validate(command, ProductValidationGroup.StateCall.class);
        return results.toState(domain.put(command.getSpuId(), command.getVersion()));
    }

    @Override
    public boolean delete(ProductLifecycleCommand command) {
        authorization.requirePermission("enjoyshop:product:write");
        validation.validate(command, ProductValidationGroup.StateCall.class);
        return domain.delete(command.getSpuId(), command.getVersion());
    }

    @Override
    public ProductStateResult restore(ProductLifecycleCommand command) {
        authorization.requirePermission("enjoyshop:product:write");
        validation.validate(command, ProductValidationGroup.StateCall.class);
        return results.toState(domain.restore(command.getSpuId(), command.getVersion()));
    }

    @Override
    public boolean purge(ProductLifecycleCommand command) {
        authorization.requirePermission("enjoyshop:product:purge");
        validation.validate(command, ProductValidationGroup.StateCall.class);
        return domain.purge(command.getSpuId(), command.getVersion());
    }
}
