package com.bcadigital.creditsimulator.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PrefixMatcherTest {

    private static final List<String> VALUES = List.of("mobil", "motor");

    @Test
    void shouldMatchExact_whenMixedCaseAndWhitespace() {
        assertThat(PrefixMatcher.parse(VALUES, key -> key, "  MoBiL "))
                .contains(new Match<>("mobil", false));
    }

    @Test
    void shouldAssume_whenUniquePrefix() {
        assertThat(PrefixMatcher.parse(VALUES, key -> key, "mot"))
                .contains(new Match<>("motor", true));
    }

    @Test
    void shouldReturnEmpty_whenAmbiguous() {
        assertThat(PrefixMatcher.parse(VALUES, key -> key, "mo")).isEmpty();
        assertThat(PrefixMatcher.candidates(VALUES, key -> key, "mo"))
                .containsExactly("mobil", "motor");
    }

    @Test
    void shouldReturnEmpty_whenUnknownBlankOrNull() {
        assertThat(PrefixMatcher.parse(VALUES, key -> key, "ngawur")).isEmpty();
        assertThat(PrefixMatcher.parse(VALUES, key -> key, "")).isEmpty();
        assertThat(PrefixMatcher.parse(VALUES, key -> key, "   ")).isEmpty();
        assertThat(PrefixMatcher.parse(VALUES, key -> key, null)).isEmpty();
        assertThat(PrefixMatcher.candidates(VALUES, key -> key, "ngawur")).isEmpty();
        assertThat(PrefixMatcher.candidates(VALUES, key -> key, null)).isEmpty();
    }
}
