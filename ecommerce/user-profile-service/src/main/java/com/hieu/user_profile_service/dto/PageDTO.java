package com.hieu.user_profile_service.dto;

import lombok.Value;

import java.util.List;

@Value
public class PageDTO<T> {
    List<T> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
}
