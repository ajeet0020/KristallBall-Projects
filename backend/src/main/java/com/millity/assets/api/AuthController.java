package com.millity.assets.api;

import com.millity.assets.security.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) { this.authenticationManager=authenticationManager; this.jwtService=jwtService; }
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {}
    public record CurrentUser(Long id, String username, String role, Long baseId) {}
    @PostMapping("/login") public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            return new TokenResponse(jwtService.issue((JwtPrincipal) auth.getPrincipal()), "Bearer", jwtService.expirationSeconds());
        } catch (AuthenticationException ex) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"); }
    }
    @GetMapping("/me") @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','BASE_COMMANDER','LOGISTICS_OFFICER')")
    public CurrentUser me(@AuthenticationPrincipal JwtPrincipal principal) { return new CurrentUser(principal.id(), principal.username(), principal.role().name(), principal.baseId()); }
}
