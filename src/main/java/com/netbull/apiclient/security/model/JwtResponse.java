package com.netbull.apiclient.security.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class JwtResponse {

    private final String jwtToken;
}
