package com.nhat.workflowhub.ai.guardrail;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AgentRateLimitGuard {

  private static final int MAX_REQUESTS = 12;
  private static final Duration WINDOW = Duration.ofMinutes(1);

  private final Clock clock;
  private final Map<UUID, Deque<Instant>> requestsByUser = new HashMap<>();

  public AgentRateLimitGuard() {
    this(Clock.systemUTC());
  }

  public AgentRateLimitGuard(Clock clock) {
    this.clock = clock;
  }

  public synchronized Decision allow(UUID userId) {
    Instant now = clock.instant();
    Deque<Instant> requests = requestsByUser.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
    while (!requests.isEmpty() && requests.peekFirst().isBefore(now.minus(WINDOW))) {
      requests.removeFirst();
    }

    if (requests.size() >= MAX_REQUESTS) {
      Instant oldest = requests.peekFirst();
      long retryAfterSeconds = oldest == null ? WINDOW.toSeconds() : Duration.between(now, oldest.plus(WINDOW)).toSeconds();
      return new Decision(false, Math.max(retryAfterSeconds, 1), 0);
    }

    requests.addLast(now);
    return new Decision(true, 0, MAX_REQUESTS - requests.size());
  }

  public record Decision(boolean allowed, long retryAfterSeconds, long remainingRequests) {
  }
}
