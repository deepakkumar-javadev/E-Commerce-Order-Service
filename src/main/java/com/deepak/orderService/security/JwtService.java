package com.deepak.orderService.security;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final String SECRET_KEY = "mySecretKeyForMacyEcommerceApplication2026SecureKey";

	public String extractEmail(String token) {

		return extractAllClaims(token).getSubject();
	}

	public Long extractUserId(String token) {

		return extractAllClaims(token).get("userId", Long.class);
	}

	public String extractRole(String token) {

		return extractAllClaims(token).get("role", String.class);
	}

	private Claims extractAllClaims(String token) {

		return Jwts.parser().verifyWith(getSignKey()).build().parseSignedClaims(token).getPayload();
	}

	private SecretKey getSignKey() {

		return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
	}
}
