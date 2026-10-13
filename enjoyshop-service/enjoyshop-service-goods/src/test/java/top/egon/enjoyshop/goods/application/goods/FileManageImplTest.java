package top.egon.enjoyshop.goods.application.goods;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.enjoyshop.goods.application.goods.converter.FileUploadDomainConverter;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.domain.goods.FileDomainService;
import top.egon.enjoyshop.goods.domain.goods.ProductImageBO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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

    @Mock
    private FileAuthorizationComponent authorization;

    @Mock
    private ValidationUtils validation;

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
    void denialDoesNotCallDomain() {
        lenient().doThrow(new CommonException(GoodsErrorStatus.FILE_FORBIDDEN))
                .when(authorization).requirePermission("enjoyshop:file:write");

        assertThatThrownBy(() -> fileManage.upload(new FileUploadCommand().setContent(new byte[0])))
                .isInstanceOf(CommonException.class);

        verifyNoInteractions(fileDomainService, converter);
    }

    @Test
    void allowValidatesThenCallsDomain() {
        when(fileDomainService.upload(any(ProductImageBO.class))).thenReturn(STABLE_URL);

        fileManage.upload(FileUploadCommand.builder().content(new byte[]{1}).build());

        InOrder order = inOrder(authorization, validation, fileDomainService);
        order.verify(authorization).requirePermission("enjoyshop:file:write");
        order.verify(validation).validate(any(FileUploadCommand.class));
        order.verify(fileDomainService).upload(any(ProductImageBO.class));
    }

    @Test
    void application_validation_rejects_empty_command_before_domain_call() {
        lenient().doThrow(new CommonException(GoodsErrorStatus.FILE_FORBIDDEN))
                .when(authorization).requirePermission("enjoyshop:file:write");
        assertThatThrownBy(() -> fileManage.upload(new FileUploadCommand().setContent(new byte[0])))
                .isInstanceOf(CommonException.class);
        verifyNoInteractions(fileDomainService, converter);

        ValidationUtils realValidation = new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());
        FileAuthorizationComponent allowing = mock(FileAuthorizationComponent.class);
        FileManageImpl allowed = new FileManageImpl(fileDomainService, converter, allowing, realValidation);
        assertThatThrownBy(() -> allowed.upload(new FileUploadCommand().setContent(new byte[0])))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(fileDomainService);
    }
}
