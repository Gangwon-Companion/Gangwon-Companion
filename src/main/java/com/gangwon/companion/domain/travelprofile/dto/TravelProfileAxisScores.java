package com.gangwon.companion.domain.travelprofile.dto;

import com.gangwon.companion.domain.travelprofile.entity.TravelProfile;

import java.util.Map;

public record TravelProfileAxisScores(
        Map<String, Integer> space,
        Map<String, Integer> activity,
        Map<String, Integer> schedule,
        Map<String, Integer> place
) {
    public TravelProfileAxisScores {
        space = immutable(space);
        activity = immutable(activity);
        schedule = immutable(schedule);
        place = immutable(place);
    }

    public static TravelProfileAxisScores from(TravelProfile profile) {
        if (profile.getSpaceCityScore() == null) return null;
        return new TravelProfileAxisScores(
                Map.of("C", profile.getSpaceCityScore(), "N", profile.getSpaceNatureScore()),
                Map.of("A", profile.getActivityActiveScore(), "R", profile.getActivityRestScore()),
                Map.of("P", profile.getSchedulePlannedScore(), "S", profile.getScheduleSpontaneousScore()),
                Map.of("F", profile.getPlaceFamousScore(), "H", profile.getPlaceHiddenScore())
        );
    }

    public void validate() {
        validateAxis(space, "C", "N");
        validateAxis(activity, "A", "R");
        validateAxis(schedule, "P", "S");
        validateAxis(place, "F", "H");
    }

    private static Map<String, Integer> immutable(Map<String, Integer> values) {
        return values == null ? Map.of() : Map.copyOf(values);
    }

    private static void validateAxis(Map<String, Integer> values, String left, String right) {
        if (values.size() != 2 || !values.keySet().equals(java.util.Set.of(left, right))) {
            throw new IllegalArgumentException("invalid travel profile axis keys");
        }
        int leftScore = values.get(left);
        int rightScore = values.get(right);
        if (leftScore < 0 || rightScore < 0 || leftScore > 100 || rightScore > 100
                || leftScore + rightScore != 100) {
            throw new IllegalArgumentException("travel profile axis scores must total 100");
        }
    }
}
