package top.egon.enjoyshop.goods.application.ad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageResultRecord;

@Validated
public interface AdManage {

    AdResult create(@Valid AdSaveCommand command);

    AdResult replace(@NotNull @Positive Long adId, @Valid AdSaveCommand command);

    PageResultRecord<AdResult> page(@Valid AdPageQuery query);

    AdResult detail(@NotNull @Positive Long adId);

    boolean delete(@Valid AdLifecycleCommand command);

    List<AdPublicResult> published(@Valid AdReadQuery query);
}
