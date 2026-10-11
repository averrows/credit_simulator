package com.bcadigital.creditsimulator.repository;

import java.util.ArrayList;
import java.util.List;

import com.bcadigital.creditsimulator.model.Sheet;

/** Test double keeping sheets in memory; can be told to fail like a broken file. */
public final class InMemorySheetRepository implements SheetRepository {

    private final List<Sheet> sheets = new ArrayList<>();
    private boolean failNextFind;
    private boolean failSave;

    /** Makes the next {@link #findAll()} fail once, as if the saved file were corrupt. */
    public InMemorySheetRepository failingFirstFind() {
        failNextFind = true;
        return this;
    }

    /** Makes every {@link #save(Sheet)} fail, as if the disk were read-only. */
    public InMemorySheetRepository failingSave() {
        failSave = true;
        return this;
    }

    @Override
    public List<Sheet> findAll() {
        if (failNextFind) {
            failNextFind = false;
            throw new SheetStorageException("file rusak", null);
        }
        return List.copyOf(sheets);
    }

    @Override
    public void save(final Sheet sheet) {
        if (failSave) {
            throw new SheetStorageException("disk penuh", null);
        }
        for (int i = 0; i < sheets.size(); i++) {
            if (sheets.get(i).name().equalsIgnoreCase(sheet.name())) {
                sheets.set(i, sheet);
                return;
            }
        }
        sheets.add(sheet);
    }

    @Override
    public String location() {
        return "memori";
    }
}
