package com.bcadigital.creditsimulator.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Installment result for a single loan year, mirroring one column of the Rumus sheet.
 *
 * @param year the 1-based loan year
 * @param annualRate the rate applied that year as a fraction, e.g. {@code 0.081}; never null
 * @param principal the principal still owed at the start of the year ("Pokok
 *     Pinjaman"); never null
 * @param monthlyInstallment the monthly installment in rupiah (2 decimals); never null
 */
public record YearlyResult(int year, BigDecimal annualRate, BigDecimal principal, BigDecimal monthlyInstallment) {

    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    /**
     * Creates a yearly result.
     */
    public YearlyResult {
        Objects.requireNonNull(annualRate, "annualRate");
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(monthlyInstallment, "monthlyInstallment");
    }

    /**
     * Returns the principal grown by this year's rate ("Total Pinjaman").
     *
     * @return {@code principal × (1 + annualRate)}, unrounded, never null
     */
    public BigDecimal totalWithInterest() {
        return principal.multiply(BigDecimal.ONE.add(annualRate));
    }

    /**
     * Returns what is paid during this year ("installmentYearly").
     *
     * @return twelve monthly installments, never null
     */
    public BigDecimal yearlyInstallment() {
        return monthlyInstallment.multiply(MONTHS_PER_YEAR);
    }

    /**
     * Returns the principal carried into the next year.
     *
     * @return the total with interest minus this year's payments, never null
     */
    public BigDecimal remainingPrincipal() {
        return totalWithInterest().subtract(yearlyInstallment());
    }
}
