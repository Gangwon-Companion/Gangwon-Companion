package com.gangwon.companion.global.web;

import lombok.Getter;

import java.util.List;

@Getter
public abstract class AbstractListResponse<T> {

    private final long totalCount;
    private final List<T> items;

    protected AbstractListResponse(long totalCount, List<T> items) {
        this.totalCount = totalCount;
        this.items = items;
    }
}
