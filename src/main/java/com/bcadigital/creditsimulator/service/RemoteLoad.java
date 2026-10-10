package com.bcadigital.creditsimulator.service;

import java.util.Objects;

import com.bcadigital.creditsimulator.model.LoanRequest;

/**
 * A calculation fetched via {@code load}: the request plus whether it came from
 * the bundled example instead of the network.
 *
 * @param request the validated request; never null
 * @param fallbackUsed true when the web service failed and the bundled example
 *     from the problem statement was used instead
 */
public record RemoteLoad(LoanRequest request, boolean fallbackUsed) {

    /**
     * Creates a remote load result.
     */
    public RemoteLoad {
        Objects.requireNonNull(request, "request");
    }
}
