package com.pnevsky.spring_security;

import com.pnevsky.spring_security.models.Person;
import com.pnevsky.spring_security.repositories.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityFlowTests {

    private static final String PASSWORD = "demo-password-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void prepareUsers() {
        personRepository.deleteAll();
        saveUser("user_demo", "ROLE_USER");
        saveUser("admin_demo", "ROLE_ADMIN");
    }

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
    }

    @Test
    void registrationPageIsPublic() throws Exception {
        mockMvc.perform(get("/auth/registration"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("type=\"password\"")));
    }

    @Test
    void registrationHashesPasswordAndIgnoresSubmittedRoleAndId() throws Exception {
        mockMvc.perform(post("/auth/registration").with(csrf())
                        .param("username", "new_user")
                        .param("password", PASSWORD)
                        .param("yearOfBirth", "1990")
                        .param("role", "ROLE_ADMIN")
                        .param("id", "999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        Person person = personRepository.findByUsername("new_user").orElseThrow();
        assertThat(person.getRole()).isEqualTo("ROLE_USER");
        assertThat(person.getId()).isNotEqualTo(999);
        assertThat(person.getPassword()).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, person.getPassword())).isTrue();
        assertThat(person.toString()).doesNotContain(PASSWORD, person.getPassword());
    }

    @Test
    void registrationWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/auth/registration")
                        .param("username", "new_user")
                        .param("password", PASSWORD)
                        .param("yearOfBirth", "1990"))
                .andExpect(status().isForbidden());
        assertThat(personRepository.findByUsername("new_user")).isEmpty();
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        mockMvc.perform(post("/auth/registration").with(csrf())
                        .param("username", "user_demo")
                        .param("password", PASSWORD)
                        .param("yearOfBirth", "1990"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("person", "username"));
        assertThat(personRepository.count()).isEqualTo(2);
    }

    @Test
    void databaseEnforcesUniqueUsername() {
        Person duplicate = new Person("user_demo", 1990);
        duplicate.setPassword(passwordEncoder.encode(PASSWORD));
        duplicate.setRole("ROLE_USER");
        assertThatThrownBy(() -> personRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void invalidRegistrationDoesNotStoreUserOrRedisplayPassword() throws Exception {
        mockMvc.perform(post("/auth/registration").with(csrf())
                        .param("username", "invalid_user")
                        .param("password", "short")
                        .param("yearOfBirth", Integer.toString(Year.now().getValue() + 1)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("person", "password", "yearOfBirth"))
                .andExpect(content().string(not(containsString("value=\"short\""))));
        assertThat(personRepository.findByUsername("invalid_user")).isEmpty();
    }

    @Test
    void blankUsernameAndMissingBirthYearAreRejected() throws Exception {
        mockMvc.perform(post("/auth/registration").with(csrf())
                        .param("username", " ")
                        .param("password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("person", "username", "yearOfBirth"));
        assertThat(personRepository.count()).isEqualTo(2);
    }

    @Test
    void passwordExceedingBcryptByteLimitIsRejected() throws Exception {
        mockMvc.perform(post("/auth/registration").with(csrf())
                        .param("username", "unicode_user")
                        .param("password", "я".repeat(40))
                        .param("yearOfBirth", "1990"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("person", "password"));
        assertThat(personRepository.findByUsername("unicode_user")).isEmpty();
    }

    @Test
    void correctCredentialsAuthenticateUser() throws Exception {
        login("user_demo");
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/process_login").with(csrf())
                        .param("username", "user_demo")
                        .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void userCannotAccessAdminPage() throws Exception {
        MockHttpSession session = login("user_demo");
        mockMvc.perform(get("/admin").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminPage() throws Exception {
        MockHttpSession session = login("admin_demo");
        mockMvc.perform(get("/admin").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Admin page")));
    }

    @Test
    void userInfoDoesNotExposePasswordHash() throws Exception {
        MockHttpSession session = login("user_demo");
        String hash = personRepository.findByUsername("user_demo").orElseThrow().getPassword();
        mockMvc.perform(get("/showUserInfo").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("user_demo")))
                .andExpect(content().string(not(containsString(hash))))
                .andExpect(content().string(not(containsString(PASSWORD))));
    }

    @Test
    void logoutRequiresCsrfTokenAndClearsSession() throws Exception {
        MockHttpSession session = login("user_demo");
        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(unauthenticated());
        assertThat(session.isInvalid()).isTrue();
    }

    private MockHttpSession login(String username) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/process_login").with(csrf())
                        .param("username", username)
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hello"))
                .andExpect(authenticated().withUsername(username))
                .andReturn().getRequest().getSession(false);
    }

    private void saveUser(String username, String role) {
        Person person = new Person(username, 1990);
        person.setPassword(passwordEncoder.encode(PASSWORD));
        person.setRole(role);
        personRepository.save(person);
    }
}
