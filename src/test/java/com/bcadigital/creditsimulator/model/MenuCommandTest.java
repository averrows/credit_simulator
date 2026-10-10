package com.bcadigital.creditsimulator.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MenuCommandTest {

    @Test
    void shouldMatchExact_whenMixedCaseAndSurroundingWhitespace() {
        assertThat(MenuCommand.parse("  HiTuNg ")).contains(new Match<>(MenuCommand.HITUNG, false));
        assertThat(MenuCommand.parse("LOAD")).contains(new Match<>(MenuCommand.LOAD, false));
        assertThat(MenuCommand.parse("Show")).contains(new Match<>(MenuCommand.SHOW, false));
        assertThat(MenuCommand.parse("exit")).contains(new Match<>(MenuCommand.EXIT, false));
    }

    @Test
    void shouldMatchByPrefix_whenUniquePrefix() {
        assertThat(MenuCommand.parse("h")).contains(new Match<>(MenuCommand.HITUNG, true));
        assertThat(MenuCommand.parse("HIT")).contains(new Match<>(MenuCommand.HITUNG, true));
        assertThat(MenuCommand.parse("lo")).contains(new Match<>(MenuCommand.LOAD, true));
        assertThat(MenuCommand.parse("S")).contains(new Match<>(MenuCommand.SHOW, true));
        assertThat(MenuCommand.parse("exi")).contains(new Match<>(MenuCommand.EXIT, true));
        assertThat(MenuCommand.parse("  l  ")).contains(new Match<>(MenuCommand.LOAD, true));
    }

    @Test
    void shouldReturnEmpty_whenUnknownBlankOrNull() {
        assertThat(MenuCommand.parse("ngawur")).isEmpty();
        assertThat(MenuCommand.parse("show me")).isEmpty();
        assertThat(MenuCommand.parse("hitungx")).isEmpty();
        assertThat(MenuCommand.parse("!@#$")).isEmpty();
        assertThat(MenuCommand.parse("")).isEmpty();
        assertThat(MenuCommand.parse("   ")).isEmpty();
        assertThat(MenuCommand.parse(null)).isEmpty();
    }

    @Test
    void shouldReturnEmpty_whenInputExceedsCommandNames() {
        assertThat(MenuCommand.parse("x".repeat(10_000))).isEmpty();
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
    void shouldReturnLowercaseKey_whenKey() {
        assertThat(MenuCommand.HITUNG.key()).isEqualTo("hitung");
    }
}
