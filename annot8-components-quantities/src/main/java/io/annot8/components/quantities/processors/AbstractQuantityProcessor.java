/* Annot8 (annot8.io) - Licensed under Apache-2.0. */
package io.annot8.components.quantities.processors;

import io.annot8.api.annotations.Annotation.Builder;
import io.annot8.common.data.bounds.SpanBounds;
import io.annot8.common.data.content.Text;
import io.annot8.components.base.text.processors.AbstractTextProcessor;
import io.annot8.conventions.PropertyKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractQuantityProcessor extends AbstractTextProcessor {

  private final String annotationType;
  private final String normalizedUnit;

  private final List<Pattern> patterns = new ArrayList<>();
  private final List<Double> normalizationConstants = new ArrayList<>();

  public AbstractQuantityProcessor(String annotationType, String normalizedUnit) {
    this.annotationType = annotationType;
    this.normalizedUnit = normalizedUnit;
  }

  protected void add(Pattern pattern, double normalizationConstant) {
    patterns.add(pattern);
    normalizationConstants.add(normalizationConstant);
  }

  @Override
  protected void process(Text content) {
    for (int i = 0; i < patterns.size(); i++) {
      process(content, patterns.get(i), normalizationConstants.get(i));
    }
  }

  /**
   * Process a Content object for a pattern
   *
   * @param content The Content object containing the text to process
   * @param pattern The first group must be the number and the second group must be the multiplier
   *     (e.g. million)
   * @param normalization The normalization factor to multiply the extracted value by
   */
  protected void process(Text content, Pattern pattern, double normalization) {
    Matcher m = pattern.matcher(content.getData());
    while (m.find()) {
      Builder builder =
          content
              .getAnnotations()
              .create()
              .withType(annotationType)
              .withBounds(new SpanBounds(m.start(), m.end()));

      try {
        builder =
            builder
                .withProperty(
                    PropertyKeys.PROPERTY_KEY_VALUE,
                    normalise(m.group(1), m.group(2), normalization))
                .withProperty(PropertyKeys.PROPERTY_KEY_UNIT, normalizedUnit);
      } catch (Exception e) {
        log().warn("Unable to parse and normalise value", e);
      }

      builder.save();
    }
  }

  private static double normalise(String number, String multiplier, double normalization) {
    double n = Double.parseDouble(number.replaceAll("[^0-9\\.]", ""));

    long m = 1L;
    if (multiplier != null) {
      switch (multiplier.toLowerCase()) {
        case "thousand":
          m = 1000L;
          break;
        case "million":
          m = 1000000L;
          break;
        case "billion":
          m = 1000000000L;
          break;
        case "trillion":
          m = 1000000000000L;
          break;
      }
    }

    return n * m * normalization;
  }
}
