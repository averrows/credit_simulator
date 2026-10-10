package com.bcadigital.creditsimulator.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MenuCommandTest {

    @Test
    void shouldParse_whenMixedCaseAndSurroundingWhitespace() {
        assertThat(MenuCommand.parse("  HiTuNg ")).contains(MenuCommand.HITUNG);
        assertThat(MenuCommand.parse("LOAD")).contains(MenuCommand.LOAD);
        assertThat(MenuCommand.parse("Show")).contains(MenuCommand.SHOW);
        assertThat(MenuCommand.parse("exit")).contains(MenuCommand.EXIT);
    }

    @Test
    void shouldReturnEmpty_whenUnknownBlankOrNull() {
        assertThat(MenuCommand.parse("ngawur")).isEmpty();
        assertThat(MenuCommand.parse("")).isEmpty();
        assertThat(MenuCommand.parse("   ")).isEmpty();
        assertThat(MenuCommand.parse(null)).isEmpty();
    }

    @Test
    void shouldDescribeAll_whenCalled() {
        assertThat(MenuCommand.describeAll())
                .contains("hitung")
                .contains("load")
                .contains("show")
                .contains("exit");
    }
}
