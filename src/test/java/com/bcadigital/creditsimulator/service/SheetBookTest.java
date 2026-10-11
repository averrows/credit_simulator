package com.bcadigital.creditsimulator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.bcadigital.creditsimulator.model.InvalidChoiceException;
import com.bcadigital.creditsimulator.model.InvalidSheetNameException;
import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.Rupiah;
import com.bcadigital.creditsimulator.model.Sheet;
import com.bcadigital.creditsimulator.model.SimulationResult;
import com.bcadigital.creditsimulator.model.VehicleCondition;
import com.bcadigital.creditsimulator.model.VehicleType;
import com.bcadigital.creditsimulator.repository.InMemorySheetRepository;
import com.bcadigital.creditsimulator.repository.SheetStorageException;

class SheetBookTest {

    private static SimulationResult result(final long total) {
        return new SimulationResult(new LoanRequest(VehicleType.MOBIL, VehicleCondition.BEKAS, 2015,
                Rupiah.of(total), 3, Rupiah.of(total / 4)), List.of());
    }

    @Test
    void shouldStartEmpty_whenNothingSaved() {
        final SheetBook book = SheetBook.open(new InMemorySheetRepository());

        assertThat(book.sheets()).isEmpty();
        assertThat(book.current()).isEmpty();
        assertThat(book.loadWarning()).isEmpty();
        assertThat(book.nextDefaultName()).isEqualTo("Sheet1");
    }

    @Test
    void shouldPersistAndBecomeCurrent_whenSaved() {
        final InMemorySheetRepository repository = new InMemorySheetRepository();
        final SheetBook book = SheetBook.open(repository);

        final Sheet sheet = book.save("  mobil baru ", result(100));

        assertThat(sheet.name()).isEqualTo("mobil baru");
        assertThat(book.current()).contains(sheet);
        assertThat(repository.findAll()).containsExactly(sheet);
    }

    @Test
    void shouldUseNextFreeDefaultName_whenNameBlank() {
        final SheetBook book = SheetBook.open(new InMemorySheetRepository());
        book.save("Sheet2", result(100));

        assertThat(book.save("", result(200)).name()).isEqualTo("Sheet3");
        assertThat(book.save(null, result(300)).name()).isEqualTo("Sheet4");
    }

    @Test
    void shouldReplaceInPlace_whenNameExistsIgnoringCase() {
        final SheetBook book = SheetBook.open(new InMemorySheetRepository());
        book.save("a", result(100));
        book.save("b", result(200));

        book.save("A", result(300));

        assertThat(book.sheets()).extracting(Sheet::name).containsExactly("A", "b");
        assertThat(book.sheets().get(0).result()).isEqualTo(result(300));
    }

    @Test
    void shouldStayUnchanged_whenRepositoryCannotWrite() {
        final SheetBook book = SheetBook.open(new InMemorySheetRepository().failingSave());

        assertThatThrownBy(() -> book.save("a", result(100))).isInstanceOf(SheetStorageException.class);
        assertThat(book.sheets()).isEmpty();
        assertThat(book.current()).isEmpty();
    }

    @Test
    void shouldReject_whenNameInvalid() {
        final SheetBook book = SheetBook.open(new InMemorySheetRepository());

        assertThatThrownBy(() -> book.save("a/b", result(100))).isInstanceOf(InvalidSheetNameException.class);
        assertThatThrownBy(() -> book.save("x".repeat(32), result(100)))
                .isInstanceOf(InvalidSheetNameException.class);
    }

    @Test
    void shouldSwitchByPrefix_whenUnique() {
        final SheetBook book = SheetBook.open(new InMemorySheetRepository());
        final Sheet motor = book.save("motor", result(100));
        book.save("mobil", result(200));

        assertThat(book.switchTo("mot").value()).isEqualTo(motor);
        assertThat(book.current()).contains(motor);
        assertThatThrownBy(() -> book.switchTo("mo")).isInstanceOf(InvalidChoiceException.class);
        assertThatThrownBy(() -> book.switchTo("truk")).hasMessage("sheet tidak dikenal: \"truk\"");
    }

    @Test
    void shouldReopenWithLastSheetCurrent_whenRepositoryHasSheets() {
        final InMemorySheetRepository repository = new InMemorySheetRepository();
        final SheetBook first = SheetBook.open(repository);
        first.save("a", result(100));
        final Sheet b = first.save("b", result(200));

        final SheetBook reopened = SheetBook.open(repository);

        assertThat(reopened.sheets()).hasSize(2);
        assertThat(reopened.current()).contains(b);
    }

    @Test
    void shouldStartEmptyWithWarning_whenRepositoryUnreadable() {
        final SheetBook book = SheetBook.open(new InMemorySheetRepository().failingFirstFind());

        assertThat(book.sheets()).isEmpty();
        assertThat(book.loadWarning()).hasValueSatisfying(w -> assertThat(w).contains("file rusak").contains("memori"));
    }
}
