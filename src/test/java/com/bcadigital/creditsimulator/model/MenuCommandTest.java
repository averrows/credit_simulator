package com.bcadigital.creditsimulator.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MenuCommandTest {

    @Test
    void shouldMatchExact_whenMixedCaseAndSurroundingWhitespace() {
        assertThat(MenuCommand.parse("  HiTuNg ")).isEqualTo(new Match<>(MenuCommand.HITUNG, false));
        assertThat(MenuCommand.parse("LOAD")).isEqualTo(new Match<>(MenuCommand.LOAD, false));
        assertThat(MenuCommand.parse("Show")).isEqualTo(new Match<>(MenuCommand.SHOW, false));
        assertThat(MenuCommand.parse("exit")).isEqualTo(new Match<>(MenuCommand.EXIT, false));
    }

    @Test
    void shouldMatchByPrefix_whenUniquePrefix() {
        assertThat(MenuCommand.parse("h")).isEqualTo(new Match<>(MenuCommand.HITUNG, true));
        assertThat(MenuCommand.parse("HIT")).isEqualTo(new Match<>(MenuCommand.HITUNG, true));
        assertThat(MenuCommand.parse("lo")).isEqualTo(new Match<>(MenuCommand.LOAD, true));
        assertThat(MenuCommand.parse("SH")).isEqualTo(new Match<>(MenuCommand.SHOW, true));
        assertThat(MenuCommand.parse("sa")).isEqualTo(new Match<>(MenuCommand.SAVE, true));
        assertThat(MenuCommand.parse("sw")).isEqualTo(new Match<>(MenuCommand.SWITCH, true));
        assertThat(MenuCommand.parse("exi")).isEqualTo(new Match<>(MenuCommand.EXIT, true));
        assertThat(MenuCommand.parse("  l  ")).isEqualTo(new Match<>(MenuCommand.LOAD, true));
    }

    @Test
    void shouldReject_whenUnknownBlankOrNull() {
        assertThatThrownBy(() -> MenuCommand.parse("ngawur")).hasMessage("perintah tidak dikenal: \"ngawur\"");
        assertThatThrownBy(() -> MenuCommand.parse("ngawur")).isInstanceOf(InvalidChoiceException.class);
        assertThatThrownBy(() -> MenuCommand.parse("show me")).isInstanceOf(InvalidChoiceException.class);
        assertThatThrownBy(() -> MenuCommand.parse("hitungx")).isInstanceOf(InvalidChoiceException.class);
        assertThatThrownBy(() -> MenuCommand.parse("!@#$")).isInstanceOf(InvalidChoiceException.class);
        assertThatThrownBy(() -> MenuCommand.parse("")).isInstanceOf(InvalidChoiceException.class);
        assertThatThrownBy(() -> MenuCommand.parse("   ")).isInstanceOf(InvalidChoiceException.class);
        assertThatThrownBy(() -> MenuCommand.parse(null)).isInstanceOf(InvalidChoiceException.class);
    }

    @Test
    void shouldReject_whenInputExceedsCommandNames() {
        assertThatThrownBy(() -> MenuCommand.parse("x".repeat(10_000))).isInstanceOf(InvalidChoiceException.class);
    }

    @Test
    void shouldDescribeAll_whenCalled() {
        assertThat(MenuCommand.describeAll())
                .contains("hitung")
                .contains("load")
                .contains("show")
                .contains("exit");
    }

    @Test
    void shouldNameCandidates_whenPrefixSharedBySCommands() {
        assertThatThrownBy(() -> MenuCommand.parse("s")).hasMessage("\"s\" ambigu (save atau switch atau show?)");
    }

    @Test
    void shouldAlignDescriptions_whenDescribeAll() {
        assertThat(MenuCommand.describeAll())
                .startsWith("hitung  - simulasi baru (tanya 6 input berurutan)" + System.lineSeparator())
                .contains("exit    - keluar" + System.lineSeparator());
    }

    @Test
    void shouldReturnLowercaseKey_whenKey() {
        assertThat(MenuCommand.HITUNG.key()).isEqualTo("hitung");
    }
}
