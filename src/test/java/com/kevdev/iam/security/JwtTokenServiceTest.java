package com.kevdev.iam.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JwtTokenServiceTest {

    @Test
    void issuesThreeSegmentJwtAndRolesClaim() throws Exception {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) key[i] = (byte) (i + 1);
        String secretB64 = Base64.getEncoder().encodeToString(key);

        JwtTokenService svc = new JwtTokenService(new ObjectMapper(), secretB64, "ias", 15);

        User user = new User(
            "admin",
            "x",
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        String jwt = svc.issueAccessToken(user);
        assertEquals(3, jwt.split("\\.").length);

        String payloadB64 = jwt.split("\\.")[1];
        byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadB64);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = new ObjectMapper().readValue(new String(payloadBytes, StandardCharsets.UTF_8), Map.class);

        assertEquals("ias", payload.get("iss"));
        assertEquals("admin", payload.get("sub"));

        Object rolesObj = payload.get("roles");
        assertNotNull(rolesObj);
        assertTrue(rolesObj instanceof List);

        @SuppressWarnings("unchecked")
        List<Object> roles = (List<Object>) rolesObj;
        assertEquals(1, roles.size());
        assertEquals("ADMIN", roles.get(0).toString());
    }
}

