package com.bcadigital.creditsimulator.service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import com.bcadigital.creditsimulator.model.VehicleType;

/**
 * Creates the interest-rate policy for a vehicle kind (Rule 9: Mobil 8%, Motor 9%).
 *
 * <p>Callers never branch on vehicle type for rates. The mapping lives in a
 * registry, so adding a kind only registers a new entry — the lookup logic in
 * {@link #forType} never changes.
 */
public final class VehicleRateFactory {

    private static final Map<VehicleType, Supplier<RatePolicy>> REGISTRY = new EnumMap<>(VehicleType.class);

    static {
        REGISTRY.put(VehicleType.MOBIL, () -> new SteppedRatePolicy(new BigDecimal("0.08")));
        REGISTRY.put(VehicleType.MOTOR, () -> new SteppedRatePolicy(new BigDecimal("0.09")));
    }

    private VehicleRateFactory() {
    }

    /**
     * Creates the rate policy for the given vehicle kind.
     *
     * @param vehicleType the vehicle kind; never null
     * @return the policy, never null
     */
    public static RatePolicy forType(final VehicleType vehicleType) {
        Objects.requireNonNull(vehicleType, "vehicleType");
        final Supplier<RatePolicy> factory = REGISTRY.get(vehicleType);
        if (factory == null) {
            throw new IllegalStateException("no rate policy registered for: " + vehicleType);
        }
        return factory.get();
    }

    /**
     * Registers the rate policy factory for a vehicle kind.
     *
     * <p>This is the only method to touch when a new kind arrives; {@link #forType}
     * stays closed.
     *
     * @param vehicleType the vehicle kind; never null
     * @param factory supplies the policy; never null
     */
    static void register(final VehicleType vehicleType, final Supplier<RatePolicy> factory) {
        REGISTRY.put(
                Objects.requireNonNull(vehicleType, "vehicleType"),
                Objects.requireNonNull(factory, "factory"));
    }
}
