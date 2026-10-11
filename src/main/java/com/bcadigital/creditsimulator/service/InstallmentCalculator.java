package com.bcadigital.creditsimulator.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import java.util.function.Function;

import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.SimulationResult;
import com.bcadigital.creditsimulator.model.VehicleType;
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
public final class InstallmentCalculator implements SimulationCalculator {

    private static final int MONTHS_PER_YEAR = 12;
    private static final int MONEY_SCALE = 2;

    private final Function<VehicleType, RatePolicy> rates;

    /**
     * Creates a calculator using {@link VehicleRateFactory} for rate lookup.
     */
    public InstallmentCalculator() {
        this(VehicleRateFactory::forType);
    }

    /**
     * Creates a calculator with an explicit rate lookup (tests inject a stub here).
     *
     * @param rates resolves the policy for a vehicle kind; never null
     */
    public InstallmentCalculator(final Function<VehicleType, RatePolicy> rates) {
        this.rates = Objects.requireNonNull(rates, "rates");
    }

    /**
     * Calculates the full schedule for a validated request.
     *
     * @param request the validated request; never null
     * @return the per-year schedule, never null
     */
    @Override
    public SimulationResult calculate(final LoanRequest request) {
        Objects.requireNonNull(request, "request");
        final RatePolicy policy = rates.apply(request.vehicleType());
        final BigDecimal principal = BigDecimal.valueOf(request.principal());
        final int totalMonths = request.loanTenureYears() * MONTHS_PER_YEAR;
        // Two-arg iterate + limit: next() runs only on years inside the tenor,
        // so monthsLeft never reaches zero.
        final List<YearlyResult> yearly = Stream
                .iterate(
                        YearStep.of(1, principal, policy.annualRate(1), totalMonths),
                        prev -> YearStep.of(
                                prev.year() + 1,
                                prev.remaining(),
                                policy.annualRate(prev.year() + 1),
                                prev.monthsLeft() - MONTHS_PER_YEAR))
                .limit(request.loanTenureYears())
                .map(step -> new YearlyResult(step.year(), step.rate(), step.monthly()))
                .toList();
        return new SimulationResult(request, yearly);
    }

    /**
     * One computed loan year. Pure math over {@code (basis, rate, monthsLeft)} —
     * it knows nothing about rate schedules or tenors; the caller derives the rate
     * per year and counts the months down by 12.
     *
     * @param year the 1-based loan year (carried for the output)
     * @param rate the rate applied that year
     * @param monthly the monthly installment
     * @param remaining the principal left for the next year
     * @param monthsLeft the remaining months this slice was spread over
     */
    private record YearStep(int year, BigDecimal rate, BigDecimal monthly, BigDecimal remaining, int monthsLeft) {

        static YearStep of(final int year, final BigDecimal basis, final BigDecimal rate, final int monthsLeft) {
            final BigDecimal yearTotal = basis.multiply(BigDecimal.ONE.add(rate));
            final BigDecimal monthly = yearTotal.divide(
                    BigDecimal.valueOf(monthsLeft), MONEY_SCALE, RoundingMode.HALF_UP);
            return new YearStep(year, rate, monthly,
                    yearTotal.subtract(monthly.multiply(BigDecimal.valueOf(MONTHS_PER_YEAR))), monthsLeft);
        }
    }
}
