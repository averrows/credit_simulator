package com.bcadigital.creditsimulator.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Commands accepted by the simulator menu loop.
 *
 * <p>Parsing is case-insensitive and trims surrounding whitespace, so interactive
 * typos like {@code " Hitung "} still work. Unknown or blank input yields
 * {@link Optional#empty()} instead of an exception: a mistyped command is an
 * expected user error, not a programming failure, so the caller simply re-prompts.
 */
public enum MenuCommand {
    /** Starts a new simulation by prompting the six loan inputs. */
    HITUNG("hitung  - simulasi baru (tanya 6 input berurutan)"),
    /** Fetches a saved calculation from the JSON web service and auto-displays it. */
    LOAD("load    - ambil perhitungan tersimpan + tampilkan hasil"),
    /** Lists all available commands. */
    SHOW("show    - tampilkan perintah ini"),
    /** Exits the application. */
    EXIT("exit    - keluar");

    private final String description;

    MenuCommand(final String description) {
        this.description = description;
    }

    /**
     * Returns the lowercase key used for matching user input.
     *
     * @return the lowercase key, e.g. {@code "hitung"}
     */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Parses raw user input into a command, accepting a unique prefix.
     *
     * <p>Matching is case-insensitive and trims surrounding whitespace. An exact match
     * yields {@code assumed = false}; a unique prefix (e.g. {@code "h"} for {@code hitung})
     * yields {@code assumed = true} so the caller can tell the user which command was
     * auto-selected instead of silently guessing.
     *
     * @param raw the raw line typed by the user or read from a file; may be null
     * @return the match, or empty when the input is null, blank, unknown, or ambiguous
     *     (a prefix of more than one command)
     */
    public static Optional<Match<MenuCommand>> parse(final String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        final String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (final MenuCommand command : values()) {
            if (command.key().equals(normalized)) {
                return Optional.of(new Match<>(command, false));
            }
        }
        MenuCommand hit = null;
        for (final MenuCommand command : values()) {
            if (command.key().startsWith(normalized)) {
                if (hit != null) {
                    return Optional.empty();
                }
                hit = command;
            }
        }
        return Optional.ofNullable(hit).map(command -> new Match<>(command, true));
    }

    /**
     * Returns the multi-line help text listing every command.
     *
     * @return one {@code name - description} line per command, in declaration order
     */
    public static String describeAll() {
        final StringBuilder text = new StringBuilder();
        for (final MenuCommand command : values()) {
            text.append(command.description).append(System.lineSeparator());
        }
        return text.toString();
    }
}
