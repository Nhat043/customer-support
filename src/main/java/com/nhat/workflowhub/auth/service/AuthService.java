package com.nhat.workflowhub.auth.service;

import com.nhat.workflowhub.auth.dto.LoginRequest;
import com.nhat.workflowhub.auth.dto.LoginResponse;
import com.nhat.workflowhub.auth.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final UserAccountRepository userAccountRepository;

  public AuthService(UserAccountRepository userAccountRepository) {
    this.userAccountRepository = userAccountRepository;
  }

  public LoginResponse login(LoginRequest request) {
    userAccountRepository.findByEmail(request.email());
    return new LoginResponse("demo-access-token", "Bearer");
  }
}
