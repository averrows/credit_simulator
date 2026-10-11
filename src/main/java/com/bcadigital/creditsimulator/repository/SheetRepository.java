package com.bcadigital.creditsimulator.repository;

import java.util.List;

import com.bcadigital.creditsimulator.model.Sheet;

/**
 * Collection-like access to saved sheets (Repository pattern).
 *
 * <p>The service layer reads and saves {@link Sheet} objects without knowing how
 * or where they are stored; {@link JsonSheetRepository} keeps them in a JSON file,
 * and tests use an in-memory implementation. Sheet names are unique ignoring case.
 */
public interface SheetRepository {

    /**
     * Returns every saved sheet.
     *
     * @return the sheets in the order they were first saved; never null
     * @throws SheetStorageException when saved data exists but cannot be read
     */
    List<Sheet> findAll();

    /**
     * Saves a sheet: replaces the sheet with the same name (ignoring case) in place,
     * or appends it. The change is durable when this returns.
     *
     * @param sheet the sheet to save; never null
     * @throws SheetStorageException when the sheet cannot be written; saved data is
     *     then unchanged
     */
    void save(Sheet sheet);

    /**
     * Describes where sheets are kept, for messages to the user.
     *
     * @return e.g. the file path, never null
     */
    String location();
}
