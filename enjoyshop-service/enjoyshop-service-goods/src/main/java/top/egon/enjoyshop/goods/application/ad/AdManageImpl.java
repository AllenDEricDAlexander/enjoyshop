package top.egon.enjoyshop.goods.application.ad;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.enjoyshop.goods.common.validation.AdValidationGroup;
import top.egon.enjoyshop.goods.domain.ad.AdBO;
import top.egon.enjoyshop.goods.domain.ad.AdDomainQuery;
import top.egon.enjoyshop.goods.domain.ad.AdDomainService;
import top.egon.enjoyshop.goods.domain.ad.AdEligibilitySpecification;

@Service("adManage")
@Validated
@Slf4j
@RequiredArgsConstructor
public class AdManageImpl implements AdManage {

    @Qualifier("adAuthorizationComponent")
    private final AdAuthorizationComponent authorization;

    @Qualifier("adDomainService")
    private final AdDomainService domainService;

    @Qualifier("adApplicationConverterImpl")
    private final AdApplicationConverter applicationConverter;

    @Qualifier("adResultConverterImpl")
    private final AdResultConverter resultConverter;

    @Qualifier("adPublicResultConverterImpl")
    private final AdPublicResultConverter publicResultConverter;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validation;

    @Qualifier("adClock")
    private final Clock clock;

    @Qualifier("adEligibilitySpecification")
    private final AdEligibilitySpecification eligibility;

    @Override
    public AdResult create(AdSaveCommand command) {
        authorization.requirePermission("enjoyshop:ad:write");
        trimName(command);
        validation.validate(command, AdValidationGroup.Create.class);
        return resultConverter.toTarget(domainService.create(applicationConverter.toTarget(command)));
    }

    @Override
    public AdResult replace(Long adId, AdSaveCommand command) {
        authorization.requirePermission("enjoyshop:ad:write");
        trimName(command);
        validation.validate(command, AdValidationGroup.Update.class);
        return resultConverter.toTarget(domainService.replace(applicationConverter.toReplaceTarget(command, adId)));
    }

    @Override
    public PageResultRecord<AdResult> page(AdPageQuery query) {
        authorization.requirePermission("enjoyshop:ad:read");
        validation.validate(query, AdValidationGroup.Page.class);
        AdDomainQuery domainQuery = applicationConverter.toDomainQuery(query);
        PageResultRecord<AdBO> page = domainService.page(domainQuery);
        return PageResultRecord.success(page.records().stream().map(resultConverter::toTarget).toList(),
                page.page().total(), page.page().pageNo(), page.page().pageSize());
    }

    @Override
    public AdResult detail(Long adId) {
        authorization.requirePermission("enjoyshop:ad:read");
        AdDomainQuery query = applicationConverter.toDetailQuery(adId);
        validation.validate(query, AdValidationGroup.Detail.class);
        return resultConverter.toTarget(domainService.detail(query));
    }

    @Override
    public boolean delete(AdLifecycleCommand command) {
        authorization.requirePermission("enjoyshop:ad:write");
        validation.validate(command, AdValidationGroup.Delete.class);
        return domainService.delete(applicationConverter.toDeleteTarget(command));
    }

    @Override
    public List<AdPublicResult> published(AdReadQuery query) {
        validation.validate(query, AdValidationGroup.PublicRead.class);
        AdDomainQuery domainQuery = applicationConverter.toDomainQuery(query);
        Instant now = clock.instant();
        return domainService.publishedCandidates(domainQuery).stream()
                .filter(ad -> eligibility.isEligible(ad, now))
                .limit(query.getLimit())
                .map(publicResultConverter::toTarget)
                .toList();
    }

    private static void trimName(AdSaveCommand command) {
        if (command != null && command.getName() != null) {
            command.setName(command.getName().trim());
        }
    }
}
