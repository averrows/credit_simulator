package com.bcadigital.creditsimulator.controller;

import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.Match;
import com.bcadigital.creditsimulator.model.MenuCommand;
import com.bcadigital.creditsimulator.model.VehicleCondition;
import com.bcadigital.creditsimulator.model.VehicleType;
import com.bcadigital.creditsimulator.service.InstallmentCalculator;
import com.bcadigital.creditsimulator.service.InvalidLoanInputException;
import com.bcadigital.creditsimulator.service.LoanValidator;
import com.bcadigital.creditsimulator.service.RemoteLoad;
import com.bcadigital.creditsimulator.service.RemoteLoader;
import com.bcadigital.creditsimulator.view.ConsoleView;

/**
 * Owns the menu loop: reads commands and dispatches to one handler per command.
 *
 * <p>This is the deep module of the MVC trio: callers make a single {@link #run()}
 * call while the controller hides the whole session (prompting, validation retry,
 * dispatch, unknown command recovery, end-of-input handling). The web-service loader
 * will be plugged into {@code onLoad()} in a later unit.
 */
public final class SimulatorController {

    private final ConsoleView view;
    private final boolean batchMode;
    private final LoanValidator validator;
    private final InstallmentCalculator calculator;
    private final RemoteLoader loader;
    private final Map<MenuCommand, BooleanSupplier> handlers;

    /**
     * Creates a controller using the system clock.
     *
     * @param view the console view; never null
     * @param batchMode true when input comes from a file (validation failures abort
     *     instead of re-prompting, since nobody is there to retype)
     */
    public SimulatorController(final ConsoleView view, final boolean batchMode) {
        this(view, batchMode, Clock.systemDefaultZone());
    }

    /**
     * Creates a controller with an explicit clock (tests pin "current year" here).
     *
     * @param view the console view; never null
     * @param batchMode true for file mode; never re-prompts
     * @param clock the clock for year validation; never null
     */
    SimulatorController(final ConsoleView view, final boolean batchMode, final Clock clock) {
        this(view, batchMode, clock, RemoteLoader.DEFAULT_URL);
    }

    /**
     * Creates a controller with an explicit clock and API endpoint (tests pin both here).
     *
     * @param view the console view; never null
     * @param batchMode true for file mode; never re-prompts
     * @param clock the clock for year validation; never null
     * @param apiUrl the web-service endpoint for {@code load}; never null
     */
    public SimulatorController(final ConsoleView view, final boolean batchMode,
            final Clock clock, final String apiUrl) {
        this.view = Objects.requireNonNull(view, "view");
        this.batchMode = batchMode;
        Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(apiUrl, "apiUrl");
        this.validator = new LoanValidator(clock);
        this.calculator = new InstallmentCalculator();
        this.loader = RemoteLoader.withDefaultClient(apiUrl, validator);
        this.handlers = new EnumMap<>(MenuCommand.class);
        handlers.put(MenuCommand.HITUNG, () -> {
            onHitung();
            return true;
        });
        handlers.put(MenuCommand.LOAD, () -> {
            onLoad();
            return true;
        });
        handlers.put(MenuCommand.SHOW, () -> {
            view.showMessage(MenuCommand.describeAll());
            return true;
        });
        handlers.put(MenuCommand.EXIT, () -> false);
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
        Objects.requireNonNull(command, "command");
        final BooleanSupplier handler = handlers.get(command);
        if (handler == null) {
            throw new IllegalStateException("no handler registered for: " + command);
        }
        return handler.getAsBoolean();
    }

    /**
     * Registers the handler for a command. Adding a command only adds one
     * registration — the lookup in {@link #dispatch} never changes.
     *
     * @param command the command; never null
     * @param handler returns false when the loop should stop; never null
     */
    void registerHandler(final MenuCommand command, final BooleanSupplier handler) {
        handlers.put(
                Objects.requireNonNull(command, "command"),
                Objects.requireNonNull(handler, "handler"));
    }

    private void onHitung() {
        final VehicleType type = promptChoice("Jenis Kendaraan [Mobil|Motor]: ",
                VehicleType::parse, VehicleType::candidates, VehicleType::displayName);
        if (type == null) {
            return;
        }
        final VehicleCondition condition = promptChoice("Kondisi [Baru|Bekas]: ",
                VehicleCondition::parse, VehicleCondition::candidates, VehicleCondition::displayName);
        if (condition == null) {
            return;
        }
        final Integer year = promptValidated("Tahun Kendaraan [YYYY]: ",
                raw -> validator.parseYear(raw, condition));
        if (year == null) {
            return;
        }
        final Long total = promptValidated("Jumlah Pinjaman Total: ", validator::parseTotal);
        if (total == null) {
            return;
        }
        final Integer tenor = promptValidated("Tenor Pinjaman (1-6 thn): ", validator::parseTenor);
        if (tenor == null) {
            return;
        }
        final Long downPayment = promptValidated("Jumlah DP: ",
                raw -> validator.parseDownPayment(raw, condition, total));
        if (downPayment == null) {
            return;
        }
        view.showResult(calculator.calculate(new LoanRequest(type, condition, year, total, tenor, downPayment)));
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

    private void onLoad() {
        final RemoteLoad loaded = loader.load();
        if (loaded.fallbackUsed()) {
            view.showMessage("(peringatan: web service tidak dapat dihubungi — memakai contoh bawaan)");
        }
        view.showResult(calculator.calculate(loaded.request()));
    }
}
