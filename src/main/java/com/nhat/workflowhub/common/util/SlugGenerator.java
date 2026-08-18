package com.nhat.workflowhub.common.util;

public final class SlugGenerator {

  private SlugGenerator() {
  }

  public static String slugify(String input) {
    return input == null ? "" : input.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
  }
}
