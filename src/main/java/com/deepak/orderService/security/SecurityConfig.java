package com.deepak.orderService.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http

				// =========================
				// CSRF
				// =========================
				.csrf(csrf -> csrf.disable())

				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				.authorizeHttpRequests(auth -> auth

						// =========================
						// ADMIN ONLY
						// =========================
						.requestMatchers("/orders/*/status").hasRole("ADMIN")

						// =========================
						// CUSTOMER ONLY
						// =========================
						.requestMatchers("/orders/placeOrder", "/orders/*/cancel").hasRole("CUSTOMER")

						// =========================
						// CUSTOMER + ADMIN
						// =========================
						.requestMatchers("/orders/user/**", "/orders/*/items").hasAnyRole("CUSTOMER", "ADMIN")

						// Swagger
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
						// =========================
						// INTERNAL / FEIGN
						// JWT REQUIRED
						// =========================
						.requestMatchers("/orders/*/payment-status", "/orders/*/payment-order-status").authenticated()

						// =========================
						// PUBLIC / INTERNAL
						// Feign call
						// =========================
						.requestMatchers("/orders/getorder/**").permitAll()

						// =========================
						// EVERYTHING ELSE
						// =========================
						.anyRequest().authenticated())

				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}