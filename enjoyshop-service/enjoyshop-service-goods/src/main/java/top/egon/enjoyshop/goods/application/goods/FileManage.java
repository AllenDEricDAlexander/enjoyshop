package top.egon.enjoyshop.goods.application.goods;

import org.springframework.validation.annotation.Validated;

@Validated
public interface FileManage {

    String upload(FileUploadCommand command);
}
