package top.egon.enjoyshop.goods.application.goods;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface FileManage {

    String upload(@Valid @NotNull FileUploadCommand command);
}
