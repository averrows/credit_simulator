package com.bcadigital.creditsimulator.controller;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.bcadigital.creditsimulator.model.Choice;
import com.bcadigital.creditsimulator.model.InvalidChoiceException;
import com.bcadigital.creditsimulator.model.InvalidInputException;
import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.Match;
import com.bcadigital.creditsimulator.model.MenuCommand;
import com.bcadigital.creditsimulator.model.Rupiah;
import com.bcadigital.creditsimulator.model.VehicleCondition;
import com.bcadigital.creditsimulator.model.VehicleType;
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
        final MenuCommand command;
        try {
            command = announced(line, MenuCommand.parse(line));
        } catch (final InvalidChoiceException e) {
            view.showError(e.getMessage());
            view.showMessage(MenuCommand.describeAll());
            return !batchMode;
        }
        return dispatch(command);
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
        try {
            final VehicleType type = ask("Jenis Kendaraan [Mobil|Motor]: ",
                    raw -> announced(raw, VehicleType.parse(raw)));
            final VehicleCondition condition = ask("Kondisi [Baru|Bekas]: ",
                    raw -> announced(raw, VehicleCondition.parse(raw)));
            final int year = ask("Tahun Kendaraan [YYYY]: ", raw -> validator.parseYear(raw, condition));
            final Rupiah total = ask("Jumlah Pinjaman Total: ", validator::parseTotal);
            final int tenor = ask("Tenor Pinjaman (1-6 thn): ", validator::parseTenor);
            final Rupiah downPayment = ask("Jumlah DP: ",
                    raw -> validator.parseDownPayment(raw, condition, total));
            view.showResult(calculator.calculate(
                    new LoanRequest(type, condition, year, total, tenor, downPayment)));
        } catch (final HitungAborted e) {
            // The reason was already shown to the user; the session returns to the menu.
        }
        return true;
    }

    /**
     * Prompts one field until it parses, reporting each error at once.
     *
     * @param <T> the parsed value type
     * @param label the prompt text
     * @param parser parses one entry, throwing {@link InvalidInputException} when invalid
     * @return the valid value, never null
     * @throws HitungAborted on end-of-input, or on the first invalid entry in batch mode
     */
    private <T> T ask(final String label, final Function<String, T> parser) {
        while (true) {
            final String raw = view.prompt(label);
            if (raw == null) {
                if (batchMode) {
                    view.showError("file input tidak lengkap untuk hitung");
                }
                throw new HitungAborted();
            }
            try {
                return parser.apply(raw);
            } catch (final InvalidInputException e) {
                view.showError(e.getMessage());
                if (batchMode) {
                    throw new HitungAborted();
                }
            }
        }
    }

    /**
     * Tells the user when a choice was auto-selected from a prefix.
     *
     * @return the matched value
     */
    private <T extends Choice> T announced(final String raw, final Match<T> match) {
        if (match.assumed()) {
            view.showMessage("(otomatis: \"" + raw.trim() + "\" → " + match.value().displayName() + ")");
        }
        return match.value();
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

    /**
     * Ends one {@code hitung} session early, after the reason was shown to the user.
     * Control flow only, so it skips the stack trace.
     */
    private static final class HitungAborted extends RuntimeException {
        private static final long serialVersionUID = 1L;

        HitungAborted() {
            super(null, null, false, false);
        }
    }
}
