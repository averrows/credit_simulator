package com.bcadigital.creditsimulator.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;

import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.Match;
import com.bcadigital.creditsimulator.model.MenuCommand;
import com.bcadigital.creditsimulator.model.VehicleCondition;
import com.bcadigital.creditsimulator.model.VehicleType;
import com.bcadigital.creditsimulator.service.InvalidLoanInputException;
import com.bcadigital.creditsimulator.service.LoadService;
import com.bcadigital.creditsimulator.service.LoanValidation;
import com.bcadigital.creditsimulator.service.RemoteLoad;
import com.bcadigital.creditsimulator.service.SimulationCalculator;
import com.bcadigital.creditsimulator.view.SimulatorView;

/**
 * Owns the menu loop: reads commands and dispatches each to its {@link CommandHandler}.
 *
 * <p>This is the deep module of the MVC trio: callers make a single {@link #run()}
 * call while the controller hides the whole session (prompting, validation retry,
 * dispatch, unknown command recovery, end-of-input handling). It depends only on
 * interfaces; {@code App} is the composition root that picks the implementations.
 */
public final class SimulatorController {

    private final SimulatorView view;
    private final boolean batchMode;
    private final LoanValidation validator;
    private final SimulationCalculator calculator;
    private final LoadService loader;
    private final Map<MenuCommand, CommandHandler> handlers;

    /**
     * Creates a controller over the given collaborators.
     *
     * @param view renders prompts and results; never null
     * @param batchMode true when input comes from a file: validation failures abort
     *     instead of re-prompting, since nobody is there to retype
     * @param validator validates loan inputs; never null
     * @param calculator calculates schedules; never null
     * @param loader fetches saved calculations; never null
     */
    public SimulatorController(final SimulatorView view, final boolean batchMode,
            final LoanValidation validator, final SimulationCalculator calculator,
            final LoadService loader) {
        this.view = Objects.requireNonNull(view, "view");
        this.batchMode = batchMode;
        this.validator = Objects.requireNonNull(validator, "validator");
        this.calculator = Objects.requireNonNull(calculator, "calculator");
        this.loader = Objects.requireNonNull(loader, "loader");
        this.handlers = Map.of(
                MenuCommand.HITUNG, this::onHitung,
                MenuCommand.LOAD, this::onLoad,
                MenuCommand.SHOW, this::onShow,
                MenuCommand.EXIT, () -> false);
        if (handlers.size() != MenuCommand.values().length) {
            throw new IllegalStateException("every MenuCommand needs a handler");
        }
    }

    /**
     * Runs the menu loop until {@code exit} or end-of-input.
     *
     * <p>Lists the available commands first, so users immediately see what they
     * can do without guessing or typing {@code show}.
     */
    public void run() {
        view.showMessage(MenuCommand.describeAll());
        while (true) {
            final String raw = view.prompt("> ");
            if (raw == null) {
                return;
            }
            final boolean keepGoing = handleLine(raw);
            if (!keepGoing) {
                return;
            }
        }
    }

    /**
     * Handles one input line: parses it and dispatches, or reports the error.
     *
     * @param line the raw input line; never null
     * @return false when the loop should stop (exit command, or any unknown command in
     *     batch mode where nobody is there to retype), true otherwise
     */
    boolean handleLine(final String line) {
        final Optional<Match<MenuCommand>> parsed = MenuCommand.parse(line);
        if (parsed.isEmpty()) {
            view.showError("perintah tidak dikenal: \"" + line.trim() + "\"");
            view.showMessage(MenuCommand.describeAll());
            return !batchMode;
        }
        final Match<MenuCommand> match = parsed.get();
        if (match.assumed()) {
            view.showMessage("(otomatis: \"" + line.trim() + "\" → " + match.value().key() + ")");
        }
        return dispatch(match.value());
    }

    /**
     * Dispatches one parsed command.
     *
     * @param command the command; never null
     * @return false when the loop should stop, true otherwise
     */
    boolean dispatch(final MenuCommand command) {
        return handlers.get(Objects.requireNonNull(command, "command")).execute();
    }

