package com.bcadigital.creditsimulator.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.Rupiah;
import com.bcadigital.creditsimulator.model.Sheet;
import com.bcadigital.creditsimulator.model.VehicleCondition;
import com.bcadigital.creditsimulator.model.VehicleRatePolicyFactory;
import com.bcadigital.creditsimulator.model.VehicleType;
import com.bcadigital.creditsimulator.service.InstallmentCalculator;
import com.bcadigital.creditsimulator.service.SimulationCalculator;

class JsonSheetRepositoryTest {

    private final SimulationCalculator calculator = new InstallmentCalculator(new VehicleRatePolicyFactory());

    @TempDir
    Path dir;

    private Sheet sheet(final String name, final VehicleType type) {
        return new Sheet(name, calculator.calculate(new LoanRequest(
                type, VehicleCondition.BARU, 2025, Rupiah.of(100_000_000L), 3, Rupiah.of(35_000_000L))));
    }

    @Test
    void shouldFindNothing_whenFileMissing() {
        assertThat(new JsonSheetRepository(dir.resolve("none.json"), calculator).findAll()).isEmpty();
    }

    @Test
    void shouldReadBackInSaveOrder_whenReopened() {
        final Path file = dir.resolve("sheets.json");
        final JsonSheetRepository repository = new JsonSheetRepository(file, calculator);
        final Sheet mobil = sheet("mobil baru", VehicleType.MOBIL);
        final Sheet motor = sheet("Sheet2", VehicleType.MOTOR);

        repository.save(mobil);
        repository.save(motor);

        assertThat(new JsonSheetRepository(file, calculator).findAll()).containsExactly(mobil, motor);
    }

    @Test
    void shouldReplaceInPlace_whenNameExistsIgnoringCase() {
        final Path file = dir.resolve("sheets.json");
        final JsonSheetRepository repository = new JsonSheetRepository(file, calculator);
        repository.save(sheet("a", VehicleType.MOBIL));
        repository.save(sheet("b", VehicleType.MOBIL));

        repository.save(sheet("A", VehicleType.MOTOR));

        assertThat(new JsonSheetRepository(file, calculator).findAll())
                .containsExactly(sheet("A", VehicleType.MOTOR), sheet("b", VehicleType.MOBIL));
    }

    @Test
    void shouldWriteWebServiceKeys_whenSaved() throws IOException {
        final Path file = dir.resolve("sheets.json");
        new JsonSheetRepository(file, calculator).save(sheet("a", VehicleType.MOBIL));

        assertThat(Files.readString(file))
                .startsWith("[\n")
                .contains("\"name\": \"a\"", "\"vehicleType\": \"Mobil\"", "\"vehicleCondition\": \"Baru\"",
                        "\"vehicleYear\": 2025", "\"totalLoanAmount\": 100000000", "\"loanTenure\": 3",
                        "\"downPayment\": 35000000");
    }

    @Test
    void shouldLoad_whenNewVehicleYearIsNoLongerCurrent() throws IOException {
        final Path file = dir.resolve("sheets.json");
        Files.writeString(file, "[{\"name\": \"lama\", \"vehicleType\": \"Mobil\", \"vehicleCondition\": \"Baru\","
                + " \"vehicleYear\": 2001, \"totalLoanAmount\": 100000000, \"loanTenure\": 3,"
                + " \"downPayment\": 35000000}]");

        assertThat(new JsonSheetRepository(file, calculator).findAll()).extracting(Sheet::name).containsExactly("lama");
    }

    @Test
    void shouldReportOnceThenActEmpty_whenFileCorrupt() throws IOException {
        final Path file = dir.resolve("sheets.json");
        Files.writeString(file, "[{\"name\": \"a\", \"vehicleType\": \"Pesawat\"}]");
        final JsonSheetRepository repository = new JsonSheetRepository(file, calculator);

        assertThatThrownBy(repository::findAll)
                .isInstanceOf(SheetStorageException.class).hasMessageContaining(file.toString());
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void shouldReplaceCorruptFile_whenSavedAfterReport() throws IOException {
        final Path file = dir.resolve("sheets.json");
        Files.writeString(file, "{not json");
        final JsonSheetRepository repository = new JsonSheetRepository(file, calculator);
        assertThatThrownBy(repository::findAll).isInstanceOf(SheetStorageException.class);

        repository.save(sheet("baru", VehicleType.MOBIL));

        assertThat(new JsonSheetRepository(file, calculator).findAll()).extracting(Sheet::name).containsExactly("baru");
    }

    @Test
    void shouldKeepOldFile_whenSaveFails() throws IOException {
        final Path file = dir.resolve("missing-dir").resolve("sheets.json");

        final JsonSheetRepository repository = new JsonSheetRepository(file, calculator);

        assertThatThrownBy(() -> repository.save(sheet("a", VehicleType.MOBIL)))
                .isInstanceOf(SheetStorageException.class);
        assertThat(Files.exists(file)).isFalse();
        assertThat(repository.findAll()).isEmpty();
    }
}
