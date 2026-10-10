package com.bcadigital.creditsimulator.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link InputSource} reading lines from a batch input file (file mode).
 */
public final class FileInputSource implements InputSource {

    private final BufferedReader reader;

    /**
     * Creates a source reading the given file using UTF-8.
     *
     * @param file the batch input file; never null
     * @throws UncheckedIOException when the file cannot be opened
     */
    public FileInputSource(final Path file) {
        Objects.requireNonNull(file, "file");
        try {
            this.reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException("gagal membuka file input: " + file, e);
        }
    }

    @Override
    public Optional<String> readLine() {
        try {
            return Optional.ofNullable(reader.readLine());
        } catch (final IOException e) {
            throw new UncheckedIOException("gagal membaca file input", e);
        }
    }
}
