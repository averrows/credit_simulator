package com.bcadigital.creditsimulator.service;

import java.time.Clock;
import java.time.Year;
import java.util.Objects;
import java.util.function.Supplier;

import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.VehicleCondition;
import com.bcadigital.creditsimulator.model.VehicleType;

/**
 * Validates raw loan inputs against Rules of the Game 1-4 and builds the request.
 *
 * <p>This is the single home of every input rule, so callers (interactive prompts,
 * batch files, the web-service loader) share one contract: one {@link #validate}
 * call takes the six raw values and either returns a valid {@link LoanRequest} or
 * throws a specific {@link InvalidLoanInputException} naming the offending field.
 * The clock is injected so tests pin "current year" instead of depending on the wall
 * clock.
 */
public final class LoanValidator {

    /** Maximum total loan amount in rupiah (1 miliar). */
    public static final long MAX_TOTAL_LOAN = 1_000_000_000L;

    /** Minimum loan tenure in years. */
    public static final int MIN_TENURE_YEARS = 1;

    /** Maximum loan tenure in years (Rule 2). */
    public static final int MAX_TENURE_YEARS = 6;

    private static final int NEW_VEHICLE_MIN_DP_PERCENT = 35;
    private static final int USED_VEHICLE_MIN_DP_PERCENT = 25;

    private final Clock clock;

    /**
     * Creates a validator using the given clock for "current year".
     *
     * @param clock the clock; never null
     */
    public LoanValidator(final Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Parses and validates the six raw inputs into a loan request.
     *
     * @param vehicleType the already-parsed vehicle kind; never null
     * @param vehicleCondition the already-parsed condition; never null
     * @param yearRaw the raw year text (4 digits)
     * @param totalRaw the raw total-loan text (1 to 1.000.000.000)
     * @param tenorRaw the raw tenure text (1 to 6)
     * @param downPaymentRaw the raw down-payment text
     * @return a valid request, never null
     * @throws InvalidYearException when the year is not 4 digits, or a new vehicle
     *     is older than {@code currentYear - 1}
     * @throws InvalidTotalLoanException when the total is not numeric or out of range
     * @throws InvalidTenorException when the tenure is not numeric or out of range
     * @throws InvalidDownPaymentException when the DP is not numeric, not less than
     *     the total, or below the condition minimum
     */
    public LoanRequest validate(
            final VehicleType vehicleType,
            final VehicleCondition vehicleCondition,
            final String yearRaw,
            final String totalRaw,
            final String tenorRaw,
            final String downPaymentRaw) {
        Objects.requireNonNull(vehicleType, "vehicleType");
        Objects.requireNonNull(vehicleCondition, "vehicleCondition");
        final int year = parseYear(yearRaw, vehicleCondition);
        final long total = parseTotal(totalRaw);
        final int tenor = parseTenor(tenorRaw);
        final long downPayment = parseDownPayment(downPaymentRaw, vehicleCondition, total);
        return new LoanRequest(vehicleType, vehicleCondition, year, total, tenor, downPayment);
    }

    private int parseYear(final String raw, final VehicleCondition condition) {
        final String digits = requireDigits(raw,
                () -> new InvalidYearException("Tahun kendaraan harus 4 digit angka, contoh: 2025"));
        if (digits.length() != 4) {
            throw new InvalidYearException("Tahun kendaraan harus 4 digit angka, contoh: 2025");
        }
        final int year = Integer.parseInt(digits);
        final int minYear = Year.now(clock).getValue() - 1;
        if (condition == VehicleCondition.BARU && year < minYear) {
            throw new InvalidYearException(
                    "Kendaraan baru: tahun minimal " + minYear + " (tahun ini - 1)");
        }
        return year;
    }

    private long parseTotal(final String raw) {
        final String digits = requireDigits(raw,
                () -> new InvalidTotalLoanException("Jumlah pinjaman harus angka, contoh: 100000000"));
        final long total = parseLong(digits,
                () -> new InvalidTotalLoanException("Jumlah pinjaman harus angka, contoh: 100000000"));
        if (total < 1 || total > MAX_TOTAL_LOAN) {
            throw new InvalidTotalLoanException("Jumlah pinjaman harus 1 sampai 1000000000 (1 miliar)");
        }
        return total;
    }

    private int parseTenor(final String raw) {
        final String digits = requireDigits(raw,
                () -> new InvalidTenorException("Tenor harus angka 1 sampai 6 (tahun)"));
        final long tenor = parseLong(digits,
                () -> new InvalidTenorException("Tenor harus angka 1 sampai 6 (tahun)"));
        if (tenor < MIN_TENURE_YEARS || tenor > MAX_TENURE_YEARS) {
            throw new InvalidTenorException("Tenor harus 1 sampai 6 tahun");
        }
        return (int) tenor;
    }

    private long parseDownPayment(
            final String raw, final VehicleCondition condition, final long total) {
        final String digits = requireDigits(raw,
                () -> new InvalidDownPaymentException("DP harus angka, contoh: 35000000"));
        final long downPayment = parseLong(digits,
                () -> new InvalidDownPaymentException("DP harus angka, contoh: 35000000"));
        if (downPayment >= total) {
            throw new InvalidDownPaymentException("DP harus kurang dari jumlah pinjaman");
        }
        final int requiredPercent = condition == VehicleCondition.BARU
                ? NEW_VEHICLE_MIN_DP_PERCENT
                : USED_VEHICLE_MIN_DP_PERCENT;
        if (downPayment * 100 < (long) requiredPercent * total) {
            throw new InvalidDownPaymentException(
                    "DP minimal " + requiredPercent + "% dari pinjaman (kondisi " + condition.displayName() + ")");
        }
        return downPayment;
    }

    private static String requireDigits(final String raw, final Supplier<InvalidLoanInputException> failure) {
        if (raw == null || !raw.trim().matches("\\d+")) {
            throw failure.get();
        }
        return raw.trim();
    }

    private static long parseLong(final String digits, final Supplier<InvalidLoanInputException> failure) {
        try {
            return Long.parseLong(digits);
        } catch (final NumberFormatException e) {
            throw failure.get();
        }
    }
}
