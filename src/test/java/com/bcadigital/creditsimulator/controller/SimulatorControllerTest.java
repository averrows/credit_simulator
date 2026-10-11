package com.bcadigital.creditsimulator.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.MenuCommand;
import com.bcadigital.creditsimulator.model.SimulationResult;
import com.bcadigital.creditsimulator.model.VehicleCondition;
import com.bcadigital.creditsimulator.model.VehicleType;
import com.bcadigital.creditsimulator.model.YearlyResult;
import com.bcadigital.creditsimulator.service.InstallmentCalculator;
import com.bcadigital.creditsimulator.service.LoadService;
import com.bcadigital.creditsimulator.service.LoanValidation;
import com.bcadigital.creditsimulator.service.LoanValidator;
import com.bcadigital.creditsimulator.service.RemoteLoad;
import com.bcadigital.creditsimulator.service.RemoteLoader;
import com.bcadigital.creditsimulator.service.SimulationCalculator;
import com.bcadigital.creditsimulator.view.ConsoleView;
import com.bcadigital.creditsimulator.view.InputSource;
import com.bcadigital.creditsimulator.view.SimulatorView;

class SimulatorControllerTest {

    private static final Clock PINNED = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC);
    private static final String UNREACHABLE_URL = "http://[::1";

    private static final class QueueInputSource implements InputSource {
        private final Deque<String> lines;

        QueueInputSource(final String... lines) {
            this.lines = new ArrayDeque<>();
            for (final String line : lines) {
                this.lines.add(line);
            }
        }

        @Override
        public Optional<String> readLine() {
            return Optional.ofNullable(lines.poll());
        }
    }

    private static final class Harness {
        final ByteArrayOutputStream captured = new ByteArrayOutputStream();
        final SimulatorController controller;

        /** Real collaborators, pinned clock, and an unreachable endpoint so load falls back. */
        Harness(final boolean batchMode, final String... lines) {
            final ConsoleView view = new ConsoleView(
                    new QueueInputSource(lines),
                    new PrintStream(captured, true, StandardCharsets.UTF_8));
            final LoanValidation validator = new LoanValidator(PINNED);
            this.controller = new SimulatorController(view, batchMode, validator,
                    new InstallmentCalculator(), RemoteLoader.withDefaultClient(UNREACHABLE_URL, validator));
        }

        String output() {
            return captured.toString(StandardCharsets.UTF_8);
        }
    }

    @Test
    void shouldStop_whenExitCommand() {
        final Harness harness = new Harness(false, "exit");

        harness.controller.run();

        assertThat(harness.output()).contains("> ");
    }

    @Test
    void shouldListCommands_whenStarted() {
        final Harness harness = new Harness(false, "exit");

        harness.controller.run();

        assertThat(harness.output()).contains("hitung").contains("load").contains("show").contains("exit");
    }

    @Test
    void shouldShowCommands_whenShowCommand() {
        final Harness harness = new Harness(false, "show", "exit");

        harness.controller.run();

        assertThat(harness.output()).contains("hitung").contains("load").contains("exit");
    }

    @Test
    void shouldReprompt_whenUnknownCommandInInteractiveMode() {
        final Harness harness = new Harness(false, "ngawur", "exit");

        harness.controller.run();

        assertThat(harness.output())
                .contains("Error:")
                .contains("ngawur")
                .contains("hitung");
    }

    @Test
    void shouldAbort_whenUnknownCommandInBatchMode() {
        final Harness harness = new Harness(true, "ngawur", "show", "exit");

        harness.controller.run();

        assertThat(harness.output()).contains("Error:");
        assertThat(harness.output()).doesNotContain("Suku Bunga");
    }

    @Test
    void shouldStop_whenEndOfInput() {
        final Harness harness = new Harness(false);

        harness.controller.run();

        assertThat(harness.output()).contains("> ");
    }

    @Test
    void shouldCalculate_whenFullHitungSession() {
        final Harness harness = new Harness(false,
                "hitung", "mobil", "baru", "2025", "100000000", "3", "35000000", "exit");

        harness.controller.run();

        assertThat(harness.output())
                .contains("tahun 1 : Rp. 1,950,000.00/bln , Suku Bunga : 8%")
                .contains("tahun 2 : Rp. 2,107,950.00/bln , Suku Bunga : 8,1%")
                .contains("tahun 3 : Rp. 2,289,233.70/bln , Suku Bunga : 8,6%");
    }

    @Test
    void shouldCalculate_whenMoneySuffixes() {
        final Harness harness = new Harness(false,
                "hitung", "mobil", "baru", "2025", "100 jt", "3", "35 jt", "exit");

        harness.controller.run();

        assertThat(harness.output()).contains("tahun 1 : Rp. 1,950,000.00/bln");
    }

    @Test
    void shouldAssumeOptions_whenUniquePrefixes() {
        final Harness harness = new Harness(false,
                "hitung", "mot", "bek", "2015", "100000000", "2", "25000000", "exit");

        harness.controller.run();

        assertThat(harness.output())
                .contains("(otomatis: \"mot\" → Motor)")
                .contains("(otomatis: \"bek\" → Bekas)")
                .contains("tahun 1 : Rp. 3,406,250.00/bln , Suku Bunga : 9%")
                .contains("tahun 2 : Rp. 3,716,218.75/bln , Suku Bunga : 9,1%");
    }

    @Test
    void shouldRepromptField_whenYearInvalid() {
        final Harness harness = new Harness(false,
                "hitung", "mobil", "baru", "2020", "2025", "100000000", "3", "35000000", "exit");

        harness.controller.run();

        assertThat(harness.output()).contains("Error:").contains("tahun 1 : Rp. 1,950,000.00/bln");
    }

    @Test
    void shouldRejectImmediately_whenYearNotFourDigits() {
        final Harness harness = new Harness(false,
                "hitung", "mobil", "baru", "2", "2025", "100000000", "3", "35000000", "exit");

        harness.controller.run();

        assertThat(harness.output())
                .contains("Error: Tahun kendaraan harus 4 digit angka")
                .contains("tahun 1 : Rp. 1,950,000.00/bln");
    }

    @Test
    void shouldReportAmbiguity_whenSharedPrefix() {
        final Harness harness = new Harness(false,
                "hitung", "mo", "mobil", "bekas", "2015", "100000000", "2", "25000000", "exit");

        harness.controller.run();

        assertThat(harness.output())
                .contains("\"mo\" ambigu (Mobil atau Motor?)")
                .contains("tahun 1 :");
    }

    @Test
    void shouldAbort_whenInvalidInputInBatchMode() {
        final Harness harness = new Harness(true,
                "hitung", "mobil", "baru", "2020", "2025", "100000000", "3", "35000000", "exit");

        harness.controller.run();

        assertThat(harness.output()).contains("Error:");
        assertThat(harness.output()).doesNotContain("Suku Bunga");
    }

    @Test
    void shouldWarnAndCalculateFallback_whenLoadFails() {
        final Harness harness = new Harness(false, "load", "exit");

        harness.controller.run();

        assertThat(harness.output())
                .contains("(peringatan: web service tidak dapat dihubungi — memakai contoh bawaan)")
                .contains("tahun 1 : Rp. 7,500,000.00/bln , Suku Bunga : 8%")
                .contains("tahun 6 : Rp. 11,423,235.41/bln , Suku Bunga : 9,3%");
    }

    @Test
    void shouldAbortHitung_whenEndOfInput() {
        final Harness harness = new Harness(false);

        assertThat(harness.controller.dispatch(MenuCommand.HITUNG)).isTrue();
        assertThat(harness.output()).contains("Jenis Kendaraan");
    }

    @Test
    void shouldCalculateFallback_whenDispatchLoad() {
        final Harness harness = new Harness(false);

        assertThat(harness.controller.dispatch(MenuCommand.LOAD)).isTrue();
        assertThat(harness.output()).contains("(peringatan:").contains("tahun 6 :");
    }

    @Test
    void shouldReturnFalse_whenDispatchExit() {
        final Harness harness = new Harness(false);

        assertThat(harness.controller.dispatch(MenuCommand.EXIT)).isFalse();
        assertThat(harness.controller.dispatch(MenuCommand.SHOW)).isTrue();
    }

    @Test
    void shouldUseInjectedDoubles_whenDispatchLoad() {
        final LoanRequest request = new LoanRequest(
                VehicleType.MOTOR, VehicleCondition.BEKAS, 2015, 100_000_000L, 2, 25_000_000L);
        final SimulationResult result = new SimulationResult(request, List.of(
                new YearlyResult(1, new BigDecimal("0.09"), new BigDecimal("3406250.00"))));
        final List<SimulationResult> shown = new ArrayList<>();
        final SimulatorView view = new SimulatorView() {
            @Override
            public String prompt(final String label) {
                return null;
            }

            @Override
            public void showMessage(final String message) {
            }

            @Override
            public void showError(final String message) {
            }

            @Override
            public void showResult(final SimulationResult value) {
                shown.add(value);
            }
        };
        final LoanValidation validator = new LoanValidator(PINNED);
        final SimulationCalculator calculator = req -> {
            assertThat(req).isEqualTo(request);
            return result;
        };
        final LoadService loader = () -> new RemoteLoad(request, false);
        final SimulatorController controller =
                new SimulatorController(view, false, validator, calculator, loader);

        assertThat(controller.dispatch(MenuCommand.LOAD)).isTrue();
        assertThat(shown).containsExactly(result);
    }
}
