package com.bcadigital.creditsimulator.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.bcadigital.creditsimulator.model.MenuCommand;
import com.bcadigital.creditsimulator.view.ConsoleView;
import com.bcadigital.creditsimulator.view.InputSource;

class SimulatorControllerTest {

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

        Harness(final boolean batchMode, final String... lines) {
            final ConsoleView view = new ConsoleView(
                    new QueueInputSource(lines),
                    new PrintStream(captured, true, StandardCharsets.UTF_8));
            this.controller = new SimulatorController(view, batchMode);
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
        assertThat(harness.output()).doesNotContain("(hitung:");
    }

    @Test
    void shouldStop_whenEndOfInput() {
        final Harness harness = new Harness(false);

        harness.controller.run();

        assertThat(harness.output()).contains("> ");
    }

    @Test
    void shouldRunPlaceholders_whenHitungAndLoad() {
        final Harness harness = new Harness(false, "hitung", "load", "exit");

        harness.controller.run();

        assertThat(harness.output()).contains("(hitung:").contains("(load:");
    }

    @Test
    void shouldReturnFalse_whenDispatchExit() {
        final Harness harness = new Harness(false);

        assertThat(harness.controller.dispatch(MenuCommand.EXIT)).isFalse();
        assertThat(harness.controller.dispatch(MenuCommand.SHOW)).isTrue();
    }
}
