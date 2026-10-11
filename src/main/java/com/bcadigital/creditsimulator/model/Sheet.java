package com.bcadigital.creditsimulator.model;

import java.util.Objects;

/**
 * A saved calculation under a user-chosen name, shown as one tab of the workbook.
 *
 * <p>Implements {@link Choice}, so users switch sheets by typing a unique prefix
 * of the name just like any other option.
 *
 * @param name the sheet name: 1-31 letters, digits, spaces, {@code _} or {@code -},
 *     trimmed (the 31-character limit and character set keep it a valid Excel tab name)
 * @param result the saved calculation; never null
 */
public record Sheet(String name, SimulationResult result) implements Choice {

    private static final String VALID_NAME = "[A-Za-z0-9 _-]{1,31}";

    /**
     * Creates a sheet, trimming the name.
     *
     * @throws InvalidSheetNameException when the name is null, blank, too long, or
     *     contains other characters
     */
    public Sheet {
        name = name == null ? "" : name.trim();
        if (!name.matches(VALID_NAME)) {
            throw new InvalidSheetNameException("Nama sheet harus 1-31 huruf/angka/spasi/_/-, dapat: \""
                    + name + "\"");
        }
        Objects.requireNonNull(result, "result");
    }

    @Override
    public String displayName() {
        return name;
    }
}
