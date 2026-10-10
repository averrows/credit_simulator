package com.bcadigital.creditsimulator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.bcadigital.creditsimulator.model.VehicleCondition;
import com.bcadigital.creditsimulator.model.VehicleType;

class RemoteLoaderTest {

    private static final Clock PINNED = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC);

    private static final String REMOTE_JSON = "{"
            + "\"vehicleType\": \"Motor\","
            + "\"vehicleCondition\": \"Bekas\","
            + "\"vehicleYear\": 2015,"
            + "\"totalLoanAmount\": 100000000,"
            + "\"loanTenure\": 2,"
            + "\"downPayment\": 25000000"
            + "}";

    @SuppressWarnings("unchecked")
    private static RemoteLoader loaderWith(final int status, final String body) throws Exception {
        final HttpClient http = mock(HttpClient.class);
        final HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(http.<String>send(any(HttpRequest.class), any())).thenReturn(response);
        return new RemoteLoader("http://example.test/loan", http, new LoanValidator(PINNED));
    }

    private static RemoteLoader failingLoader() throws Exception {
        final HttpClient http = mock(HttpClient.class);
        when(http.send(any(HttpRequest.class), any())).thenThrow(new IOException("putus"));
        return new RemoteLoader("http://example.test/loan", http, new LoanValidator(PINNED));
    }

    @Test
    void shouldLoadRemote_when200ValidJson() throws Exception {
        final RemoteLoad loaded = loaderWith(200, REMOTE_JSON).load();

        assertThat(loaded.fallbackUsed()).isFalse();
        assertThat(loaded.request().vehicleType()).isEqualTo(VehicleType.MOTOR);
        assertThat(loaded.request().vehicleCondition()).isEqualTo(VehicleCondition.BEKAS);
        assertThat(loaded.request().vehicleYear()).isEqualTo(2015);
        assertThat(loaded.request().totalLoanAmount()).isEqualTo(100_000_000L);
        assertThat(loaded.request().loanTenureYears()).isEqualTo(2);
        assertThat(loaded.request().downPayment()).isEqualTo(25_000_000L);
    }

    @Test
    void shouldFallback_whenNon200() throws Exception {
        final RemoteLoad loaded = loaderWith(404, "not found").load();

        assertThat(loaded.fallbackUsed()).isTrue();
        assertThat(loaded.request().vehicleType()).isEqualTo(VehicleType.MOBIL);
        assertThat(loaded.request().loanTenureYears()).isEqualTo(6);
        assertThat(loaded.request().downPayment()).isEqualTo(500_000_000L);
    }

    @Test
    void shouldFallback_whenTransportFails() throws Exception {
        final RemoteLoad loaded = failingLoader().load();

        assertThat(loaded.fallbackUsed()).isTrue();
        assertThat(loaded.request().vehicleType()).isEqualTo(VehicleType.MOBIL);
    }

    @Test
    void shouldFallback_whenMalformedJson() throws Exception {
        final RemoteLoad loaded = loaderWith(200, "{ngawur").load();

        assertThat(loaded.fallbackUsed()).isTrue();
    }

    @Test
    void shouldFallback_whenRuleViolating() throws Exception {
        final String invalid = REMOTE_JSON.replace("\"downPayment\": 25000000", "\"downPayment\": 1000");

        final RemoteLoad loaded = loaderWith(200, invalid).load();

        assertThat(loaded.fallbackUsed()).isTrue();
    }

    @Test
    void shouldFallback_whenUnknownEnum() throws Exception {
        final String invalid = REMOTE_JSON.replace("\"vehicleType\": \"Motor\"", "\"vehicleType\": \"Pesawat\"");

        final RemoteLoad loaded = loaderWith(200, invalid).load();

        assertThat(loaded.fallbackUsed()).isTrue();
    }
}
