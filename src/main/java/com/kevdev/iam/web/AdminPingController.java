package com.kevdev.iam.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AdminPingController {
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/admin/ping")
  String ping() { return "admin ok"; }
}
