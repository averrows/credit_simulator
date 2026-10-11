package com.bcadigital.creditsimulator.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * An immutable, non-negative amount of whole rupiah (value object).
 *
 * <p>Owns everything the application knows about money input: Indonesian magnitude
 * suffixes ({@code 500 rb}, {@code 203.35 jt}, {@code 0,1 m}) and both dot and comma
 * as decimal mark. Callers compare and combine amounts through intention-revealing
 * methods instead of raw {@code long} arithmetic.
 *
 * @param amount the amount in whole rupiah; never negative
 */
public record Rupiah(long amount) implements Comparable<Rupiah> {

    /** Magnitude suffixes, longest first so "miliar" wins over "m". */
    private static final List<Suffix> SUFFIXES = List.of(
            new Suffix("milyar", 1_000_000_000L),
            new Suffix("miliar", 1_000_000_000L),
            new Suffix("juta", 1_000_000L),
            new Suffix("ribu", 1_000L),
            new Suffix("jt", 1_000_000L),
            new Suffix("rb", 1_000L),
            new Suffix("m", 1_000_000_000L));

    private record Suffix(String text, long multiplier) {
    }

    /**
     * Creates an amount.
     *
     * @param amount the amount in whole rupiah
     * @throws IllegalArgumentException when {@code amount} is negative
     */
    public Rupiah {
        if (amount < 0) {
            throw new IllegalArgumentException("rupiah tidak boleh negatif: " + amount);
        }
    }

    /**
     * Creates an amount; shorthand for the constructor.
     *
     * @param amount the amount in whole rupiah; never negative
     * @return the amount, never null
     */
    public static Rupiah of(final long amount) {
        return new Rupiah(amount);
    }

    /**
     * Parses user input: plain digits ({@code 100000000}) or a number with a magnitude
     * suffix. With a suffix, the last dot/comma is the decimal mark and the other kind
     * (if any) a thousand separator, so {@code 203.35 jt} and {@code 203,35 jt} both
     * mean 203.350.000.
     *
     * @param raw the raw text; may be null
     * @return the amount, or empty when the text is not a whole, representable
     *     number of rupiah (bad format, fractional rupiah, or overflow)
     */
    public static Optional<Rupiah> parse(final String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        final String text = raw.trim().toLowerCase(Locale.ROOT);
        for (final Suffix suffix : SUFFIXES) {
            if (text.endsWith(suffix.text())) {
                final String number = text.substring(0, text.length() - suffix.text().length()).trim();
                return parseScaled(number, suffix.multiplier());
            }
        }
        return text.matches("\\d+") ? parseScaled(text, 1) : Optional.empty();
    }

    /**
     * Returns this amount minus another.
     *
     * @param other the amount to subtract; never null and not larger than this
     * @return the difference, never null
     * @throws IllegalArgumentException when {@code other} is larger than this
     */
    public Rupiah minus(final Rupiah other) {
        return new Rupiah(amount - other.amount);
    }

    /**
     * Tells whether this amount is strictly greater than another.
     *
     * @param other the amount to compare with; never null
     * @return true when this is greater
     */
    public boolean isGreaterThan(final Rupiah other) {
        return amount > other.amount;
    }

    /**
     * Tells whether this amount is at least the given percentage of a whole.
     *
     * @param percent the percentage, e.g. {@code 35}; 0 to 100
     * @param whole the reference amount; never null
     * @return true when {@code this >= percent% of whole}, computed without rounding
     */
    public boolean isAtLeastPercentOf(final int percent, final Rupiah whole) {
        return BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(100))
                .compareTo(BigDecimal.valueOf(whole.amount).multiply(BigDecimal.valueOf(percent))) >= 0;
    }

    /**
     * Returns the amount for exact decimal arithmetic.
     *
     * @return the amount as a scale-0 decimal, never null
     */
    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(amount);
    }

    @Override
    public int compareTo(final Rupiah other) {
        return Long.compare(amount, other.amount);
    }

    private static Optional<Rupiah> parseScaled(final String number, final long multiplier) {
        final Optional<String> canonical = canonicalDecimal(number);
        if (canonical.isEmpty()) {
            return Optional.empty();
        }
        final BigDecimal rupiah = new BigDecimal(canonical.get()).multiply(BigDecimal.valueOf(multiplier));
        if (rupiah.stripTrailingZeros().scale() > 0 || rupiah.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
            return Optional.empty();
        }
        return Optional.of(new Rupiah(rupiah.longValueExact()));
    }

    /**
     * Rewrites a localized number as {@code digits[.digits]}, or empty when it is not one.
     */
    private static Optional<String> canonicalDecimal(final String text) {
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
        return canonical.matches("\\d+(\\.\\d+)?") ? Optional.of(canonical) : Optional.empty();
    }
}
