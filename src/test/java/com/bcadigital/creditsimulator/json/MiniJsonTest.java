package com.bcadigital.creditsimulator.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

class MiniJsonTest {

    @Test
    void shouldParse_whenMixedTypesAndWhitespace() {
        final Map<String, String> values = MiniJson.parseObject(
                "{\n  \"name\" : \"Mobil\", \"year\" : 2025,\t\"total\" : -12.5e2 , \"ok\" : true ,"
                        + " \"no\" : false , \"nil\" : null }");

        assertThat(values)
                .containsEntry("name", "Mobil")
                .containsEntry("year", "2025")
                .containsEntry("total", "-12.5e2")
                .containsEntry("ok", "true")
                .containsEntry("no", "false")
                .containsEntry("nil", "null");
    }

    @Test
    void shouldParse_whenEscapes() {
        final Map<String, String> values =
                MiniJson.parseObject("{\"s\": \"a\\\"b\\\\c\\/d\\n\\t\\u0041\"}");

        assertThat(values).containsEntry("s", "a\"b\\c/d\n\tA");
    }

    @Test
    void shouldParse_whenEmptyObject() {
        assertThat(MiniJson.parseObject("{}")).isEmpty();
    }

    @Test
    void shouldReject_whenMalformed() {
        final String[] malformed = {
            "",
            "{",
            "[1, 2]",
            "{\"a\"}",
            "{\"a\":}",
            "{\"a\":1,}",
            "{\"a\" 1}",
            "{\"a\": \"tak berakhir}",
            "{\"a\": \"x\"} sisa",
            "{\"a\": tru}",
            "{\"a\": \"\\q\"}",
            "{\"a\": \"\\u12\"}",
        };
        for (final String json : malformed) {
            assertThrows(IllegalArgumentException.class, () -> MiniJson.parseObject(json), json);
        }
    }

    @Test
    void shouldParseArrayOfObjects_whenArrayGiven() {
        assertThat(MiniJson.parseObjectArray(" [ {\"a\": 1}, {\"b\": \"x\"} ] "))
                .containsExactly(Map.of("a", "1"), Map.of("b", "x"));
        assertThat(MiniJson.parseObjectArray("[]")).isEmpty();
    }

    @Test
    void shouldReject_whenArrayMalformed() {
        assertThatThrownBy(() -> MiniJson.parseObjectArray("[{\"a\": 1} {\"b\": 2}]"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MiniJson.parseObjectArray("[{\"a\": 1}"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MiniJson.parseObjectArray("[{\"a\": 1}] x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
