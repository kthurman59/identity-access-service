package com.kevdev.iam.it;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevdev.iam.security.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.profiles.active=test"
})
class AuthFlowIT {

  @Container
  static final PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("iam")
      .withUsername("postgres")
      .withPassword("postgres");

  @DynamicPropertySource
  static void dbProps(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", pg::getJdbcUrl);
    r.add("spring.datasource.username", pg::getUsername);
    r.add("spring.datasource.password", pg::getPassword);
  }

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate rest;

  @Autowired
  RefreshTokenService refreshTokenService;

  final ObjectMapper om = new ObjectMapper();

  private HttpHeaders headers() {
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    h.add("X-Tenant-Key", "demo");
    return h;
  }

  private String baseUrl() {
    return "http://localhost:" + port;
  }

  private Map<String, String> login() throws Exception {
    String body = "{\"username\":\"admin\",\"password\":\"Admin123!\"}";
    ResponseEntity<String> resp = rest.postForEntity(baseUrl() + "/auth/login",
        new HttpEntity<>(body, headers()), String.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    return om.readValue(resp.getBody(), new TypeReference<>() {});
  }

  private ResponseEntity<String> refresh(String refreshToken) {
    String body = "{\"refreshToken\":\"" + refreshToken + "\"}";
    return rest.postForEntity(baseUrl() + "/auth/refresh",
        new HttpEntity<>(body, headers()), String.class);
  }

  @Test
  void login_returns_tokens() throws Exception {
    Map<String, String> t = login();
    assertThat(t.get("accessToken")).isNotBlank();
    assertThat(t.get("refreshToken")).isNotBlank();
  }

  @Test
  void refresh_rotates_and_old_is_rejected() throws Exception {
    Map<String, String> t1 = login();
    String oldRefresh = t1.get("refreshToken");

    ResponseEntity<String> first = refresh(oldRefresh);
    assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, String> t2 = om.readValue(first.getBody(), new TypeReference<>() {});
    String newRefresh = t2.get("refreshToken");
    assertThat(newRefresh).isNotBlank();
    assertThat(newRefresh).isNotEqualTo(oldRefresh);

    ResponseEntity<String> reuse = refresh(oldRefresh);
    assertThat(reuse.getStatusCode().is4xxClientError()).isTrue();
  }

  @Test
  void revoke_blocks_refresh() throws Exception {
    Map<String, String> t1 = login();
    String refreshTok = t1.get("refreshToken");

    // service level revoke to avoid guessing an extra endpoint path
    refreshTokenService.revoke("demo", refreshTok);

    ResponseEntity<String> resp = refresh(refreshTok);
    assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
  }
}

