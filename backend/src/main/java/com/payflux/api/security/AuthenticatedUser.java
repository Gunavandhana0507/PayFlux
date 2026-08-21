package com.payflux.api.security;

public record AuthenticatedUser(String userId, String email, String role) {}
