package com.bcadigital.creditsimulator.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.SimulationResult;
import com.bcadigital.creditsimulator.model.YearlyResult;

/**
 * Calculates per-year monthly installments with the Rumus sequential flat-interest
 * method: each year's amount is the remaining principal grown by that year's rate,
 * spread over the remaining months.
 *
 * <p>Worked example (Mobil, principal 75jt, tenor 3): year 1 = 75jt × 1,08 / 36 =
 * 2,25jt/bln; the paid yearly slice is subtracted before year 2 grows the rest at
 * 8,1%, and so on. Monthly values round half-up to cents.
 */
public final class InstallmentCalculator {

    private static final int MONTHS_PER_YEAR = 12;
    private static final int MONEY_SCALE = 2;

    /**
     * Calculates the full schedule for a validated request.
     *
     * @param request the validated request; never null
     * @return the per-year schedule, never null
     */
    public SimulationResult calculate(final LoanRequest request) {
        Objects.requireNonNull(request, "request");
        final RatePolicy policy = VehicleRateFactory.forType(request.vehicleType());
        BigDecimal remaining = BigDecimal.valueOf(request.totalLoanAmount() - request.downPayment());
        final int totalMonths = request.loanTenureYears() * MONTHS_PER_YEAR;
        final List<YearlyResult> yearly = new ArrayList<>();
        for (int year = 1; year <= request.loanTenureYears(); year++) {
            final BigDecimal rate = policy.annualRate(year);
            final BigDecimal yearTotal = remaining.multiply(BigDecimal.ONE.add(rate));
            final int monthsLeft = totalMonths - (year - 1) * MONTHS_PER_YEAR;
            final BigDecimal monthly = yearTotal.divide(
                    BigDecimal.valueOf(monthsLeft), MONEY_SCALE, RoundingMode.HALF_UP);
            yearly.add(new YearlyResult(year, rate, monthly));
            remaining = yearTotal.subtract(monthly.multiply(BigDecimal.valueOf(MONTHS_PER_YEAR)));
        }
        return new SimulationResult(request, yearly);
    }
}
