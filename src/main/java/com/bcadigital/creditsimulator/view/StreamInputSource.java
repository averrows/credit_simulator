package com.bcadigital.creditsimulator.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link InputSource} reading from an input stream (standard input in interactive mode).
 */
public final class StreamInputSource implements InputSource {

    private final BufferedReader reader;

    /**
     * Creates a source reading the given stream using UTF-8.
     *
     * @param input the stream to read; never null
     */
    public StreamInputSource(final InputStream input) {
        Objects.requireNonNull(input, "input");
        this.reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
    }

    @Override
    public Optional<String> readLine() {
        try {
            return Optional.ofNullable(reader.readLine());
        } catch (final IOException e) {
            throw new UncheckedIOException("gagal membaca input", e);
        }
    }
}
