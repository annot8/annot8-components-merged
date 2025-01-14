/* Annot8 (annot8.io) - Licensed under Apache-2.0. */
package io.annot8.components.financial.processors;

import io.annot8.api.capabilities.Capabilities;
import io.annot8.api.components.annotations.ComponentDescription;
import io.annot8.api.components.annotations.ComponentName;
import io.annot8.api.context.Context;
import io.annot8.api.settings.NoSettings;
import io.annot8.api.stores.AnnotationStore;
import io.annot8.common.components.AbstractProcessorDescriptor;
import io.annot8.common.components.capabilities.SimpleCapabilities;
import io.annot8.common.data.bounds.SpanBounds;
import io.annot8.common.data.content.Text;
import io.annot8.components.base.text.processors.AbstractTextProcessor;
import io.annot8.conventions.AnnotationTypes;
import io.annot8.conventions.PropertyKeys;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.iban4j.Iban4jException;

@ComponentName("IBAN")
@ComponentDescription("Extract IBAN (International Bank Account Number) numbers from text")
public class IBAN extends AbstractProcessorDescriptor<IBAN.Processor, NoSettings> {

  @Override
  protected Processor createComponent(Context context, NoSettings settings) {
    return new Processor();
  }

  @Override
  public Capabilities capabilities() {
    return new SimpleCapabilities.Builder()
        .withProcessesContent(Text.class)
        .withCreatesAnnotations(AnnotationTypes.ANNOTATION_TYPE_FINANCIALACCOUNT, SpanBounds.class)
        .build();
  }

  public static class Processor extends AbstractTextProcessor {

    private static final Pattern IBAN_PATTERN =
        Pattern.compile(
            "\\b([A-Z]{2})\\s*([0-9]{2})\\s*(([A-Z0-9]{4}\\s*){2,7}[A-Z0-9]{1,4})\\b",
            Pattern.CASE_INSENSITIVE);

    @Override
    protected void process(Text content) {
      Matcher m = IBAN_PATTERN.matcher(content.getData());
      AnnotationStore annotationStore = content.getAnnotations();

      while (m.find()) {
        String code = m.group().replaceAll("\\s*", "").toUpperCase();

        try {
          org.iban4j.Iban iban = org.iban4j.Iban.valueOf(code);

          annotationStore
              .create()
              .withType(AnnotationTypes.ANNOTATION_TYPE_FINANCIALACCOUNT)
              .withBounds(new SpanBounds(m.start(), m.end()))
              .withProperty(PropertyKeys.PROPERTY_KEY_ACCOUNTNUMBER, iban.getAccountNumber())
              .withProperty(PropertyKeys.PROPERTY_KEY_BANKCODE, iban.getBankCode())
              .withProperty(PropertyKeys.PROPERTY_KEY_BRANCHCODE, iban.getBranchCode())
              .withProperty(PropertyKeys.PROPERTY_KEY_COUNTRY, iban.getCountryCode().getAlpha2())
              .save();

        } catch (Iban4jException e) {
          // Not a valid IBAN, so continue
        }
      }
    }
  }
}
