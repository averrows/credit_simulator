package com.bcadigital.creditsimulator.model;

import java.util.Locale;

/**
 * An option the user picks by typing its name, such as a vehicle type or a menu command.
 *
 * <p>Implementing this is all an enum needs to become leniently parseable through
 * {@link ChoiceMatcher}: case-insensitive, trimmed, and unique prefixes accepted.
 */
public interface Choice {

    /**
     * Returns the name shown to users, e.g. {@code "Mobil"}.
     *
     * @return the display name, never null or blank
     */
    String displayName();

    /**
     * Returns the key user input is matched against.
     *
     * @return the lowercase display name, e.g. {@code "mobil"}
     */
    default String key() {
        return displayName().toLowerCase(Locale.ROOT);
    }
}
