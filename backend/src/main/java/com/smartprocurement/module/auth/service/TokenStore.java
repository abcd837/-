package com.smartprocurement.module.auth.service;

import com.smartprocurement.module.auth.dto.LoginResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {

    private final Map<String, LoginResponse> tokens = new ConcurrentHashMap<>();

    public void put(String token, LoginResponse loginResponse) {
        tokens.put(token, loginResponse);
    }

    public LoginResponse get(String token) {
        return tokens.get(token);
    }

    public void remove(String token) {
        tokens.remove(token);
    }
}
