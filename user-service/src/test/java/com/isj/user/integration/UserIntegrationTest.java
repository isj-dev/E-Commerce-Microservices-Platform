package com.isj.user.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isj.user.dto.LoginRequest;
import com.isj.user.dto.SignUpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.config.import=",
                "eureka.client.enabled=false",
                "jwt.secret=test-secret-key-must-be-at-least-256-bits-long-for-hmac",
                "jwt.expiration=86400000",
                "jwt.refresh-expiration=604800000"
        }
)
@Testcontainers
class UserIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    WebApplicationContext wac;

    @Autowired
    ObjectMapper objectMapper;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void signUpAndLoginFlow() throws Exception {
        SignUpRequest signUp = signUpRequest("flow@example.com", "password123", "Flow User");

        mockMvc.perform(post("/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUp)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("flow@example.com"));

        LoginRequest login = loginRequest("flow@example.com", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void duplicateEmailReturnConflict() throws Exception {
        SignUpRequest signUp = signUpRequest("dup@example.com", "password123", "Dup User");

        mockMvc.perform(post("/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUp)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUp)))
                .andExpect(status().isConflict());
    }

    @Test
    void loginWithWrongPasswordReturnUnauthorized() throws Exception {
        LoginRequest login = loginRequest("nobody@example.com", "wrongpass");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isNotFound());
    }

    private SignUpRequest signUpRequest(String email, String password, String name) throws Exception {
        SignUpRequest req = new SignUpRequest();
        setField(req, "email", email);
        setField(req, "password", password);
        setField(req, "name", name);
        return req;
    }

    private LoginRequest loginRequest(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        setField(req, "email", email);
        setField(req, "password", password);
        return req;
    }

    private void setField(Object obj, String fieldName, String value) throws Exception {
        var field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
