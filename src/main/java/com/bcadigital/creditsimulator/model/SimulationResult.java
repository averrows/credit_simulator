package com.bcadigital.creditsimulator.model;

import java.util.List;
import java.util.Objects;

/**
 * A complete simulation: the request plus one result per loan year.
 *
 * @param request the validated request; never null
 * @param yearlyResults one entry per loan year in order; never null, defensively copied
 */
public record SimulationResult(LoanRequest request, List<YearlyResult> yearlyResults) {

    /**
     * Creates a simulation result.
     */
    public SimulationResult {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(yearlyResults, "yearlyResults");
        yearlyResults = List.copyOf(yearlyResults);
    }
}
