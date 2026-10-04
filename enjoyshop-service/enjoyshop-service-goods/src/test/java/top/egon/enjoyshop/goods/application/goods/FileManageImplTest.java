package top.egon.enjoyshop.goods.application.goods;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.enjoyshop.goods.application.goods.converter.FileUploadDomainConverter;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.domain.goods.FileDomainService;
import top.egon.enjoyshop.goods.domain.goods.ProductImageBO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileManageImplTest {

    private static final String STABLE_URL = "https://images.example.test/product-media/opaque-key.png";

    @Mock
    private FileDomainService fileDomainService;

    @Spy
    private FileUploadDomainConverter converter = Mappers.getMapper(FileUploadDomainConverter.class);

    @InjectMocks
    private FileManageImpl fileManage;

    @Test
    void maps_command_bytes_defensively_and_returns_domain_acknowledgement() {
        byte[] source = {12, 34, 56};
        FileUploadCommand command = FileUploadCommand.builder().content(source).build();
        source[0] = 0;
        when(fileDomainService.upload(any(ProductImageBO.class))).thenReturn(STABLE_URL);

        assertThat(fileManage.upload(command)).isEqualTo(STABLE_URL);

        ArgumentCaptor<ProductImageBO> imageCaptor = ArgumentCaptor.forClass(ProductImageBO.class);
        verify(converter).toTarget(command);
        verify(fileDomainService).upload(imageCaptor.capture());
        assertThat(imageCaptor.getValue().getContent()).containsExactly(12, 34, 56);
    }

    @Test
    void propagates_storage_failure_without_returning_a_url() {
        CommonException unavailable = new CommonException(GoodsErrorStatus.FILE_STORAGE_UNAVAILABLE);
        when(fileDomainService.upload(any(ProductImageBO.class))).thenThrow(unavailable);

        assertThatThrownBy(() -> fileManage.upload(FileUploadCommand.builder().content(new byte[]{1}).build()))
                .isSameAs(unavailable);
    }

    @Test
    void application_validation_rejects_empty_command_before_domain_call() {
        ProxyFactory proxyFactory = new ProxyFactory(fileManage);
        proxyFactory.addAdvice(new MethodValidationInterceptor());
        FileManage validatedManage = (FileManage) proxyFactory.getProxy();

        assertThatThrownBy(() -> validatedManage.upload(new FileUploadCommand().setContent(new byte[0])))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);

        verifyNoInteractions(fileDomainService, converter);
    }
}
