package com.arn.ycyw.your_car_your_way.services.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtServiceImpl {
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    @Value("${app.jwt.expiration-minutes}")
    private long expirationMinutes;

    public String generateToken(String subject){
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(subject)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .sign(Algorithm.HMAC256(jwtSecret));
    }

    public String extractToken(String token){
        return JWT.require(Algorithm.HMAC256(jwtSecret))
                .build().verify(token).getToken();
    }

    public String extractUsername(String token){
        return JWT
                .require(Algorithm.HMAC256(jwtSecret))
                .build()
                .verify(token)
                .getSubject();
    }
}
