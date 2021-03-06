package com.example.libraryserver.book.web;

import com.example.libraryserver.DataInitializer;
import com.example.libraryserver.security.AuthenticatedUser;
import com.example.libraryserver.user.data.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyUris;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith({SpringExtension.class, RestDocumentationExtension.class})
@SpringBootTest(webEnvironment = MOCK)
@DirtiesContext
@ActiveProfiles("test")
@DisplayName("Calling book rest api")
class BookRestControllerIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired private WebApplicationContext context;
  private MockMvc mvc;

  @BeforeEach
  void setup(RestDocumentationContextProvider restDocumentationContextProvider) {
    mvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .apply(
                documentationConfiguration(restDocumentationContextProvider)
                    .operationPreprocessors()
                    .withRequestDefaults(prettyPrint(), modifyUris().port(9090))
                    .withResponseDefaults(prettyPrint(), modifyUris().port(9090)))
            .build();
  }

  private UserDetails userDetails(UUID identifier, String role) {
    return new AuthenticatedUser(
        new User(
            identifier,
            "Hans",
            "Mustermann",
            "test@example.com",
            "secret",
            Collections.singleton(role)));
  }

  @Nested
  @DisplayName("succeeds")
  class PositiveTests {

    @Test
    @DisplayName("in creating a book")
    void createBook() throws Exception {

      BookModel model =
          new BookModel("1234567890123", "title", "description", Collections.singleton("author"));
      mvc.perform(
              post("/books")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(model))
                  .with(csrf())
                  .with(user("user").roles("LIBRARY_CURATOR")))
          .andExpect(status().isCreated())
          .andExpect(header().exists("location"))
          .andExpect(jsonPath("$.identifier").exists())
          .andDo(document("create-book"));
    }

    @Test
    @DisplayName("in updating a book")
    void updateBook() throws Exception {
      BookModel model =
          new BookModel("1234567890123", "title", "description", Collections.singleton("author"));
      mvc.perform(
              put("/books/{bookIdentifier}", DataInitializer.BOOK_DEVOPS_IDENTIFIER)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(model))
                  .with(csrf())
                  .with(user("user").roles("LIBRARY_CURATOR")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title").value("title"))
          .andDo(document("update-book"));
    }

    @Test
    @DisplayName("in borrowing a book")
    void borrowBook() throws Exception {
      mvc.perform(
              post(
                      "/books/{bookIdentifier}/borrow/{userIdentifier}",
                      DataInitializer.BOOK_SPRING_ACTION_IDENTIFIER,
                      DataInitializer.BANNER_USER_IDENTIFIER)
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf())
                  .with(
                      user(
                          userDetails(
                              DataInitializer.BANNER_USER_IDENTIFIER, "LIBRARY_USER"))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.borrowedByUser").exists())
          .andDo(document("borrow-book"));
    }

    @Test
    @DisplayName("in returning a book")
    void returnBook() throws Exception {
      mvc.perform(
              post(
                      "/books/{bookIdentifier}/return/{userIdentifier}",
                      DataInitializer.BOOK_CLEAN_CODE_IDENTIFIER,
                      DataInitializer.WAYNE_USER_IDENTIFIER)
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf())
                  .with(
                      user(
                          userDetails(DataInitializer.WAYNE_USER_IDENTIFIER, "LIBRARY_USER"))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.borrowedByUser").doesNotExist())
          .andDo(document("return-book"));
    }

    @Test
    @DisplayName("in getting a list of all books")
    void listAllBooks() throws Exception {
      mvc.perform(get("/books").with(user("user")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.books.length()").value(greaterThan(0)))
          .andDo(document("get-books"));
    }

    @Test
    @DisplayName("in getting a single book")
    void getSingleBook() throws Exception {
      mvc.perform(
              get("/books/{bookIdentifier}", DataInitializer.BOOK_CLEAN_CODE_IDENTIFIER)
                  .with(user("user")))
          .andExpect(status().isOk())
          .andExpect(
              jsonPath("$.identifier").value(DataInitializer.BOOK_CLEAN_CODE_IDENTIFIER.toString()))
          .andExpect(jsonPath("$.title").value("Clean Code"))
          .andDo(document("get-book"));
    }

    @Test
    @DisplayName("in deleting a book")
    void deleteSingleBook() throws Exception {
      mvc.perform(
              delete("/books/{bookIdentifier}", DataInitializer.BOOK_CLOUD_NATIVE_IDENTIFIER)
                  .with(csrf())
                  .with(user("user").roles("LIBRARY_CURATOR")))
          .andExpect(status().isNoContent())
          .andDo(document("delete-book"));
    }
  }

  @Nested
  @DisplayName("fails")
  class NegativeTests {

    @Test
    @DisplayName("in creating a book with invalid ISBN number")
    void createBook() throws Exception {

      BookModel model =
          new BookModel("1234567", "title", "description", Collections.singleton("author"));
      mvc.perform(
              post("/books")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(model))
                  .with(csrf())
                  .with(user("user").roles("LIBRARY_CURATOR")))
          .andExpect(status().isBadRequest())
          .andExpect(
              content()
                  .string(startsWith("Field error in object \\'bookModel\\' on field \\'isbn\\'")));
    }

    @Test
    @DisplayName("in updating a book with invalid ISBN number")
    void updateBook() throws Exception {
      BookModel model =
          new BookModel("123456", "title", "description", Collections.singleton("author"));
      mvc.perform(
              put("/books/{bookIdentifier}", DataInitializer.BOOK_CLEAN_CODE_IDENTIFIER)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(model))
                  .with(csrf())
                  .with(user("user").roles("LIBRARY_CURATOR")))
          .andExpect(status().isBadRequest())
          .andExpect(
              content()
                  .string(startsWith("Field error in object \\'bookModel\\' on field \\'isbn\\'")));
    }

    @Test
    @DisplayName("in updating an unknown book")
    void updateBookUnknown() throws Exception {
      BookModel model =
          new BookModel("1234567890123", "title", "description", Collections.singleton("author"));
      mvc.perform(
              put("/books/{bookIdentifier}", UUID.randomUUID())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(model))
                  .with(csrf())
                  .with(user("user").roles("LIBRARY_CURATOR")))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("in borrowing an unknown book")
    void borrowUnknownBook() throws Exception {
      mvc.perform(
              post(
                      "/books/{bookIdentifier}/borrow/{userIdentifier}",
                      UUID.randomUUID(),
                      DataInitializer.BANNER_USER_IDENTIFIER)
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf())
                  .with(user("user").roles("LIBRARY_USER")))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("in borrowing a book for an unknown user")
    void borrowBookWithUnknownUser() throws Exception {
      mvc.perform(
              post(
                      "/books/{bookIdentifier}/borrow/{userIdentifier}",
                      DataInitializer.BOOK_SPRING_ACTION_IDENTIFIER,
                      UUID.randomUUID())
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf())
                  .with(user("user").roles("LIBRARY_USER")))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("in returning an unknown book")
    void returnUnknownBook() throws Exception {
      mvc.perform(
              post(
                      "/books/{bookIdentifier}/return/{userIdentifier}",
                      UUID.randomUUID(),
                      DataInitializer.WAYNE_USER_IDENTIFIER)
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf())
                  .with(user("user").roles("LIBRARY_USER")))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("in returning a book for an unknown user")
    void returnBookWithUnknownUser() throws Exception {
      mvc.perform(
              post(
                      "/books/{bookIdentifier}/return/{userIdentifier}",
                      DataInitializer.BOOK_CLEAN_CODE_IDENTIFIER,
                      UUID.randomUUID())
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf())
                  .with(user("user").roles("LIBRARY_USER")))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("in getting an unknown book")
    void getSingleBook() throws Exception {
      mvc.perform(get("/books/{bookIdentifier}", UUID.randomUUID()).with(user("user")))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("in deleting an unknown book")
    void deleteSingleBook() throws Exception {
      mvc.perform(
              delete("/books/{bookIdentifier}", UUID.randomUUID())
                  .with(csrf())
                  .with(user("user").roles("LIBRARY_CURATOR")))
          .andExpect(status().isNotFound());
    }
  }

  @DisplayName("fails with unauthorized")
  @Nested
  class AuthenticationTests {

    @Test
    @DisplayName("in creating a book")
    void createBookUnauthorized() throws Exception {

      BookModel model =
          new BookModel("1234567890123", "title", "description", Collections.singleton("author"));
      mvc.perform(
              post("/books")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(model))
                  .with(csrf()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("in updating a book")
    void updateBookUnauthorized() throws Exception {
      BookModel model =
          new BookModel("1234567890123", "title", "description", Collections.singleton("author"));
      mvc.perform(
              put("/books/{bookIdentifier}", DataInitializer.BOOK_DEVOPS_IDENTIFIER)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(model))
                  .with(csrf()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("in borrowing a book")
    void borrowBookUnauthorized() throws Exception {
      mvc.perform(
              post(
                      "/books/{bookIdentifier}/borrow/{userIdentifier}",
                      DataInitializer.BOOK_SPRING_ACTION_IDENTIFIER,
                      DataInitializer.BANNER_USER_IDENTIFIER)
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("in returning a book")
    void returnBookUnauthorized() throws Exception {
      mvc.perform(
              post(
                      "/books/{bookIdentifier}/return/{userIdentifier}",
                      DataInitializer.BOOK_CLEAN_CODE_IDENTIFIER,
                      DataInitializer.WAYNE_USER_IDENTIFIER)
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(csrf()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("in getting a list of all books")
    void listAllBooksUnauthorized() throws Exception {
      mvc.perform(get("/books")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("in getting a single book")
    void getSingleBookUnauthorized() throws Exception {
      mvc.perform(get("/books/{bookIdentifier}", DataInitializer.BOOK_CLEAN_CODE_IDENTIFIER))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("in deleting a book")
    void deleteSingleBookUnauthorized() throws Exception {
      mvc.perform(
              delete("/books/{bookIdentifier}", DataInitializer.BOOK_CLOUD_NATIVE_IDENTIFIER)
                  .with(csrf()))
          .andExpect(status().isUnauthorized());
    }
  }

  @DisplayName("fails for missing CSRF token")
  @Nested
  class CsrfTokenTests {

    @Test
    @DisplayName("in creating a book")
    void createBookNoCsrfToken() throws Exception {

      BookModel model =
          new BookModel("1234567890123", "title", "description", Collections.singleton("author"));
      mvc.perform(
              post("/books")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(model))
                  .with(user("user").roles("LIBRARY_CURATOR")))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("in updating a book")
    void updateBookNoCsrfToken() throws Exception {
      BookModel model =
          new BookModel("1234567890123", "title", "description", Collections.singleton("author"));
      mvc.perform(
              put("/books/{bookIdentifier}", DataInitializer.BOOK_DEVOPS_IDENTIFIER)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(model))
                  .with(user("user").roles("LIBRARY_CURATOR")))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("in borrowing a book")
    void borrowBookNoCsrfToken() throws Exception {
      mvc.perform(
              post(
                      "/books/{bookIdentifier}/borrow/{userIdentifier}",
                      DataInitializer.BOOK_SPRING_ACTION_IDENTIFIER,
                      DataInitializer.BANNER_USER_IDENTIFIER)
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(user("user").roles("LIBRARY_USER")))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("in returning a book")
    void returnBookNoCsrfToken() throws Exception {
      mvc.perform(
              post(
                      "/books/{bookIdentifier}/return/{userIdentifier}",
                      DataInitializer.BOOK_CLEAN_CODE_IDENTIFIER,
                      DataInitializer.WAYNE_USER_IDENTIFIER)
                  .contentType(MediaType.APPLICATION_JSON)
                  .with(user("user").roles("LIBRARY_USER")))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("in deleting a book")
    void deleteSingleBookNoCsrfToken() throws Exception {
      mvc.perform(
              delete("/books/{bookIdentifier}", DataInitializer.BOOK_CLOUD_NATIVE_IDENTIFIER)
                  .with(user("user").roles("LIBRARY_CURATOR")))
          .andExpect(status().isForbidden());
    }
  }
}
