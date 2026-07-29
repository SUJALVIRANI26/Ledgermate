package com.college.ledgermate.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "message", "LedgerMate backend is running",
                "registerEndpoint", "/api/register",
                "groupEndpoint", "/api/groups"
        );
    }
}

