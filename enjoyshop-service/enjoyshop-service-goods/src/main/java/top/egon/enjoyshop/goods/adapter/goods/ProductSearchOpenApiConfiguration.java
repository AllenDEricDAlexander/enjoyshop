package top.egon.enjoyshop.goods.adapter.goods;

import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.enjoyshop.goods.adapter.goods.pojo.vo.FieldErrorsVO;

/** Publishes the real generic error envelopes; a raw ResultRecord annotation loses validation data. */
@Configuration(value = "productSearchOpenApiConfiguration", proxyBeanMethods = false)
@Slf4j
public class ProductSearchOpenApiConfiguration {
    @Bean("productSearchErrorSchemas")
    OpenApiCustomizer productSearchErrorSchemas() {
        return api -> {
            if (api.getPaths() == null || !api.getPaths().containsKey("/api/v1/product-search")) { return; }
            if (api.getComponents() == null) { api.setComponents(new Components()); }
            var validation = ModelConverters.getInstance().resolveAsResolvedSchema(new AnnotatedType(
                    new TypeReference<ResultRecord<FieldErrorsVO>>() { }.getType()).resolveAsRef(true));
            validation.referencedSchemas.forEach(api.getComponents()::addSchemas);
            api.getComponents().addSchemas("SearchRequestError", validation.schema);
            var failure = ModelConverters.getInstance().resolveAsResolvedSchema(new AnnotatedType(
                    new TypeReference<ResultRecord<Void>>() { }.getType()).resolveAsRef(true));
            failure.referencedSchemas.forEach(api.getComponents()::addSchemas);
            api.getComponents().addSchemas("SearchBusinessError", failure.schema);
        };
    }
}
