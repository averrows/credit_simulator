package com.bcadigital.creditsimulator.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class StreamInputSourceTest {

    @Test
    void shouldReadLines_whenAvailable() {
        final InputSource source =
                new StreamInputSource(new ByteArrayInputStream("mobil\nbaru\n".getBytes(StandardCharsets.UTF_8)));

        assertThat(source.readLine()).contains("mobil");
        assertThat(source.readLine()).contains("baru");
        assertThat(source.readLine()).isEmpty();
    }
}
