package top.egon.enjoyshop.goods.adapter.goods.pojo.convertor;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.enjoyshop.goods.adapter.goods.pojo.dto.FileUploadDTO;
import top.egon.enjoyshop.goods.application.goods.FileUploadCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadAdapterConverterTest {

    private final FileUploadAdapterConverter converter = Mappers.getMapper(FileUploadAdapterConverter.class);

    @Test
    void maps_exact_file_bytes_without_forwarding_caller_metadata() {
        byte[] bytes = {1, 2, 3, 4};
        MockMultipartFile file = new MockMultipartFile("file", "caller-name.png", "text/plain", bytes);

        FileUploadCommand command = converter.toTarget(new FileUploadDTO().setFile(file));

        assertThat(command.getContent()).containsExactly(bytes);
    }

    @Test
    void unreadable_multipart_data_becomes_a_safe_request_error() {
        MultipartFile unreadable = new MockMultipartFile("file", "private-name.png", "image/png", new byte[]{1}) {
            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException("private multipart details");
            }
        };

        assertThatThrownBy(() -> converter.toTarget(new FileUploadDTO().setFile(unreadable)))
                .isInstanceOfSatisfying(CommonException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(610106));
    }
}
