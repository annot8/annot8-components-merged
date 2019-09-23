/* Annot8 (annot8.io) - Licensed under Apache-2.0. */
package io.annot8.components.base.processors;

import java.util.regex.Pattern;

import com.google.common.base.Strings;

import io.annot8.api.settings.Settings;

public class RegexSettings implements Settings {
  private final Pattern regex;
  private final int group;
  private final String type;

  public RegexSettings(Pattern regex, int group, String type) {
    this.regex = regex;
    this.group = group;
    this.type = type;
  }

  public Pattern getRegex() {
    return regex;
  }

  public int getGroup() {
    return group;
  }

  public String getType() {
    return type;
  }

  @Override
  public boolean validate() {
    return regex != null && group >= 0 && !Strings.isNullOrEmpty(type);
  }
}