package com.deepak.orderService.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

	@Value("${internal.service.token}")
	private String internalServiceToken;

	@Bean
	public RequestInterceptor requestInterceptor() {

		return requestTemplate -> {

			ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
					.getRequestAttributes();

			if (attributes != null) {

				HttpServletRequest request = attributes.getRequest();

				String authorization = request.getHeader("Authorization");

				System.out.println("Feign Authorization = " + authorization);

				if (authorization != null) {

					requestTemplate.header("Authorization", authorization);
				}
			}
		};
	};

}