package com.bcadigital.creditsimulator.service;

/**
 * Fetches a saved calculation for the {@code load} menu.
 *
 * <p>Abstraction over {@link RemoteLoader} so the controller depends on the
 * contract, not the HTTP implementation. Tests inject a stub here instead of
 * hitting the network.
 */
public interface LoadService {

    /**
     * Loads the saved calculation, falling back to the bundled example when the
     * remote endpoint is unusable.
     *
     * @return the load result, never null
     */
    RemoteLoad load();
}