    private boolean onHitung() {
        final VehicleType type = promptChoice("Jenis Kendaraan [Mobil|Motor]: ",
                VehicleType::parse, VehicleType::candidates, VehicleType::displayName);
        if (type == null) {
            return true;
        }
        final VehicleCondition condition = promptChoice("Kondisi [Baru|Bekas]: ",
                VehicleCondition::parse, VehicleCondition::candidates, VehicleCondition::displayName);
        if (condition == null) {
            return true;
        }
        final Integer year = promptValidated("Tahun Kendaraan [YYYY]: ",
                raw -> validator.parseYear(raw, condition));
        if (year == null) {
            return true;
        }
        final Long total = promptValidated("Jumlah Pinjaman Total: ", validator::parseTotal);
        if (total == null) {
            return true;
        }
        final Integer tenor = promptValidated("Tenor Pinjaman (1-6 thn): ", validator::parseTenor);
        if (tenor == null) {
            return true;
        }
        final Long downPayment = promptValidated("Jumlah DP: ",
                raw -> validator.parseDownPayment(raw, condition, total));
        if (downPayment == null) {
            return true;
        }
        view.showResult(calculator.calculate(new LoanRequest(type, condition, year, total, tenor, downPayment)));
        return true;
    }

    /**
     * Prompts one numeric field until it validates, reporting the error at once.
     *
     * @param <T> the parsed value type
     * @param label the prompt text
     * @param parser parses and validates one entry, throwing on invalid input
     * @return the valid value, or null on end-of-input or batch-mode abort
     */
    private <T> T promptValidated(final String label, final Function<String, T> parser) {
        while (true) {
            final String raw = view.prompt(label);
            if (raw == null) {
                if (batchMode) {
                    view.showError("file input tidak lengkap untuk hitung");
                }
                return null;
            }
            try {
                return parser.apply(raw);
            } catch (final InvalidLoanInputException e) {
                view.showError(e.getMessage());
                if (batchMode) {
                    return null;
                }
            }
        }
    }

    /**
     * Prompts until the user picks a known option, accepting unique prefixes.
     *
     * @param <T> the option type
     * @param label the prompt text
     * @param parser lenient parsing of one line
     * @param candidatesOf prefix candidates for ambiguity reports
     * @param displayOf display name of one option
     * @return the choice, or null on end-of-input or batch-mode abort
     */
    private <T> T promptChoice(final String label,
            final Function<String, Optional<Match<T>>> parser,
            final Function<String, List<T>> candidatesOf,
            final Function<T, String> displayOf) {
        while (true) {
            final String raw = view.prompt(label);
            if (raw == null) {
                if (batchMode) {
                    view.showError("file input tidak lengkap untuk hitung");
                }
                return null;
            }
            final Optional<Match<T>> parsed = parser.apply(raw);
            if (parsed.isPresent()) {
                final Match<T> match = parsed.get();
                if (match.assumed()) {
                    view.showMessage("(otomatis: \"" + raw.trim() + "\" → " + displayOf.apply(match.value()) + ")");
                }
                return match.value();
            }
            final List<T> candidates = candidatesOf.apply(raw);
            if (candidates.size() > 1) {
                final StringJoiner joiner = new StringJoiner(" atau ");
                for (final T candidate : candidates) {
                    joiner.add(displayOf.apply(candidate));
                }
                view.showError("\"" + raw.trim() + "\" ambigu (" + joiner + "?)");
            } else {
                view.showError("pilihan tidak dikenal: \"" + raw.trim() + "\"");
            }
            if (batchMode) {
                return null;
            }
        }
    }

    private boolean onLoad() {
        final RemoteLoad loaded = loader.load();
        if (loaded.fallbackUsed()) {
            view.showMessage("(peringatan: web service tidak dapat dihubungi — memakai contoh bawaan)");
        }
        view.showResult(calculator.calculate(loaded.request()));
        return true;
    }

    private boolean onShow() {
        view.showMessage(MenuCommand.describeAll());
        return true;
    }
}
