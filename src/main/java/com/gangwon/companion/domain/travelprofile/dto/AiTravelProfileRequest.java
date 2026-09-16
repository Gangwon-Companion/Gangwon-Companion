package com.gangwon.companion.domain.travelprofile.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
public record AiTravelProfileRequest(@JsonProperty("schema_version") String schemaVersion,
 @JsonProperty("reference_time") Instant referenceTime, List<Search> searches, List<Visit> visits,
 @JsonProperty("saved_courses") List<SavedCourse> savedCourses, List<Review> reviews) {
 public record Search(String keyword, String region, @JsonProperty("searched_at") Instant searchedAt) {}
 public record Visit(@JsonProperty("place_type") String placeType, @JsonProperty("place_id") Long placeId,
                     String name, String category, String region, @JsonProperty("visited_at") Instant visitedAt) {}
 public record SavedCourse(String name, @JsonProperty("saved_at") Instant savedAt, List<Place> places) {}
 public record Place(@JsonProperty("place_type") String placeType, @JsonProperty("place_id") Long placeId, String name, String region) {}
 public record Review(@JsonProperty("place_type") String placeType, @JsonProperty("place_id") Long placeId,
                      String name, Double rating, @JsonProperty("reviewed_at") Instant reviewedAt) {}
}
