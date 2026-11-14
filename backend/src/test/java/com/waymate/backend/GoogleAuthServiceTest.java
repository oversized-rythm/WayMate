package com.waymate.backend;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.waymate.backend.dto.LoginResponse;
import com.waymate.backend.entity.User;
import com.waymate.backend.exception.ApiException;
import com.waymate.backend.repository.UserRepository;
import com.waymate.backend.service.GoogleAuthService;
import com.waymate.backend.service.JwtService;
import com.waymate.backend.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GoogleAuthServiceTest {

    //Fake dependencies to not hit DB or Google's servers
    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock private GoogleIdTokenVerifier verifier;

    private GoogleAuthService googleAuthService;

    @BeforeEach
    void setUp(){
        //Runs fresh before each @Test method using our test constructor
        googleAuthService = new GoogleAuthService(userRepository,jwtService,refreshTokenService,verifier);
    }
    //TEST-1
    @Test
    void authenticateWithGoogle_newUser_createsAccountAndReturnsTokens() throws Exception {
        // 1. ARRANGE: Build a fake Google token payload
        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-sub-123");
        payload.setEmail("Aarav@Example.com"); // Mixed-case to test lowercase normalization
        payload.setEmailVerified(true);
        payload.set("name", "Aarav Sharma");
        payload.set("picture", "https://example.com/avatar.png");

        // Tell mocks what to return when called
        when(verifier.verify("valid-google-token")).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(payload);
        when(userRepository.findByGoogleId("google-sub-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("aarav@example.com")).thenReturn(Optional.empty());

        // Simulate DB generating a UUID on save()
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("waymate-access-jwt");
        when(refreshTokenService.createRefreshToken(any(), any())).thenReturn("waymate-refresh-token");

        //ACT: Call the actual service method
        LoginResponse response = googleAuthService.authenticateWithGoogle("valid-google-token");

        //ASSERT: Verify returned tokens + the exact User object passed to save()
        assertEquals("waymate-access-jwt", response.getAccessToken());
        assertEquals("waymate-refresh-token", response.getRefreshToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture()); // Captures what was saved
        User createdUser = userCaptor.getValue();

        assertEquals("aarav@example.com", createdUser.getEmail());
        assertEquals("GOOGLE", createdUser.getAuthProvider());
        assertTrue(createdUser.isEmailVerified());
    }

    //TEST-2
    @Test
    void authenticateWithGoogle_existingLocalUser_linksGoogleId() throws Exception {
        // 1. ARRANGE: Existing user in DB without a googleId yet
        User existingUser = new User();
        when(jwtService.generateToken(any())).thenReturn("jwt-123");
        existingUser.setEmail("priya@example.com");
        existingUser.setAccountStatus("ACTIVE");

        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-sub-999");
        payload.setEmail("priya@example.com");
        payload.setEmailVerified(true);

        when(verifier.verify("valid-token")).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(payload);
        when(userRepository.findByGoogleId("google-sub-999")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("priya@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(existingUser.getUserId())).thenReturn("jwt-123");
        when(refreshTokenService.createRefreshToken(existingUser, "Google SSO")).thenReturn("ref-123");

        // 2. ACT
        LoginResponse response = googleAuthService.authenticateWithGoogle("valid-token");

        // 3. ASSERT: Existing account now has googleId attached
        assertEquals("google-sub-999", existingUser.getGoogleId());
        assertEquals("jwt-123", response.getAccessToken());
    }
    //TEST-3
    @Test
    void authenticateWithGoogle_invalidToken_throwsUnauthorized() throws Exception {
        // 1. ARRANGE: Simulate Google rejecting the token
        when(verifier.verify("bad-token")).thenReturn(null);

        // 2. ACT & 3. ASSERT: Expect ApiException with 401 status and zero DB writes
        ApiException ex = assertThrows(
                ApiException.class,
                () -> googleAuthService.authenticateWithGoogle("bad-token")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verify(userRepository, never()).save(any());
    }

}
