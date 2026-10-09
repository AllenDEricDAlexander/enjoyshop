package top.egon.enjoyshop.goods.common.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import top.egon.enjoyshop.goods.application.goods.ProductSaveCommand;
import top.egon.enjoyshop.goods.application.goods.SkuSaveCommand;
import top.egon.enjoyshop.goods.application.goods.SpuSaveCommand;

class ProductCompositionValidationTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @AfterAll
    static void closeValidator() {
        FACTORY.close();
    }

    @Test
    void disabledSpecificationsRequireOneSkuWithAnEmptySpecification() {
        ProductSaveCommand command = validProduct().setSkuList(List.of(sku("A"), sku("B")));
        assertThat(VALIDATOR.validate(command, ProductValidationGroup.Create.class)).isNotEmpty();
        assertThat(VALIDATOR.validate(validProduct(), ProductValidationGroup.Create.class)).isEmpty();
    }

    @Test
    void enabledSpecificationsRejectDuplicateCombinationsAndUnadvertisedValues() {
        ProductSaveCommand command = validProduct();
        command.getSpu().setIsEnableSpec(true).setSpecItems(Map.of("颜色", List.of("红", "蓝")));
        command.setSkuList(List.of(sku("A").setSpec(Map.of("颜色", "红")),
                sku("B").setSpec(Map.of("颜色", "红"))));
        assertThat(VALIDATOR.validate(command, ProductValidationGroup.Create.class)).isNotEmpty();
        command.setSkuList(List.of(sku("A").setSpec(Map.of("颜色", "黑"))));
        assertThat(VALIDATOR.validate(command, ProductValidationGroup.Create.class)).isNotEmpty();
        command.setSkuList(List.of(sku("A").setSpec(Map.of("颜色", "蓝"))));
        assertThat(VALIDATOR.validate(command, ProductValidationGroup.Create.class)).isEmpty();
    }

    @Test
    void duplicateBarcodesAndExistingSkuIdsAreRejected() {
        ProductSaveCommand command = validProduct();
        command.getSpu().setIsEnableSpec(true).setSpecItems(Map.of("颜色", List.of("红", "蓝")));
        command.setSkuList(List.of(sku("A").setSpec(Map.of("颜色", "红")),
                sku("A").setSpec(Map.of("颜色", "蓝"))));
        assertThat(VALIDATOR.validate(command, ProductValidationGroup.Create.class)).isNotEmpty();
        command.setVersion(0L).setSkuList(List.of(sku("A").setId(1L).setSpec(Map.of("颜色", "红")),
                sku("B").setId(1L).setSpec(Map.of("颜色", "蓝"))));
        assertThat(VALIDATOR.validate(command, ProductValidationGroup.Update.class)).isNotEmpty();
    }

    @Test
    void createAndUpdateUseDistinctVersionAndIdentityGroups() {
        ProductSaveCommand command = validProduct();
        assertThat(VALIDATOR.validate(command, ProductValidationGroup.Create.class)).isEmpty();
        assertThat(VALIDATOR.validate(command, ProductValidationGroup.Update.class))
                .extracting(value -> value.getPropertyPath().toString()).contains("version");
        command.setVersion(0L);
        assertThat(VALIDATOR.validate(command, ProductValidationGroup.Create.class))
                .extracting(value -> value.getPropertyPath().toString()).contains("version");
        command.setVersion(null);
        command.getSkuList().getFirst().setId(1L);
        assertThat(VALIDATOR.validate(command, ProductValidationGroup.Create.class))
                .extracting(value -> value.getPropertyPath().toString()).contains("skuList[0].id");
    }

    @Test
    void identifiersAndQuantityTokensAreStrictWithoutGlobalJacksonChanges() throws Exception {
        assertThatThrownBy(() -> MAPPER.readValue("{\"id\":9007199254740993}", SkuSaveCommand.class))
                .isInstanceOf(JsonProcessingException.class);
        assertThatThrownBy(() -> MAPPER.readValue("{\"id\":\"01\"}", SkuSaveCommand.class))
                .isInstanceOf(JsonProcessingException.class);
        assertThatThrownBy(() -> MAPPER.readValue("{\"num\":1.5}", SkuSaveCommand.class))
                .isInstanceOf(JsonProcessingException.class);
        assertThatThrownBy(() -> MAPPER.readValue("{\"price\":\"1\"}", SkuSaveCommand.class))
                .isInstanceOf(JsonProcessingException.class);
        assertThat(MAPPER.readValue("{\"id\":\"9007199254740993\"}", SkuSaveCommand.class).getId())
                .isEqualTo(9007199254740993L);
    }

    @Test
    void unknownFieldsCannotOverrideBackendOwnedFields() {
        assertThatThrownBy(() -> MAPPER.readValue("{\"tenantId\":\"2\"}", SpuSaveCommand.class))
                .isInstanceOf(JsonProcessingException.class);
        assertThatThrownBy(() -> MAPPER.readValue("{\"status\":\"1\"}", ProductSaveCommand.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    private static ProductSaveCommand validProduct() {
        return ProductSaveCommand.builder().spu(SpuSaveCommand.builder().sn("SPU-A").name("手机")
                        .brandId(1L).category1Id(2L).category2Id(3L).category3Id(4L).templateId(5L)
                        .isEnableSpec(false).build())
                .skuList(List.of(sku("A"))).build();
    }

    private static SkuSaveCommand sku(String barcode) {
        return SkuSaveCommand.builder().sn(barcode).price(1).num(0).alertNum(0).weight(0).build();
    }
}
