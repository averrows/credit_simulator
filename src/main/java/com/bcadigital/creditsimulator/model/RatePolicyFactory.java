package com.bcadigital.creditsimulator.model;

/**
 * Creates the interest-rate {@link RatePolicy} for a vehicle kind (Factory pattern).
 *
 * <p>Callers never branch on vehicle type to find rates; they ask the factory.
 * The calculator receives a factory through its constructor, so tests can supply
 * their own rates without touching shared state.
 */
public interface RatePolicyFactory {

    /**
     * Returns the rate policy for the given vehicle kind.
     *
     * @param vehicleType the vehicle kind; never null
     * @return the policy, never null
     */
    RatePolicy forType(VehicleType vehicleType);
}
