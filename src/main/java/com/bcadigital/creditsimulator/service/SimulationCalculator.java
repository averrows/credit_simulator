package com.bcadigital.creditsimulator.service;

import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.SimulationResult;

/**
 * Calculates a payment schedule for a validated loan request.
 *
 * <p>Abstraction over {@link InstallmentCalculator} so the controller depends on
 * the contract, not the Rumus implementation.
 */
public interface SimulationCalculator {

    /**
     * Calculates the full schedule for a validated request.
     *
     * @param request the validated request; never null
     * @return the per-year schedule, never null
     */
    SimulationResult calculate(LoanRequest request);
}
