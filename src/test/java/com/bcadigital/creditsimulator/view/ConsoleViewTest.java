package com.bcadigital.creditsimulator.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.SimulationResult;
import com.bcadigital.creditsimulator.model.VehicleCondition;
import com.bcadigital.creditsimulator.model.VehicleType;
import com.bcadigital.creditsimulator.model.YearlyResult;

class ConsoleViewTest {

    private static ConsoleView viewWith(final ByteArrayOutputStream captured, final String... lines) {
        final java.util.Deque<String> queue = new java.util.ArrayDeque<>(List.of(lines));
        final InputSource input = () -> Optional.ofNullable(queue.poll());
        return new ConsoleView(input, new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @Test
    void shouldFormatResultLines_whenShowResult() {
        final ByteArrayOutputStream captured = new ByteArrayOutputStream();
        final ConsoleView view = viewWith(captured);
        final SimulationResult result = new SimulationResult(
                new LoanRequest(VehicleType.MOBIL, VehicleCondition.BEKAS, 2015, 100_000_000L, 2, 25_000_000L),
                List.of(
                        new YearlyResult(1, new BigDecimal("0.08"), new BigDecimal("2250000.00")),
                        new YearlyResult(2, new BigDecimal("0.081"), new BigDecimal("2432250.00"))));

        view.showResult(result);

        assertThat(captured.toString(StandardCharsets.UTF_8))
                .contains("tahun 1 : Rp. 2,250,000.00/bln , Suku Bunga : 8%")
                .contains("tahun 2 : Rp. 2,432,250.00/bln , Suku Bunga : 8,1%");
    }

    @Test
    void shouldReturnLine_whenPrompt() {
        final ByteArrayOutputStream captured = new ByteArrayOutputStream();
        final ConsoleView view = viewWith(captured, "mobil");

        assertThat(view.prompt("> ")).isEqualTo("mobil");
        assertThat(captured.toString(StandardCharsets.UTF_8)).contains("> ");
    }

    @Test
    void shouldReturnNull_whenEndOfInput() {
        final ByteArrayOutputStream captured = new ByteArrayOutputStream();
        final ConsoleView view = viewWith(captured);

        assertThat(view.prompt("> ")).isNull();
    }

    @Test
    void shouldPrefixError_whenShowError() {
        final ByteArrayOutputStream captured = new ByteArrayOutputStream();
        final ConsoleView view = viewWith(captured);

        view.showError("rusak");

        assertThat(captured.toString(StandardCharsets.UTF_8)).contains("Error: rusak");
    }
}
