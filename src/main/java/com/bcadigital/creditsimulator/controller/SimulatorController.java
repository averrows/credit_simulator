package com.bcadigital.creditsimulator.controller;

import java.util.Objects;
import java.util.Optional;

import com.bcadigital.creditsimulator.model.Match;
import com.bcadigital.creditsimulator.model.MenuCommand;
import com.bcadigital.creditsimulator.view.ConsoleView;

/**
 * Owns the menu loop: reads commands and dispatches to one handler per command.
 *
 * <p>This is the deep module of the MVC trio: callers make a single {@link #run()}
 * call while the controller hides the whole session (prompting, dispatch, unknown
 * command recovery, end-of-input handling). Calculation and loading logic will be
 * plugged into {@code onHitung()} and {@code onLoad()} in later units; this unit
 * only establishes the command skeleton.
 */
public final class SimulatorController {

    private final ConsoleView view;
    private final boolean batchMode;

    /**
     * Creates a controller.
     *
     * @param view the console view; never null
     * @param batchMode true when input comes from a file (unknown commands abort
     *     instead of re-prompting, since nobody is there to retype)
     */
    public SimulatorController(final ConsoleView view, final boolean batchMode) {
        this.view = Objects.requireNonNull(view, "view");
        this.batchMode = batchMode;
    }

    /**
     * Runs the menu loop until {@code exit} or end-of-input.
     */
    public void run() {
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
        switch (Objects.requireNonNull(command, "command")) {
            case HITUNG:
                onHitung();
                return true;
            case LOAD:
                onLoad();
                return true;
            case SHOW:
                view.showMessage(MenuCommand.describeAll());
                return true;
            case EXIT:
                return false;
            default:
                throw new IllegalStateException("command belum ditangani: " + command);
        }
    }

    private void onHitung() {
        view.showMessage("(hitung: simulasi 6 input — diimplementasikan di unit berikutnya)");
    }

    private void onLoad() {
        view.showMessage("(load: ambil JSON + auto-hitung — diimplementasikan di unit berikutnya)");
    }
}
