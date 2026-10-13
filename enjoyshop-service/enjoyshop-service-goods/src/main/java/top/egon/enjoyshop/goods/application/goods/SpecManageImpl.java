package top.egon.enjoyshop.goods.application.goods;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.enjoyshop.goods.application.goods.converter.SpecManageConverter;
import top.egon.enjoyshop.goods.application.goods.converter.SpecResultConverter;
import top.egon.enjoyshop.goods.domain.goods.SpecDomainService;

@Service("specManage")
@Validated
@Slf4j
@RequiredArgsConstructor
public class SpecManageImpl implements SpecManage {

    @Qualifier("specDomainService")
    private final SpecDomainService domainService;

    @Qualifier("specManageConverterImpl")
    private final SpecManageConverter manageConverter;

    @Qualifier("specResultConverterImpl")
    private final SpecResultConverter resultConverter;

    @Qualifier("specAuthorizationComponent")
    private final SpecAuthorizationComponent authorization;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validation;

    @Override
    @Transactional(readOnly = true)
    public List<SpecResult> listSpecsByCategoryName(SpecListQuery query) {
        authorization.requirePermission("enjoyshop:spec:read");
        validation.validate(query);
        return resultConverter.toTargetList(
                domainService.listByCategoryName(manageConverter.toTarget(query)));
    }
}
