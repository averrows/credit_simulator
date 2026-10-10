package com.bcadigital.creditsimulator.view;

import java.io.PrintStream;
import java.util.Objects;

/**
 * All console output of the application.
 *
 * <p>Deliberately thin: the view never validates, never branches on commands, and
 * never imports the model package. It prints exactly what the controller tells it
 * to print, so every decision stays in the controller (the deep module).
 */
public final class ConsoleView {

    private final InputSource input;
    private final PrintStream output;

    /**
     * Creates a view bound to the given input source and output stream.
     *
     * @param input source of user lines; never null
     * @param output destination for all prompts and messages; never null
     */
    public ConsoleView(final InputSource input, final PrintStream output) {
        this.input = Objects.requireNonNull(input, "input");
        this.output = Objects.requireNonNull(output, "output");
    }

    /**
     * Prints a prompt and returns the next input line.
     *
     * @param label the prompt text, printed without a trailing newline
     * @return the entered line, or null on end-of-input
     */
    public String prompt(final String label) {
        output.print(label);
        return input.readLine().orElse(null);
    }

    /**
     * Prints a normal message followed by a newline.
     *
     * @param message the message; never null
     */
    public void showMessage(final String message) {
        output.println(Objects.requireNonNull(message, "message"));
    }

    /**
     * Prints an error message prefixed with {@code Error: }.
     *
     * @param message the error description; never null
     */
    public void showError(final String message) {
        output.println("Error: " + Objects.requireNonNull(message, "message"));
    }
}
