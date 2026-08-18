package com.nhat.workflowhub.auth.security;

import com.nhat.workflowhub.auth.entity.UserAccount;
import com.nhat.workflowhub.auth.entity.UserStatus;
import com.nhat.workflowhub.auth.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserAccountRepository userAccountRepository;

  public JwtAuthenticationFilter(JwtService jwtService, UserAccountRepository userAccountRepository) {
    this.jwtService = jwtService;
    this.userAccountRepository = userAccountRepository;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/api/auth/")
        || path.startsWith("/actuator/health")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/swagger-ui");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorization = request.getHeader("Authorization");
    if (authorization != null && authorization.startsWith("Bearer ")) {
      String token = authorization.substring("Bearer ".length());
      if (jwtService.isValidAccessToken(token)) {
        var userId = jwtService.extractUserId(token);
        userAccountRepository.findById(userId)
            .filter(user -> user.getStatus() == UserStatus.ACTIVE)
            .ifPresent(user -> {
              AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getFullName());
              UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                  principal,
                  null,
                  List.of(new SimpleGrantedAuthority("ROLE_USER"))
              );
              authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
              SecurityContextHolder.getContext().setAuthentication(authentication);
            });
      }
    }

    filterChain.doFilter(request, response);
  }
}
