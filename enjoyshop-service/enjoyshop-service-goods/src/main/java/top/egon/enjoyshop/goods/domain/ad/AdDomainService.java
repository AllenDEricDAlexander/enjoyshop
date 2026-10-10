package top.egon.enjoyshop.goods.domain.ad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import java.util.List;

public interface AdDomainService {

    AdBO create(@Valid @NotNull AdBO value);

    AdBO replace(@Valid @NotNull AdBO value);

    boolean delete(@Valid @NotNull AdBO value);

    AdBO detail(@Valid @NotNull AdDomainQuery query);

    PageResultRecord<AdBO> page(@Valid @NotNull AdDomainQuery query);

    List<AdBO> publishedCandidates(@Valid @NotNull AdDomainQuery query);
}
