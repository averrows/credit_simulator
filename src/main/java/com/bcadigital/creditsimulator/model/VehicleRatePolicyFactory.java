package com.bcadigital.creditsimulator.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * The production rates (Rule 9): Mobil starts at 8%, Motor at 9%, both stepping
 * yearly as described by {@link SteppedRatePolicy}.
 *
 * <p>The policies are built once and never change, so one instance can be shared freely.
 */
public final class VehicleRatePolicyFactory implements RatePolicyFactory {

    private final Map<VehicleType, RatePolicy> policies = Map.of(
            VehicleType.MOBIL, new SteppedRatePolicy(new BigDecimal("0.08")),
            VehicleType.MOTOR, new SteppedRatePolicy(new BigDecimal("0.09")));

    /**
     * Creates the factory.
     *
     * @throws IllegalStateException when a vehicle kind has no policy (a programming error)
     */
    public VehicleRatePolicyFactory() {
        if (policies.size() != VehicleType.values().length) {
            throw new IllegalStateException("every VehicleType needs a rate policy");
        }
    }

    @Override
    public RatePolicy forType(final VehicleType vehicleType) {
        return policies.get(Objects.requireNonNull(vehicleType, "vehicleType"));
    }
}
