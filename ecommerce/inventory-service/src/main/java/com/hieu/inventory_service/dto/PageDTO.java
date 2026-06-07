package com.hieu.inventory_service.dto;

import java.util.List;

/** Generic pagination wrapper. */
public record PageDTO<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
