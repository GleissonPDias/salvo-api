package com.salvo.salvo_api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponseDTO(
    @JsonProperty("access_token")
    String accessToken,

    @JsonProperty("token_type")
    String tokenType,

    @JsonProperty("expires_in")
    Long expiresIn,

    @JsonProperty("refresh_token")
    String refreshToken
) {}
