package com.bcadigital.creditsimulator.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VehicleConditionTest {

    @Test
    void shouldMatchExact_whenMixedCaseAndSurroundingWhitespace() {
        assertThat(VehicleCondition.parse("Baru")).contains(new Match<>(VehicleCondition.BARU, false));
        assertThat(VehicleCondition.parse("  BEKAS ")).contains(new Match<>(VehicleCondition.BEKAS, false));
    }

    @Test
    void shouldMatchByPrefix_whenUniquePrefix() {
        assertThat(VehicleCondition.parse("bek")).contains(new Match<>(VehicleCondition.BEKAS, true));
        assertThat(VehicleCondition.parse("bar")).contains(new Match<>(VehicleCondition.BARU, true));
        assertThat(VehicleCondition.parse("be")).contains(new Match<>(VehicleCondition.BEKAS, true));
        assertThat(VehicleCondition.parse("ba")).contains(new Match<>(VehicleCondition.BARU, true));
    }

    @Test
    void shouldReturnEmpty_whenAmbiguousBlankOrUnknown() {
        assertThat(VehicleCondition.parse("b")).isEmpty();
        assertThat(VehicleCondition.parse("seken")).isEmpty();
        assertThat(VehicleCondition.parse("  ")).isEmpty();
        assertThat(VehicleCondition.parse(null)).isEmpty();
    }

    @Test
    void shouldListCandidates_whenSharedPrefix() {
        assertThat(VehicleCondition.candidates("b"))
                .containsExactlyInAnyOrder(VehicleCondition.BARU, VehicleCondition.BEKAS);
        assertThat(VehicleCondition.candidates("bek")).containsExactly(VehicleCondition.BEKAS);
    }
}
