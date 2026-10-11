package com.bcadigital.creditsimulator.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import com.bcadigital.creditsimulator.json.MiniJson;
import com.bcadigital.creditsimulator.model.InvalidInputException;
import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.Rupiah;
import com.bcadigital.creditsimulator.model.Sheet;
import com.bcadigital.creditsimulator.model.VehicleCondition;
import com.bcadigital.creditsimulator.model.VehicleType;
import com.bcadigital.creditsimulator.service.SimulationCalculator;

/**
 * Keeps sheets in a JSON file: an array with one object per sheet, using the same
 * keys as the web-service payload plus {@code name}:
 *
 * <pre>
 * [
 *   {"name": "Sheet1", "vehicleType": "Mobil", "vehicleCondition": "Baru",
 *    "vehicleYear": 2025, "totalLoanAmount": 100000000, "loanTenure": 3,
 *    "downPayment": 35000000}
 * ]
 * </pre>
 *
 * <p>The file is read once and cached. Only inputs are stored; loading recalculates
 * each schedule, so the calculator stays the single source of truth for the math.
 * Input rules are deliberately not re-validated on load: a sheet saved as
 * "Baru 2025" must stay loadable in later years.
 *
 * <p>An unreadable file is reported by the first {@link #findAll()}, then treated as
 * empty so the session can go on; the next {@link #save} replaces it. Every write
 * goes to a temporary file that atomically replaces the old one, so a failed save
 * never corrupts what was saved before.
 */
public final class JsonSheetRepository implements SheetRepository {

    /** Default file name, created in the working directory. */
    public static final String DEFAULT_FILE = "credit_simulator_sheets.json";

    private final Path file;
    private final SimulationCalculator calculator;
    private List<Sheet> cache;

    /**
     * Creates a repository for the given file. Nothing is read until first use.
     *
     * @param file the JSON file; need not exist yet; never null
     * @param calculator rebuilds each schedule from the saved inputs; never null
     */
    public JsonSheetRepository(final Path file, final SimulationCalculator calculator) {
        this.file = Objects.requireNonNull(file, "file");
        this.calculator = Objects.requireNonNull(calculator, "calculator");
    }

    @Override
    public List<Sheet> findAll() {
        if (cache == null) {
            // Set before reading: if the file is unreadable, the cache stays empty and
            // later calls no longer throw (see class comment).
            cache = new ArrayList<>();
            cache.addAll(readFile());
        }
        return List.copyOf(cache);
    }

    @Override
    public void save(final Sheet sheet) {
        Objects.requireNonNull(sheet, "sheet");
        final List<Sheet> updated = new ArrayList<>(cachedOrEmpty());
        final int existing = indexOf(updated, sheet.name());
        if (existing >= 0) {
            updated.set(existing, sheet);
        } else {
            updated.add(sheet);
        }
        writeFile(updated);
        cache = updated;
    }

    @Override
    public String location() {
        return file.toString();
    }

    /** Loads the cache if needed; an unreadable file was already reported, so it counts as empty here. */
    private List<Sheet> cachedOrEmpty() {
        try {
            return findAll();
        } catch (final SheetStorageException e) {
            return List.of();
        }
    }

    private List<Sheet> readFile() {
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            final List<Sheet> sheets = new ArrayList<>();
            for (final Map<String, String> values : MiniJson.parseObjectArray(Files.readString(file))) {
                sheets.add(new Sheet(require(values, "name"), calculator.calculate(toRequest(values))));
            }
            return sheets;
        } catch (final IOException | IllegalArgumentException | InvalidInputException e) {
            throw new SheetStorageException("file sheet " + file + " tidak bisa dibaca (" + e.getMessage() + ")", e);
        }
    }

    private void writeFile(final List<Sheet> sheets) {
        Path temp = null;
        try {
            temp = Files.createTempFile(file.toAbsolutePath().getParent(), ".credit_simulator", ".json.tmp");
            Files.writeString(temp, toJson(sheets), StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (final IOException e) {
            deleteQuietly(temp);
            throw new SheetStorageException("gagal menyimpan sheet ke " + file + " (" + e.getMessage() + ")", e);
        }
    }

    private static String toJson(final List<Sheet> sheets) {
        final StringJoiner array = new StringJoiner(",\n", "[\n", "\n]\n");
        for (final Sheet sheet : sheets) {
            final LoanRequest request = sheet.result().request();
            array.add("  {\"name\": " + quote(sheet.name())
                    + ", \"vehicleType\": " + quote(request.vehicleType().displayName())
                    + ", \"vehicleCondition\": " + quote(request.vehicleCondition().displayName())
                    + ", \"vehicleYear\": " + request.vehicleYear()
                    + ", \"totalLoanAmount\": " + request.totalLoanAmount().amount()
                    + ", \"loanTenure\": " + request.loanTenureYears()
                    + ", \"downPayment\": " + request.downPayment().amount() + "}");
        }
        return array.toString();
    }

    private static LoanRequest toRequest(final Map<String, String> values) {
        return new LoanRequest(
                VehicleType.parse(require(values, "vehicleType")).value(),
                VehicleCondition.parse(require(values, "vehicleCondition")).value(),
                Integer.parseInt(require(values, "vehicleYear")),
                Rupiah.of(Long.parseLong(require(values, "totalLoanAmount"))),
                Integer.parseInt(require(values, "loanTenure")),
                Rupiah.of(Long.parseLong(require(values, "downPayment"))));
    }

    private static int indexOf(final List<Sheet> sheets, final String name) {
        for (int i = 0; i < sheets.size(); i++) {
            if (sheets.get(i).name().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    private static String require(final Map<String, String> values, final String key) {
        final String value = values.get(key);
        if (value == null) {
            throw new IllegalArgumentException("tidak ada \"" + key + "\"");
        }
        return value;
    }

    /** Sheet names and display names never contain control characters, so quotes and backslashes suffice. */
    private static String quote(final String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static void deleteQuietly(final Path temp) {
        if (temp == null) {
            return;
        }
        try {
            Files.deleteIfExists(temp);
        } catch (final IOException e) {
            // Best effort: the save already failed and is reported; a stray temp file is harmless.
        }
    }
}
