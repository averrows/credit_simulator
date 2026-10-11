package com.bcadigital.creditsimulator.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.bcadigital.creditsimulator.json.MiniJson;
import com.bcadigital.creditsimulator.model.InvalidInputException;
import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.VehicleCondition;
import com.bcadigital.creditsimulator.model.VehicleType;

/**
 * Fetches a saved calculation from the JSON web service for the {@code load} menu.
 *
 * <p>Contract: one {@link #load()} call either returns the validated remote data or,
 * on <em>any</em> remote problem (dead endpoint, non-200 status, malformed JSON,
 * missing keys, rule violations), the validated bundled example from the problem
 * statement — flagging it via {@link RemoteLoad#fallbackUsed()} so the controller
 * warns instead of silently substituting. Uses only the JDK HTTP client plus the
 * internal {@code MiniJson} parser, keeping the zero-dependency rule.
 */
public final class RemoteLoader implements LoadService {

    /** Web-service endpoint from the problem statement. */
    public static final String DEFAULT_URL = "https://run.mocky.io/v3/9108b1da-beec-409e-ae14-e8091955666c";

    /** Bundled example payload from the problem statement, used when the service fails. */
    static final String FALLBACK_JSON = "{"
            + "\"vehicleType\": \"Mobil\","
            + "\"vehicleCondition\": \"Baru\","
            + "\"vehicleYear\": 2025,"
            + "\"totalLoanAmount\": 1000000000,"
            + "\"loanTenure\": 6,"
            + "\"downPayment\": 500000000"
            + "}";

    private final String url;
    private final HttpClient http;
    private final LoanValidation validator;

    /**
     * Creates a loader with an explicit HTTP client (tests inject a stub here).
     *
     * @param url the endpoint; never null
     * @param http the HTTP client; never null
     * @param validator the loan validator; never null
     */
    public RemoteLoader(final String url, final HttpClient http, final LoanValidation validator) {
        this.url = Objects.requireNonNull(url, "url");
        this.http = Objects.requireNonNull(http, "http");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    /**
     * Creates a loader with a default JDK HTTP client (10s timeouts, redirects on).
     *
     * @param url the endpoint; never null
     * @param validator the loan validator; never null
     * @return the loader, never null
     */
    public static RemoteLoader withDefaultClient(final String url, final LoanValidation validator) {
        final HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        return new RemoteLoader(url, http, validator);
    }

    /**
     * Loads and validates the remote calculation, or the bundled example.
     *
     * @return the load result, never null
     * @throws InvalidInputException only when even the bundled example violates
     *     the rules under the validator's clock (a stale-example programmer error,
     *     not a network problem)
     */
    @Override
    public RemoteLoad load() {
        final Optional<String> body = fetchBody();
        if (body.isPresent()) {
            try {
                return new RemoteLoad(buildRequest(MiniJson.parseObject(body.get())), false);
            } catch (final IllegalArgumentException | InvalidInputException e) {
                // Remote data unusable — fall through to the bundled example.
            }
        }
        return new RemoteLoad(buildRequest(MiniJson.parseObject(FALLBACK_JSON)), true);
    }

    private LoanRequest buildRequest(final Map<String, String> values) {
        return validator.validate(
                VehicleType.parse(values.get("vehicleType")).value(),
                VehicleCondition.parse(values.get("vehicleCondition")).value(),
                values.get("vehicleYear"),
                values.get("totalLoanAmount"),
                values.get("loanTenure"),
                values.get("downPayment"));
    }

    private Optional<String> fetchBody() {
        try {
            final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            final HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.statusCode() == 200 ? Optional.ofNullable(response.body()) : Optional.empty();
        } catch (final IOException | IllegalArgumentException e) {
            return Optional.empty();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}
