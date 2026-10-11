package com.bcadigital.creditsimulator;

import java.nio.file.Path;
import java.time.Clock;

import com.bcadigital.creditsimulator.controller.SimulatorController;
import com.bcadigital.creditsimulator.model.VehicleRatePolicyFactory;
import com.bcadigital.creditsimulator.repository.JsonSheetRepository;
import com.bcadigital.creditsimulator.service.InstallmentCalculator;
import com.bcadigital.creditsimulator.service.LoanValidation;
import com.bcadigital.creditsimulator.service.LoanValidator;
import com.bcadigital.creditsimulator.service.RemoteLoader;
import com.bcadigital.creditsimulator.service.SheetBook;
import com.bcadigital.creditsimulator.service.SimulationCalculator;
import com.bcadigital.creditsimulator.view.ConsoleView;
import com.bcadigital.creditsimulator.view.FileInputSource;
import com.bcadigital.creditsimulator.view.StreamInputSource;

/**
 * Entry point and composition root: the only place that names concrete classes.
 * Decides the input source once (keyboard vs. batch file), wires the MVC trio
 * against their interfaces, and hands over to the controller with one {@code run()}.
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
     * <p>Exits with status 1 when a batch file contains invalid or incomplete input.
     *
     * @param args zero arguments for interactive mode, or one batch-file path
     */
    public static void main(final String[] args) {
        final AppArgs.Options options = AppArgs.parse(args);
        if (options.help()) {
            System.out.println("Usage: credit_simulator [file_inputs.txt] [--url URL]");
            return;
        }
        final boolean batchMode = options.file() != null;
        final ConsoleView view;
        if (batchMode) {
            view = new ConsoleView(new FileInputSource(Path.of(options.file())), System.out);
        } else {
            view = new ConsoleView(new StreamInputSource(System.in), System.out);
        }
        final LoanValidation validator = new LoanValidator(Clock.systemDefaultZone());
        final SimulationCalculator calculator = new InstallmentCalculator(new VehicleRatePolicyFactory());
        final SheetBook sheets = SheetBook.open(
                new JsonSheetRepository(Path.of(JsonSheetRepository.DEFAULT_FILE), calculator));
        final boolean ok = new SimulatorController(view, batchMode, validator, calculator,
                RemoteLoader.withDefaultClient(options.apiUrl(), validator), sheets).run();
        if (!ok) {
            System.exit(1);
        }
    }
}
