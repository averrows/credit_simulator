package com.bcadigital.creditsimulator.view;

import com.bcadigital.creditsimulator.model.SimulationResult;

/**
 * Renders prompts, messages, and results for one simulator session.
 *
 * <p>Abstraction over {@link ConsoleView} so the controller depends on the
 * contract, not the {@code PrintStream} implementation. Tests inject a stub here
 * to assert on rendered output without console I/O.
 */
public interface SimulatorView {

    /**
     * Prints a prompt and returns the next input line.
     *
     * @param label the prompt text, printed without a trailing newline
     * @return the entered line, or null on end-of-input
     */
    String prompt(String label);

    /**
     * Prints a normal message followed by a newline.
     *
     * @param message the message; never null
     */
    void showMessage(String message);

    /**
     * Prints an error message.
     *
     * @param message the error description; never null
     */
    void showError(String message);

    /**
     * Prints one simulation result as per-year lines.
     *
     * @param result the result; never null
     */
    void showResult(SimulationResult result);
}
