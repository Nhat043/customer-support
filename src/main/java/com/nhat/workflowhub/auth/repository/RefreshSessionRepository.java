package com.nhat.workflowhub.auth.repository;

import com.nhat.workflowhub.auth.entity.RefreshSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

  Optional<RefreshSession> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);
}
