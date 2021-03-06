package com.example.libraryserver.actuator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = MOCK)
@DirtiesContext
@ActiveProfiles("test")
@DisplayName("Calling spring boot actuator")
class ActuatorIntegrationTest {

  @Autowired private WebApplicationContext context;

  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Nested
  @DisplayName("succeeds")
  class PositiveTests {

    @Test
    @DisplayName("for health endpoint without authentication")
    void health() throws Exception {

      mvc.perform(get("/actuator/health"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("UP"))
          .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    @DisplayName("for health endpoint details with authentication")
    void healthWithAuth() throws Exception {

      mvc.perform(get("/actuator/health").with(user("user").roles("LIBRARY_ACTUATOR")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("UP"))
          .andExpect(jsonPath("$.components").exists());
    }

    @Test
    @DisplayName("for info endpoint")
    void info() throws Exception {

      mvc.perform(get("/actuator/info"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.application.name").value("library-server"));
    }

    @Test
    @DisplayName("for env endpoint")
    void env() throws Exception {

      mvc.perform(get("/actuator/env").with(user("user").roles("LIBRARY_ACTUATOR")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.propertySources.length()").value(greaterThan(0)));
    }

    @Test
    @DisplayName("for metrics endpoint")
    void metrics() throws Exception {

      mvc.perform(get("/actuator/metrics").with(user("user").roles("LIBRARY_ACTUATOR")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.names").exists());
    }
  }

  @Nested
  @DisplayName("fails")
  class NegativeTests {

    @Test
    @DisplayName("for env endpoint when user is not authenticated")
    void env() throws Exception {

      mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("for metrics endpoint when user is not authenticated")
    void metrics() throws Exception {

      mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    }
  }
}
