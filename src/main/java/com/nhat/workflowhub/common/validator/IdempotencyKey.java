package com.nhat.workflowhub.common.validator;

public final class IdempotencyKey {

  private IdempotencyKey() {
  }

  public static boolean isValid(String value) {
    return value != null && !value.isBlank();
  }
}
