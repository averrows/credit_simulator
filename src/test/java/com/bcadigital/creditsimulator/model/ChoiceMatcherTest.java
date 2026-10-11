package com.bcadigital.creditsimulator.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class ChoiceMatcherTest {

    private enum Fruit implements Choice {
        MANGGA("Mangga"), MANGGIS("Manggis"), APEL("Apel");

        private final String displayName;

        Fruit(final String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String displayName() {
            return displayName;
        }
    }

    private final ChoiceMatcher<Fruit> matcher = new ChoiceMatcher<>(List.of(Fruit.values()), "buah");

    @Test
    void shouldMatchExact_whenMixedCaseAndWhitespace() {
        assertThat(matcher.match("  MaNgGa ")).isEqualTo(new Match<>(Fruit.MANGGA, false));
    }

    @Test
    void shouldMatchExact_whenKeyIsAlsoPrefixOfAnother() {
        assertThat(matcher.match("mangga")).isEqualTo(new Match<>(Fruit.MANGGA, false));
    }

    @Test
    void shouldAssume_whenUniquePrefix() {
        assertThat(matcher.match("ap")).isEqualTo(new Match<>(Fruit.APEL, true));
        assertThat(matcher.match("manggi")).isEqualTo(new Match<>(Fruit.MANGGIS, true));
    }

    @Test
    void shouldNameCandidates_whenAmbiguous() {
        assertThatThrownBy(() -> matcher.match(" mang "))
                .isInstanceOf(InvalidChoiceException.class)
                .hasMessage("\"mang\" ambigu (Mangga atau Manggis?)");
    }

    @Test
    void shouldUseNoun_whenUnknownBlankOrNull() {
        assertThatThrownBy(() -> matcher.match("durian")).hasMessage("buah tidak dikenal: \"durian\"");
        assertThatThrownBy(() -> matcher.match("   ")).hasMessage("buah tidak dikenal: \"\"");
        assertThatThrownBy(() -> matcher.match(null)).isInstanceOf(InvalidChoiceException.class);
    }

    @Test
    void shouldReject_whenNoOptions() {
        assertThatThrownBy(() -> new ChoiceMatcher<Fruit>(List.of(), "buah"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
