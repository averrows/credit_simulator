package com.bcadigital.creditsimulator.controller;

/**
 * The behaviour behind one menu command (Command pattern).
 *
 * <p>The controller maps every {@code MenuCommand} to one handler, so the menu loop
 * executes commands without knowing what any of them do.
 */
@FunctionalInterface
interface CommandHandler {

    /**
     * Executes the command, reporting any user error through the view.
     *
     * @return true to keep the menu loop running, false to end the session
     */
    boolean execute();
}
