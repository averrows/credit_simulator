package com.bcadigital.creditsimulator.service;

import java.math.BigDecimal;
import java.util.Objects;

import com.bcadigital.creditsimulator.model.VehicleType;

/**
 * Creates the interest-rate policy for a vehicle kind (Rule 9: Mobil 8%, Motor 9%).
 *
 * <p>Callers never branch on vehicle type for rates; adding a kind later touches
 * only this factory.
 */
public final class VehicleRateFactory {

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
        return switch (vehicleType) {
            case MOBIL -> new SteppedRatePolicy(new BigDecimal("0.08"));
            case MOTOR -> new SteppedRatePolicy(new BigDecimal("0.09"));
        };
    }
}
