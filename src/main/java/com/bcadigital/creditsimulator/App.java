package com.bcadigital.creditsimulator;

import java.nio.file.Path;

import com.bcadigital.creditsimulator.controller.SimulatorController;
import com.bcadigital.creditsimulator.view.ConsoleView;
import com.bcadigital.creditsimulator.view.FileInputSource;
import com.bcadigital.creditsimulator.view.StreamInputSource;

/**
 * Entry point. Decides the input source once (keyboard vs. batch file), wires the
 * MVC trio, and hands over to the controller with a single {@code run()} call.
 *
 * <p>Usage: {@code credit_simulator} (interactive) or
 * {@code credit_simulator file_inputs.txt} (batch).
 */
public final class App {

    private App() {
    }

    /**
     * Application entry point.
     *
     * @param args zero arguments for interactive mode, or one batch-file path
     */
    public static void main(final String[] args) {
        if (args.length > 1 || (args.length == 1 && ("-h".equals(args[0]) || "--help".equals(args[0])))) {
            System.out.println("Usage: credit_simulator [file_inputs.txt]");
            return;
        }
        final boolean batchMode = args.length == 1;
        final ConsoleView view;
        if (batchMode) {
            view = new ConsoleView(new FileInputSource(Path.of(args[0])), System.out);
        } else {
            view = new ConsoleView(new StreamInputSource(System.in), System.out);
        }
        new SimulatorController(view, batchMode).run();
    }
}
