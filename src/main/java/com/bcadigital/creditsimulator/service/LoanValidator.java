package com.bcadigital.creditsimulator.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
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
public final class LoanValidator implements LoanValidation {

    /** Maximum total loan amount in rupiah (1 miliar). */
    public static final long MAX_TOTAL_LOAN = 1_000_000_000L;

    /** Minimum loan tenure in years. */
    public static final int MIN_TENURE_YEARS = 1;

    /** Maximum loan tenure in years (Rule 2). */
    public static final int MAX_TENURE_YEARS = 6;

    /** Rupiah multiplier per amount suffix. */
    private static final Map<String, Long> SUFFIX_MULTIPLIERS = Map.of(
            "milyar", 1_000_000_000L,
            "miliar", 1_000_000_000L,
            "juta", 1_000_000L,
            "jt", 1_000_000L,
            "ribu", 1_000L,
            "rb", 1_000L,
            "m", 1_000_000_000L);

    /** Suffixes longest-first, so "miliar" matches before "m". */
    private static final List<String> SUFFIX_ORDER =
            List.of("milyar", "miliar", "juta", "ribu", "jt", "rb", "m");

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
     * @param totalRaw the raw total-loan text (1 to 1.000.000.000; plain digits
     *     or with an rb/jt/m suffix)
     * @param tenorRaw the raw tenure text (1 to 6)
     * @param downPaymentRaw the raw down-payment text (plain digits or with an
     *     rb/jt/m suffix)
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

    /**
     * Parses and validates one year input immediately after entry.
     *
     * @param raw the raw year text
     * @param condition the vehicle condition for the Rule 1 check; never null
     * @return the valid year
     * @throws InvalidYearException when not 4 digits, or a new vehicle is too old
     */
    public int parseYear(final String raw, final VehicleCondition condition) {
        final String digits = requireDigits(raw,
                () -> new InvalidYearException("Tahun kendaraan harus 4 digit angka, contoh: 2025"));
        if (digits.length() != 4) {
            throw new InvalidYearException("Tahun kendaraan harus 4 digit angka, contoh: 2025");
        }
        final int year = Integer.parseInt(digits);
        final OptionalInt minYear = condition.minimumYear(Year.now(clock).getValue());
        if (minYear.isPresent() && year < minYear.getAsInt()) {
            throw new InvalidYearException(
                    "Kendaraan " + condition.key() + ": tahun minimal " + minYear.getAsInt());
        }
        return year;
    }

    /**
     * Parses and validates one total-loan input immediately after entry.
     *
     * @param raw the raw total text
     * @return the valid total in rupiah
     * @throws InvalidTotalLoanException when not numeric or out of range
     */
    public long parseTotal(final String raw) {
        final long total = parseMoney(raw,
                () -> new InvalidTotalLoanException(
                        "Jumlah pinjaman harus angka, contoh: 100000000, 100 jt, atau 0,1 m"));
        if (total < 1 || total > MAX_TOTAL_LOAN) {
            throw new InvalidTotalLoanException("Jumlah pinjaman harus 1 sampai 1000000000 (1 miliar)");
        }
        return total;
    }

    /**
     * Parses and validates one tenure input immediately after entry.
     *
     * @param raw the raw tenure text
     * @return the valid tenure in years
     * @throws InvalidTenorException when not numeric or out of range
     */
    public int parseTenor(final String raw) {
        final String digits = requireDigits(raw,
                () -> new InvalidTenorException("Tenor harus angka 1 sampai 6 (tahun)"));
        final long tenor = parseLong(digits,
                () -> new InvalidTenorException("Tenor harus angka 1 sampai 6 (tahun)"));
        if (tenor < MIN_TENURE_YEARS || tenor > MAX_TENURE_YEARS) {
            throw new InvalidTenorException("Tenor harus 1 sampai 6 tahun");
        }
        return (int) tenor;
    }

    /**
     * Parses and validates one down-payment input immediately after entry.
     *
     * @param raw the raw DP text
     * @param condition the vehicle condition for the minimum; never null
     * @param total the already-validated total loan amount
     * @return the valid DP in rupiah
     * @throws InvalidDownPaymentException when not numeric, not less than the
     *     total, or below the condition minimum
     */
    public long parseDownPayment(
            final String raw, final VehicleCondition condition, final long total) {
        final long downPayment = parseMoney(raw,
                () -> new InvalidDownPaymentException("DP harus angka, contoh: 35000000 atau 35 jt"));
        if (downPayment >= total) {
            throw new InvalidDownPaymentException("DP harus kurang dari jumlah pinjaman");
        }
        if (!condition.meetsMinimumDownPayment(downPayment, total)) {
            throw new InvalidDownPaymentException("DP minimal " + condition.minDownPaymentPercent()
                    + "% dari pinjaman (kondisi " + condition.displayName() + ")");
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

    /**
     * Parses a money amount: plain digits ({@code 100000000}) or with an Indonesian
     * magnitude suffix ({@code 500 rb}, {@code 203.35 jt}, {@code 0,1 m}).
     *
     * <p>With a suffix, the last dot/comma is the decimal mark and the other kind
     * (if any) is a thousand separator, so {@code 203.35 jt} and {@code 203,35 jt}
     * both mean 203.350.000. The result must be whole rupiah.
     *
     * @param raw the raw text; may be null
     * @param failure builds the domain error for any bad format
     * @return whole rupiah
     */
    private static long parseMoney(final String raw, final Supplier<InvalidLoanInputException> failure) {
        if (raw == null) {
            throw failure.get();
        }
        String text = raw.trim().toLowerCase(Locale.ROOT);
        boolean suffixed = false;
        BigDecimal multiplier = BigDecimal.ONE;
        for (final String suffix : SUFFIX_ORDER) {
            if (text.endsWith(suffix)) {
                suffixed = true;
                multiplier = BigDecimal.valueOf(SUFFIX_MULTIPLIERS.get(suffix));
                text = text.substring(0, text.length() - suffix.length()).trim();
                break;
            }
        }
        if (!suffixed) {
            if (!text.matches("\\d+")) {
                throw failure.get();
            }
            return parseLong(text, failure);
        }
        final BigDecimal rupiah;
        try {
            rupiah = new BigDecimal(normalizeDecimal(text)).multiply(multiplier);
        } catch (final NumberFormatException e) {
            throw failure.get();
        }
        if (rupiah.stripTrailingZeros().scale() > 0) {
            throw failure.get();
        }
        try {
            return rupiah.longValueExact();
        } catch (final ArithmeticException e) {
            throw failure.get();
        }
    }

    private static String normalizeDecimal(final String text) {
        final boolean hasDot = text.contains(".");
        final boolean hasComma = text.contains(",");
        final String canonical;
        if (hasDot && hasComma) {
            final char decimal = text.lastIndexOf('.') > text.lastIndexOf(',') ? '.' : ',';
            final char thousand = decimal == '.' ? ',' : '.';
            canonical = text.replace(String.valueOf(thousand), "").replace(decimal, '.');
        } else if (hasComma) {
            canonical = text.matches("\\d{1,3}(,\\d{3})+") ? text.replace(",", "") : text.replace(',', '.');
        } else if (hasDot) {
            canonical = text.matches("\\d{1,3}(\\.\\d{3})+") ? text.replace(".", "") : text;
        } else {
            canonical = text;
        }
        if (!canonical.matches("\\d+(\\.\\d+)?")) {
            throw new NumberFormatException("bukan angka: " + text);
        }
        return canonical;
    }
}
