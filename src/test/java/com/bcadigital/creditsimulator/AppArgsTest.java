package com.bcadigital.creditsimulator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.bcadigital.creditsimulator.service.RemoteLoader;

class AppArgsTest {

    @Test
    void shouldDefault_whenNoArgs() {
        final AppArgs.Options options = AppArgs.parse(new String[0]);

        assertThat(options.file()).isNull();
        assertThat(options.apiUrl()).isEqualTo(RemoteLoader.DEFAULT_URL);
        assertThat(options.help()).isFalse();
    }

    @Test
    void shouldUseFile_whenPositionalArg() {
        final AppArgs.Options options = AppArgs.parse(new String[]{"file_inputs.txt"});

        assertThat(options.file()).isEqualTo("file_inputs.txt");
        assertThat(options.help()).isFalse();
    }

    @Test
    void shouldParseUrl_whenBeforeAndAfterFile() {
        assertThat(AppArgs.parse(new String[]{"--url", "http://x", "f.txt"}).apiUrl())
                .isEqualTo("http://x");
        assertThat(AppArgs.parse(new String[]{"f.txt", "--url", "http://x"}).apiUrl())
                .isEqualTo("http://x");
        assertThat(AppArgs.parse(new String[]{"f.txt", "--url", "http://x"}).file())
                .isEqualTo("f.txt");
    }

    @Test
    void shouldShowHelp_whenHelpUnknownOrExtraArgs() {
        assertThat(AppArgs.parse(new String[]{"-h"}).help()).isTrue();
        assertThat(AppArgs.parse(new String[]{"--help"}).help()).isTrue();
        assertThat(AppArgs.parse(new String[]{"-x"}).help()).isTrue();
        assertThat(AppArgs.parse(new String[]{"--url"}).help()).isTrue();
        assertThat(AppArgs.parse(new String[]{"a.txt", "b.txt"}).help()).isTrue();
    }
}
