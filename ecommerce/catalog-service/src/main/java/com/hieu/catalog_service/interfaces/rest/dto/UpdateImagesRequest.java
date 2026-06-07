package com.hieu.catalog_service.interfaces.rest.dto;

import jakarta.validation.constraints.Pattern;

import java.util.List;

public record UpdateImagesRequest(
        @Pattern(regexp = "^https?://[\\w\\-./%?=&:#]+$", message = "thumbnail must be http(s) URL")
        String thumbnail,
        List<@Pattern(regexp = "^https?://[\\w\\-./%?=&:#]+$", message = "image must be http(s) URL") String> images
) {}
