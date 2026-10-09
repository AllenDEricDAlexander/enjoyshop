package top.egon.enjoyshop.goods.adapter.goods;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration(value = "productOpenApiConfiguration", proxyBeanMethods = false)
@Slf4j
public class ProductOpenApiConfiguration {
    @Bean("productOpenApiCustomizer")
    OpenApiCustomizer productOpenApiCustomizer() { return ProductOpenApiConfiguration::customize; }

    private static void customize(io.swagger.v3.oas.models.OpenAPI api) {
        io.swagger.v3.oas.models.Components components = api.getComponents();
        if (components == null) { components = new io.swagger.v3.oas.models.Components(); api.setComponents(components); }
        components.addSecuritySchemes("bearerAuth", new io.swagger.v3.oas.models.security.SecurityScheme()
                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));
        var validation = io.swagger.v3.core.converter.ModelConverters.getInstance().resolveAsResolvedSchema(
                new io.swagger.v3.core.converter.AnnotatedType(new com.fasterxml.jackson.core.type.TypeReference<
                        top.egon.cola.component.common.core.pojo.ResultRecord<top.egon.enjoyshop.goods.adapter.goods.pojo.vo.FieldErrorsVO>>() { }.getType()).resolveAsRef(true));
        validation.referencedSchemas.forEach(components::addSchemas);
        components.addSchemas("ProductRequestError", validation.schema);
        var failure = io.swagger.v3.core.converter.ModelConverters.getInstance().resolveAsResolvedSchema(
                new io.swagger.v3.core.converter.AnnotatedType(new com.fasterxml.jackson.core.type.TypeReference<
                        top.egon.cola.component.common.core.pojo.ResultRecord<Void>>() { }.getType()).resolveAsRef(true));
        failure.referencedSchemas.forEach(components::addSchemas);
        components.addSchemas("ProductBusinessError", failure.schema);
        components.addSchemas("ProductFilterError", new io.swagger.v3.oas.models.media.ObjectSchema()
                .addProperty("code", new io.swagger.v3.oas.models.media.StringSchema())
                .addProperty("message", new io.swagger.v3.oas.models.media.StringSchema())
                .required(java.util.List.of("code", "message")));
        components.addSchemas("ProductAuthorizationError", new io.swagger.v3.oas.models.media.ComposedSchema()
                .addOneOfItem(new io.swagger.v3.oas.models.media.Schema<>().$ref("#/components/schemas/ProductBusinessError"))
                .addOneOfItem(new io.swagger.v3.oas.models.media.Schema<>().$ref("#/components/schemas/ProductFilterError")));

        productInputSchemas(components);
    }

    /** Project the same DTO fields under their native Create/Update validation groups. */
    private static void productInputSchemas(io.swagger.v3.oas.models.Components components) {
        var save = io.swagger.v3.core.converter.ModelConverters.getInstance().resolveAsResolvedSchema(
                new io.swagger.v3.core.converter.AnnotatedType(top.egon.enjoyshop.goods.application.goods.ProductSaveCommand.class).resolveAsRef(true));
        save.referencedSchemas.forEach(components::addSchemas);
        var root = components.getSchemas().get("ProductSaveCommand");
        var sku = components.getSchemas().get("SkuSaveCommand");
        var createSku = new io.swagger.v3.oas.models.media.ObjectSchema();
        createSku.setProperties(new java.util.LinkedHashMap<>(sku.getProperties()));
        createSku.setRequired(sku.getRequired() == null ? java.util.List.of() : new java.util.ArrayList<>(sku.getRequired()));
        createSku.addProperty("id", new io.swagger.v3.oas.models.media.Schema<>().type("null"));
        createSku.setAdditionalProperties(false);
        components.addSchemas("ProductCreateSkuInput", createSku);
        var create = new io.swagger.v3.oas.models.media.ObjectSchema();
        create.setProperties(new java.util.LinkedHashMap<>(root.getProperties()));
        create.setRequired(java.util.List.of("spu", "skuList"));
        create.addProperty("version", new io.swagger.v3.oas.models.media.Schema<>().type("null"));
        create.addProperty("skuList", new io.swagger.v3.oas.models.media.ArraySchema().minItems(1).maxItems(100)
                .items(new io.swagger.v3.oas.models.media.Schema<>().$ref("#/components/schemas/ProductCreateSkuInput")));
        create.setAdditionalProperties(false);
        components.addSchemas("ProductCreateInput", create);
        var update = new io.swagger.v3.oas.models.media.ObjectSchema();
        update.setProperties(new java.util.LinkedHashMap<>(root.getProperties()));
        update.setRequired(java.util.List.of("spu", "skuList", "version"));
        update.addProperty("version", new io.swagger.v3.oas.models.media.IntegerSchema().format("int64").minimum(java.math.BigDecimal.ZERO));
        update.setAdditionalProperties(false);
        components.addSchemas("ProductUpdateInput", update);
    }

}
