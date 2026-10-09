package top.egon.enjoyshop.goods.common.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

/** Strict product-field tokens; no global Jackson coercion policy is changed. */
public class ProductIdDeserializer extends StdDeserializer<Long> {

    public ProductIdDeserializer() {
        super(Long.class);
    }

    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (!parser.hasToken(JsonToken.VALUE_STRING)
                || !parser.getText().matches("(?:0|[1-9][0-9]{0,18})")) {
            throw JsonMappingException.from(parser, "Product identifiers require canonical decimal strings");
        }
        try {
            return Long.parseLong(parser.getText());
        } catch (NumberFormatException failure) {
            throw JsonMappingException.from(parser, "Product identifier exceeds the supported range");
        }
    }

    /** Money in cents and quantities are integers, including on the wire. */
    public static class IntegerValueDeserializer extends StdDeserializer<Integer> {
        public IntegerValueDeserializer() {
            super(Integer.class);
        }

        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                throw JsonMappingException.from(parser, "Product quantities and cent prices require integer tokens");
            }
            return parser.getIntValue();
        }
    }

    public static class LongIntegerValueDeserializer extends StdDeserializer<Long> {
        public LongIntegerValueDeserializer() { super(Long.class); }
        @Override
        public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                throw JsonMappingException.from(parser, "Product versions require integer tokens");
            }
            return parser.getLongValue();
        }
    }

    public static class BooleanValueDeserializer extends StdDeserializer<Boolean> {
        public BooleanValueDeserializer() {
            super(Boolean.class);
        }

        @Override
        public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_TRUE) && !parser.hasToken(JsonToken.VALUE_FALSE)) {
                throw JsonMappingException.from(parser, "Product flags require boolean tokens");
            }
            return parser.hasToken(JsonToken.VALUE_TRUE);
        }
    }
}
