package com.bcadigital.creditsimulator.model;

import java.util.List;
import java.util.Locale;

/**
 * Commands accepted by the simulator menu loop.
 *
 * <p>Parsing is case-insensitive, trims surrounding whitespace, and accepts a unique
 * prefix, so interactive input like {@code " H "} still selects {@code hitung}.
 */
public enum MenuCommand implements Choice {
    /** Starts a new simulation by prompting the six loan inputs. */
    HITUNG("simulasi baru (tanya 6 input berurutan)"),
    /** Fetches a saved calculation from the JSON web service and auto-displays it. */
    LOAD("ambil perhitungan tersimpan + tampilkan hasil"),
    /** Lists all available commands. */
    SHOW("tampilkan perintah ini"),
    /** Exits the application. */
    EXIT("keluar");

    private static final ChoiceMatcher<MenuCommand> MATCHER = new ChoiceMatcher<>(List.of(values()), "perintah");

    private final String description;

    MenuCommand(final String description) {
        this.description = description;
    }

    /**
     * Returns the command as the user types it.
     *
     * @return the lowercase command name, e.g. {@code "hitung"}
     */
    @Override
    public String displayName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Parses raw user input into a command, accepting a unique prefix.
     *
     * @param raw the raw line typed by the user or read from a file; may be null
     * @return the match ({@code assumed = true} for prefix matches), never null
     * @throws InvalidChoiceException when the input is null, blank, unknown, or ambiguous
     */
    public static Match<MenuCommand> parse(final String raw) {
        return MATCHER.match(raw);
    }

    /**
     * Returns the multi-line help text listing every command.
     *
     * @return one {@code name - description} line per command, in declaration order
     */
    public static String describeAll() {
        final StringBuilder text = new StringBuilder();
        for (final MenuCommand command : values()) {
            text.append(String.format("%-8s- %s", command.key(), command.description))
                    .append(System.lineSeparator());
        }
        return text.toString();
    }
}
