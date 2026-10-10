package com.bcadigital.creditsimulator.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Installment result for a single loan year.
 *
 * @param year the 1-based loan year
 * @param annualRate the rate applied that year as a fraction, e.g. {@code 0.081}; never null
 * @param monthlyInstallment the monthly installment in rupiah (2 decimals); never null
 */
public record YearlyResult(int year, BigDecimal annualRate, BigDecimal monthlyInstallment) {

    /**
     * Creates a yearly result.
     */
    public YearlyResult {
        Objects.requireNonNull(annualRate, "annualRate");
        Objects.requireNonNull(monthlyInstallment, "monthlyInstallment");
    }
}
