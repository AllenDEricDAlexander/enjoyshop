package top.egon.enjoyshop.goods.adapter.goods;

import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.enjoyshop.goods.adapter.goods.pojo.vo.FieldErrorsVO;
import top.egon.enjoyshop.goods.application.ad.AdSaveCommand;

@Configuration(value = "adOpenApiConfiguration", proxyBeanMethods = false)
@Slf4j
public class AdOpenApiConfiguration {

    @Bean("adsGroupedOpenApi")
    GroupedOpenApi adsGroupedOpenApi(@Qualifier("adOpenApiCustomizer") OpenApiCustomizer customizer) {
        return GroupedOpenApi.builder()
                .group("ads")
                .pathsToMatch("/api/v1/ads", "/api/v1/ads/**", "/api/v1/ad-slots/*/ads")
                .addOpenApiCustomizer(customizer)
                .build();
    }

    @Bean("adOpenApiCustomizer")
    OpenApiCustomizer adOpenApiCustomizer() {
        return AdOpenApiConfiguration::customize;
    }

    private static void customize(OpenAPI api) {
        var components = api.getComponents();
        if (components == null) {
            components = new io.swagger.v3.oas.models.Components();
            api.setComponents(components);
        }
        var validation = ModelConverters.getInstance().resolveAsResolvedSchema(
                new AnnotatedType(new TypeReference<ResultRecord<FieldErrorsVO>>() { }.getType()).resolveAsRef(true));
        validation.referencedSchemas.forEach(components::addSchemas);
        components.addSchemas("AdRequestError", validation.schema);
        var failure = ModelConverters.getInstance().resolveAsResolvedSchema(
                new AnnotatedType(new TypeReference<ResultRecord<Void>>() { }.getType()).resolveAsRef(true));
        failure.referencedSchemas.forEach(components::addSchemas);
        components.addSchemas("AdBusinessError", failure.schema);
        components.addSchemas("AdFilterError", new ObjectSchema()
                .addProperty("code", new StringSchema())
                .addProperty("message", new StringSchema())
                .required(List.of("code", "message")));
        components.addSchemas("AdAuthorizationError", new ComposedSchema()
                .addOneOfItem(new Schema<>().$ref("#/components/schemas/AdBusinessError"))
                .addOneOfItem(new Schema<>().$ref("#/components/schemas/AdFilterError")));
        adInputSchemas(components);
        useOperationInputSchemas(api);
        setPublicOperationAnonymous(api);
    }

    private static void adInputSchemas(io.swagger.v3.oas.models.Components components) {
        var save = ModelConverters.getInstance().resolveAsResolvedSchema(
                new AnnotatedType(AdSaveCommand.class).resolveAsRef(true));
        save.referencedSchemas.forEach(components::addSchemas);
        Schema<?> source = components.getSchemas().get("AdSaveCommand");
        if (source == null || source.getProperties() == null) {
            return;
        }
        var create = copySchema(source);
        var createVersion = new Schema<>(SpecVersion.V31);
        createVersion.setTypes(Set.of("integer", "null"));
        createVersion.format("int64").minimum(BigDecimal.ZERO).readOnly(true);
        create.addProperty("version", createVersion);
        create.setRequired(List.of("name", "position", "imageUrl", "targetUrl", "status", "startAt", "endAt", "sortOrder"));
        create.setAdditionalProperties(Boolean.FALSE);
        components.addSchemas("AdCreateInput", create);

        var update = copySchema(source);
        var updateVersion = new io.swagger.v3.oas.models.media.IntegerSchema()
                .format("int64").minimum(BigDecimal.ZERO);
        updateVersion.setSpecVersion(SpecVersion.V31);
        update.addProperty("version", updateVersion);
        update.setRequired(List.of("version", "name", "position", "imageUrl", "targetUrl", "status", "startAt", "endAt", "sortOrder"));
        update.setAdditionalProperties(Boolean.FALSE);
        components.addSchemas("AdUpdateInput", update);
    }

    private static void useOperationInputSchemas(OpenAPI api) {
        if (api.getPaths() == null || api.getPaths().get("/api/v1/ads") == null
                || api.getPaths().get("/api/v1/ads/{adId}") == null) {
            return;
        }
        setRequestSchema(api.getPaths().get("/api/v1/ads").getPost(), "#/components/schemas/AdCreateInput");
        setRequestSchema(api.getPaths().get("/api/v1/ads/{adId}").getPut(), "#/components/schemas/AdUpdateInput");
    }

    private static void setRequestSchema(io.swagger.v3.oas.models.Operation operation, String schemaReference) {
        if (operation == null || operation.getRequestBody() == null || operation.getRequestBody().getContent() == null) {
            return;
        }
        operation.getRequestBody().getContent().values().forEach(mediaType ->
                mediaType.setSchema(new Schema<>().$ref(schemaReference)));
    }

    @SuppressWarnings("unchecked")
    private static ObjectSchema copySchema(Schema<?> source) {
        ObjectSchema copy = new ObjectSchema();
        copy.setSpecVersion(SpecVersion.V31);
        copy.setProperties(new LinkedHashMap<>(source.getProperties()));
        copy.setRequired(source.getRequired() == null ? new ArrayList<>() : new ArrayList<>(source.getRequired()));
        copy.setDescription(source.getDescription());
        copy.setAdditionalProperties(source.getAdditionalProperties());
        return copy;
    }

    private static void setPublicOperationAnonymous(OpenAPI api) {
        if (api.getPaths() == null || api.getPaths().get("/api/v1/ad-slots/{position}/ads") == null) {
            return;
        }
        var operation = api.getPaths().get("/api/v1/ad-slots/{position}/ads").getGet();
        if (operation != null) {
            operation.setSecurity(List.of());
        }
    }
}
