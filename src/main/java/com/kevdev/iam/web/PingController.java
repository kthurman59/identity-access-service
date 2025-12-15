package com.kevdev.iam.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/secure/ping")
    public String securePing() {
        return "pong";
    }

    @GetMapping("/admin/ping")
    public String adminPing() {
        return "pong";
    }
}
