package top.egon.enjoyshop.goods.infrastructure.goods.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.common.core.exception.CommonException;

/** Only individual JSON TEXT fields cross this codec; whole POJOs are never serialized as mappers. */
@Component("productJsonFieldConverter")
@Slf4j
@RequiredArgsConstructor
public class ProductJsonFieldConverter implements BaseForwardConverter<String, JsonNode> {
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper mapper;

    @Override
    public JsonNode toTarget(String source) {
        if (source == null) {
            return null;
        }
        try {
            JsonNode node = mapper.readTree(source);
            if (node == null || node.isNull()) {
                throw invalid();
            }
            return node;
        } catch (JsonProcessingException failure) {
            throw new CommonException(610299, "PRODUCT_INTERNAL_ERROR", "Stored product JSON is invalid", failure);
        }
    }

    @Named("decodeProductStrings")
    public List<String> decodeStrings(String source) {
        if (source == null) {
            return List.of();
        }
        return strings(toTarget(source));
    }

    @Named("decodeProductSpecItems")
    public Map<String, List<String>> decodeSpecItems(String source) {
        if (source == null) {
            return Map.of();
        }
        JsonNode node = toTarget(source);
        if (!node.isObject()) {
            throw invalid();
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> result.put(entry.getKey(), strings(entry.getValue())));
        return result;
    }

    @Named("decodeProductMap")
    public Map<String, String> decodeMap(String source) {
        if (source == null) {
            return Map.of();
        }
        JsonNode node = toTarget(source);
        if (!node.isObject()) {
            throw invalid();
        }
        Map<String, String> result = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            if (!entry.getValue().isTextual()) {
                throw invalid();
            }
            result.put(entry.getKey(), entry.getValue().textValue());
        });
        return result;
    }

    @Named("encodeProductField")
    public String encode(Object field) {
        if (field == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(field);
        } catch (JsonProcessingException failure) {
            throw new CommonException(610299, "PRODUCT_INTERNAL_ERROR", "Product field cannot be encoded", failure);
        }
    }

    private static List<String> strings(JsonNode node) {
        if (!node.isArray()) {
            throw invalid();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode value : node) {
            if (!value.isTextual()) {
                throw invalid();
            }
            values.add(value.textValue());
        }
        return values;
    }

    private static CommonException invalid() {
        return new CommonException(610299, "PRODUCT_INTERNAL_ERROR", "Stored product JSON has an invalid shape");
    }
}
