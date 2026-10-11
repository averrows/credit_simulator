package com.bcadigital.creditsimulator;

import java.util.Objects;

import com.bcadigital.creditsimulator.service.RemoteLoader;

/**
 * Parses command-line arguments into options (the only logic {@code App} had).
 *
 * <p>Extracted so {@code main} stays wiring-only and the parsing rules are unit
 * tested: {@code [file] [--url URL]}, {@code -h/--help}, anything else shows usage.
 */
public final class AppArgs {

    /**
     * Parsed options.
     *
     * @param file the batch file, or null for interactive mode; never null when present
     * @param apiUrl the web-service endpoint for {@code load}; never null
     * @param help true when usage should be shown instead of running
     */
    public record Options(String file, String apiUrl, boolean help) {
        /**
         * Creates options.
         */
        public Options {
            Objects.requireNonNull(apiUrl, "apiUrl");
        }
    }

    private AppArgs() {
    }

    /**
     * Parses raw arguments.
     *
     * @param args the raw arguments; never null
     * @return the options, never null
     */
    public static Options parse(final String[] args) {
        Objects.requireNonNull(args, "args");
        String apiUrl = RemoteLoader.DEFAULT_URL;
        String file = null;
        boolean help = false;
        for (int i = 0; i < args.length; i++) {
            if ("--url".equals(args[i]) && i + 1 < args.length) {
                apiUrl = args[++i];
            } else if ("-h".equals(args[i]) || "--help".equals(args[i])
                    || args[i].startsWith("-") || file != null) {
                help = true;
            } else {
                file = args[i];
            }
        }
        return new Options(file, apiUrl, help);
    }
}
