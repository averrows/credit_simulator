package com.bcadigital.creditsimulator.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.bcadigital.creditsimulator.model.ChoiceMatcher;
import com.bcadigital.creditsimulator.model.InvalidChoiceException;
import com.bcadigital.creditsimulator.model.InvalidSheetNameException;
import com.bcadigital.creditsimulator.model.Match;
import com.bcadigital.creditsimulator.model.Sheet;
import com.bcadigital.creditsimulator.model.SimulationResult;
import com.bcadigital.creditsimulator.repository.SheetRepository;
import com.bcadigital.creditsimulator.repository.SheetStorageException;

/**
 * The sheet session: saving results under names and switching between them.
 *
 * <p>The {@link SheetRepository} owns the saved sheets; this service adds what a
 * session needs on top: default names, prefix matching when switching, which sheet
 * is current, and starting empty with a warning when saved data is unreadable
 * instead of failing.
 */
public final class SheetBook {

    private static final String DEFAULT_NAME_PREFIX = "Sheet";

    private final SheetRepository repository;
    private final String loadWarning;
    private String currentName;

    private SheetBook(final SheetRepository repository, final String currentName, final String loadWarning) {
        this.repository = repository;
        this.currentName = currentName;
        this.loadWarning = loadWarning;
    }

    /**
     * Opens the sheets saved in the repository. Never fails: unreadable data yields
     * an empty book whose {@link #loadWarning()} explains what happened.
     *
     * @param repository where the sheets live; never null
     * @return the book, with the last saved sheet as current, never null
     */
    public static SheetBook open(final SheetRepository repository) {
        Objects.requireNonNull(repository, "repository");
        try {
            final List<Sheet> saved = repository.findAll();
            return new SheetBook(repository, saved.isEmpty() ? null : saved.get(saved.size() - 1).name(), null);
        } catch (final SheetStorageException e) {
            return new SheetBook(repository, null, e.getMessage()
                    + " — mulai dengan sheet kosong; save berikutnya menimpa " + repository.location());
        }
    }

    /**
     * Saves a calculation as a sheet and makes it current. A sheet with the same
     * name (ignoring case) is replaced.
     *
     * @param rawName the sheet name; blank picks {@link #nextDefaultName()}
     * @param result the calculation to save; never null
     * @return the saved sheet, never null
     * @throws InvalidSheetNameException when the name is not a valid sheet name
     * @throws SheetStorageException when the sheet cannot be written; nothing changes
     */
    public Sheet save(final String rawName, final SimulationResult result) {
        final Sheet sheet = new Sheet(rawName == null || rawName.isBlank() ? nextDefaultName() : rawName, result);
        repository.save(sheet);
        currentName = sheet.name();
        return sheet;
    }

    /**
     * Makes the sheet named by the input current, accepting a unique prefix.
     *
     * @param raw the typed sheet name; may be null
     * @return the match ({@code assumed = true} for prefix matches), never null
     * @throws InvalidChoiceException when no sheet matches or the input is ambiguous
     * @throws IllegalStateException when there are no sheets; check {@link #sheets()} first
     */
    public Match<Sheet> switchTo(final String raw) {
        final List<Sheet> saved = sheets();
        if (saved.isEmpty()) {
            throw new IllegalStateException("no sheets to switch to");
        }
        final Match<Sheet> match = new ChoiceMatcher<>(saved, "sheet").match(raw);
        currentName = match.value().name();
        return match;
    }

    /**
     * Returns the saved sheets.
     *
     * @return the sheets in the order they were first saved, never null
     */
    public List<Sheet> sheets() {
        try {
            return repository.findAll();
        } catch (final SheetStorageException e) {
            // Already reported through loadWarning(); the session continues without saved sheets.
            return List.of();
        }
    }

    /**
     * Returns the sheet most recently saved or switched to.
     *
     * @return the current sheet, or empty when there is none
     */
    public Optional<Sheet> current() {
        return sheets().stream().filter(sheet -> sheet.name().equalsIgnoreCase(currentName)).findFirst();
    }

    /**
     * Returns the name a blank save would use.
     *
     * @return the first free name of the form {@code Sheet1}, {@code Sheet2}, …
     */
    public String nextDefaultName() {
        final List<Sheet> saved = sheets();
        int number = saved.size() + 1;
        while (isTaken(saved, DEFAULT_NAME_PREFIX + number)) {
            number++;
        }
        return DEFAULT_NAME_PREFIX + number;
    }

    /**
     * Explains why saved sheets could not be loaded.
     *
     * @return the user-facing warning, or empty when loading succeeded
     */
    public Optional<String> loadWarning() {
        return Optional.ofNullable(loadWarning);
    }

    /**
     * Describes where sheets are saved, for messages to the user.
     *
     * @return the repository location, never null
     */
    public String location() {
        return repository.location();
    }

    private static boolean isTaken(final List<Sheet> sheets, final String name) {
        return sheets.stream().anyMatch(sheet -> sheet.name().equalsIgnoreCase(name));
    }
}
