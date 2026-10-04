package top.egon.enjoyshop.goods.domain.goods;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface FileDomainService {

    String upload(@Valid @NotNull ProductImageBO image);
}
