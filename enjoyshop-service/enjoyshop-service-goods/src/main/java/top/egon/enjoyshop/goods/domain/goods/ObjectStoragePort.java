package top.egon.enjoyshop.goods.domain.goods;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public interface ObjectStoragePort {

    String putObject(@NotNull @Size(min = 1, max = 10 * 1024 * 1024) byte[] content,
                     @NotNull ProductImageFormat format);
}
