package com.gangwon.companion.global.web;

public record LocationResponse(
        Double latitude,
        Double longitude,
        String address
) {
}
