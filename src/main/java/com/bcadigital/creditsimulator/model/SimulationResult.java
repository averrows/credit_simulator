package com.bcadigital.creditsimulator.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * Returns the average monthly installment over the whole tenor ("Monthly Rata2").
     *
     * @return all yearly payments divided by the number of months, rounded half-up
     *     to cents; zero when there are no years
     */
    public BigDecimal averageMonthlyInstallment() {
        if (yearlyResults.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        final BigDecimal paid = yearlyResults.stream()
                .map(YearlyResult::yearlyInstallment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return paid.divide(BigDecimal.valueOf(12L * yearlyResults.size()), 2, RoundingMode.HALF_UP);
    }
}
