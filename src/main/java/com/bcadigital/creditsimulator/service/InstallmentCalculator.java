package com.bcadigital.creditsimulator.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
        final BigDecimal principal =
                BigDecimal.valueOf(request.totalLoanAmount() - request.downPayment());
        final int totalMonths = request.loanTenureYears() * MONTHS_PER_YEAR;
        final List<YearlyResult> yearly = Stream
                .iterate(
                        YearStep.first(principal, policy, totalMonths),
                        step -> step.year() <= request.loanTenureYears(),
                        step -> step.next(policy, totalMonths))
                .map(step -> new YearlyResult(step.year(), step.rate(), step.monthly()))
                .toList();
        return new SimulationResult(request, yearly);
    }

    /**
     * One computed loan year plus the principal left after its payments.
     *
     * @param year the 1-based loan year
     * @param rate the rate applied that year
     * @param monthly the monthly installment
     * @param remaining the principal left for the next year
     */
    private record YearStep(int year, BigDecimal rate, BigDecimal monthly, BigDecimal remaining) {

        static YearStep first(final BigDecimal principal, final RatePolicy policy, final int totalMonths) {
            return step(1, principal, policy, totalMonths);
        }

        YearStep next(final RatePolicy policy, final int totalMonths) {
            return step(year + 1, remaining, policy, totalMonths);
        }

        private static YearStep step(final int year, final BigDecimal remaining,
                final RatePolicy policy, final int totalMonths) {
            final BigDecimal rate = policy.annualRate(year);
            final BigDecimal yearTotal = remaining.multiply(BigDecimal.ONE.add(rate));
            final int monthsLeft = totalMonths - (year - 1) * MONTHS_PER_YEAR;
            final BigDecimal monthly = yearTotal.divide(
                    BigDecimal.valueOf(monthsLeft), MONEY_SCALE, RoundingMode.HALF_UP);
            return new YearStep(year, rate, monthly,
                    yearTotal.subtract(monthly.multiply(BigDecimal.valueOf(MONTHS_PER_YEAR))));
        }
    }
}
