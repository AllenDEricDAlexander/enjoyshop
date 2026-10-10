package top.egon.enjoyshop.goods.application.ad;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.enjoyshop.goods.domain.ad.AdBO;
import top.egon.enjoyshop.goods.domain.ad.AdDomainQuery;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        builder = @org.mapstruct.Builder(disableBuilder = true))
public interface AdApplicationConverter extends BaseForwardConverter<AdSaveCommand, AdBO> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    AdBO toTarget(AdSaveCommand source);

    @Mapping(target = "id", source = "adId")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    AdBO toReplaceTarget(AdSaveCommand source, Long adId);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "position", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "targetUrl", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "startAt", ignore = true)
    @Mapping(target = "endAt", ignore = true)
    @Mapping(target = "sortOrder", ignore = true)
    AdBO toDeleteTarget(AdLifecycleCommand source);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "page", expression = "java(new top.egon.cola.component.common.core.pojo.PageQuery(source.getPageNo(), source.getPageSize()))")
    @Mapping(target = "limit", ignore = true)
    AdDomainQuery toDomainQuery(AdPageQuery source);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "page", ignore = true)
    AdDomainQuery toDomainQuery(AdReadQuery source);

    @Mapping(target = "id", source = "adId")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "position", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "page", ignore = true)
    @Mapping(target = "limit", ignore = true)
    AdDomainQuery toDetailQuery(Long adId);
}
