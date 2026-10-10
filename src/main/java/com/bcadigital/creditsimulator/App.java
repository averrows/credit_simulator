package com.bcadigital.creditsimulator;

import java.nio.file.Path;
import java.time.Clock;

import com.bcadigital.creditsimulator.controller.SimulatorController;
import com.bcadigital.creditsimulator.service.RemoteLoader;
import com.bcadigital.creditsimulator.view.ConsoleView;
import com.bcadigital.creditsimulator.view.FileInputSource;
import com.bcadigital.creditsimulator.view.StreamInputSource;

/**
 * Entry point. Decides the input source once (keyboard vs. batch file), wires the
 * MVC trio, and hands over to the controller with a single {@code run()} call.
 *
 * <p>Usage: {@code credit_simulator} (interactive) or
 * {@code credit_simulator file_inputs.txt} (batch), with optional
 * {@code --url URL} to override the web-service endpoint for {@code load}.
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
        String apiUrl = RemoteLoader.DEFAULT_URL;
        String file = null;
        for (int i = 0; i < args.length; i++) {
            if ("--url".equals(args[i]) && i + 1 < args.length) {
                apiUrl = args[++i];
            } else if ("-h".equals(args[i]) || "--help".equals(args[i]) || args[i].startsWith("-")) {
                System.out.println("Usage: credit_simulator [file_inputs.txt] [--url URL]");
                return;
            } else if (file == null) {
                file = args[i];
            } else {
                System.out.println("Usage: credit_simulator [file_inputs.txt] [--url URL]");
                return;
            }
        }
        final boolean batchMode = file != null;
        final ConsoleView view;
        if (batchMode) {
            view = new ConsoleView(new FileInputSource(Path.of(file)), System.out);
        } else {
            view = new ConsoleView(new StreamInputSource(System.in), System.out);
        }
        new SimulatorController(view, batchMode, Clock.systemDefaultZone(), apiUrl).run();
    }
}
