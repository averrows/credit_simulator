package com.bcadigital.creditsimulator.controller;

import java.util.Objects;
import java.util.Optional;

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
            final Optional<MenuCommand> parsed = MenuCommand.parse(raw);
            if (parsed.isEmpty()) {
                view.showError("perintah tidak dikenal: \"" + raw.trim() + "\"");
                view.showMessage(MenuCommand.describeAll());
                if (batchMode) {
                    return;
                }
                continue;
            }
            final boolean keepGoing = dispatch(parsed.get());
            if (!keepGoing) {
                return;
            }
        }
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
