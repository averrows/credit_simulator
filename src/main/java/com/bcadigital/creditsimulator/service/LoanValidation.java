package com.bcadigital.creditsimulator.service;

import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.VehicleCondition;
import com.bcadigital.creditsimulator.model.VehicleType;

/**
 * Validates raw loan inputs against Rules of the Game 1-4.
 *
 * <p>Abstraction over {@link LoanValidator} so the controller and loader depend on
 * the contract, not the concrete validator. One implementation covers interactive
 * prompts, batch files, and the web-service loader.
 */
public interface LoanValidation {

    /**
     * Parses and validates the six raw inputs into a loan request.
     *
     * @param vehicleType the already-parsed vehicle kind; never null
     * @param vehicleCondition the already-parsed condition; never null
     * @param yearRaw the raw year text
     * @param totalRaw the raw total-loan text
     * @param tenorRaw the raw tenure text
     * @param downPaymentRaw the raw down-payment text
     * @return a valid request, never null
     * @throws InvalidLoanInputException when any input violates its rule
     */
    LoanRequest validate(
            VehicleType vehicleType,
            VehicleCondition vehicleCondition,
            String yearRaw,
            String totalRaw,
            String tenorRaw,
            String downPaymentRaw);

    /**
     * Parses and validates one year input.
     *
     * @param raw the raw year text
     * @param condition the vehicle condition; never null
     * @return the valid year
     * @throws InvalidYearException when the year is invalid
     */
    int parseYear(String raw, VehicleCondition condition);

    /**
     * Parses and validates one total-loan input.
     *
     * @param raw the raw total text
     * @return the valid total in rupiah
     * @throws InvalidTotalLoanException when the total is invalid
     */
    long parseTotal(String raw);

    /**
     * Parses and validates one tenure input.
     *
     * @param raw the raw tenure text
     * @return the valid tenure in years
     * @throws InvalidTenorException when the tenure is invalid
     */
    int parseTenor(String raw);

    /**
     * Parses and validates one down-payment input.
     *
     * @param raw the raw DP text
     * @param condition the vehicle condition; never null
     * @param total the already-validated total loan amount
     * @return the valid DP in rupiah
     * @throws InvalidDownPaymentException when the DP is invalid
     */
    long parseDownPayment(String raw, VehicleCondition condition, long total);
}
