package com.bcadigital.creditsimulator.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileInputSourceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadLines_whenFileExists() throws Exception {
        final Path file = tempDir.resolve("batch.txt");
        Files.write(file, List.of("hitung", "exit"), StandardCharsets.UTF_8);
        final InputSource source = new FileInputSource(file);

        assertThat(source.readLine()).contains("hitung");
        assertThat(source.readLine()).contains("exit");
        assertThat(source.readLine()).isEmpty();
    }

    @Test
    void shouldFailFast_whenFileMissing() {
        assertThrows(UncheckedIOException.class,
                () -> new FileInputSource(tempDir.resolve("tidak-ada.txt")));
    }
}
