package com.bcadigital.creditsimulator.view;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;

import com.bcadigital.creditsimulator.model.SimulationResult;
import com.bcadigital.creditsimulator.model.YearlyResult;

/**
 * All console output of the application.
 *
 * <p>Deliberately thin: the view never validates and never branches. It renders what
 * the controller hands it (including read-only model data), so every decision stays
 * in the controller (the deep module).
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

    /**
     * Prints one simulation result as per-year lines, following the spec sample:
     * {@code tahun 1 : Rp. 2,250,000.00/bln , Suku Bunga : 8%}.
     *
     * @param result the result; never null
     */
    public void showResult(final SimulationResult result) {
        Objects.requireNonNull(result, "result");
        for (final YearlyResult yearly : result.yearlyResults()) {
            output.println("tahun " + yearly.year() + " : Rp. " + formatMoney(yearly.monthlyInstallment())
                    + "/bln , Suku Bunga : " + formatRate(yearly.annualRate()));
        }
    }

    private static String formatMoney(final BigDecimal amount) {
        final DecimalFormat format = new DecimalFormat("#,##0.00",
                DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(amount);
    }

    private static String formatRate(final BigDecimal rate) {
        return rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString()
                .replace('.', ',') + "%";
    }
}
