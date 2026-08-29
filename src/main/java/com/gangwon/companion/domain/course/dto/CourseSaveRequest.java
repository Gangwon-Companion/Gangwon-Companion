package com.gangwon.companion.domain.course.dto;

import com.gangwon.companion.domain.travel.entity.PlaceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CourseSaveRequest(
        @NotBlank @Size(max = 50) String name,
        @NotEmpty @Size(max = 100) List<@Valid PlaceRequest> places
) {
    public record PlaceRequest(
            @NotNull PlaceType placeType,
            @NotNull @Positive Long placeId,
            @Positive Integer day,
            @Size(max = 100) String name,
            @Size(max = 30) String visitTime,
            @Size(max = 255) String address
    ) {}
}
