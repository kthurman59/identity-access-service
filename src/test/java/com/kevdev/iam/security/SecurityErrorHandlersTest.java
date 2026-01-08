package com.kevdev.iam.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class SecurityErrorHandlersTest {

    @Test
    void jsonAuthenticationEntryPointDelegates() throws Exception {
        SecurityErrorHandler handler = mock(SecurityErrorHandler.class);
        JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint(handler);

        var req = new MockHttpServletRequest("GET", "/secure/ping");
        var res = new MockHttpServletResponse();
        var ex = new InsufficientAuthenticationException("Unauthorized");

        entryPoint.commence(req, res, ex);

        verify(handler).commence(req, res, ex);
        verifyNoMoreInteractions(handler);
    }
}

