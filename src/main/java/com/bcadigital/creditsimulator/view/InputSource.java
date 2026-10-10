package com.bcadigital.creditsimulator.view;

import java.util.Optional;

/**
 * Source of input lines, hiding whether the application runs interactively
 * (keyboard) or in batch mode (file).
 *
 * <p>The controller only calls {@link #readLine()}; which implementation is wired
 * is decided once in {@code App}, so no {@code if (fileMode)} checks leak into
 * the menu loop.
 */
public interface InputSource {

    /**
     * Reads the next input line.
     *
     * @return the line without the line terminator, or empty on end-of-input
     */
    Optional<String> readLine();
}
