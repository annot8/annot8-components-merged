/* Annot8 (annot8.io) - Licensed under Apache-2.0. */
package io.annot8.components.financial.processors;

import io.annot8.api.annotations.Annotation;
import io.annot8.api.components.Processor;
import io.annot8.api.data.Item;
import io.annot8.api.exceptions.Annot8Exception;
import io.annot8.api.properties.Properties;
import io.annot8.api.stores.AnnotationStore;
import io.annot8.common.data.content.Text;
import io.annot8.conventions.AnnotationTypes;
import io.annot8.conventions.PropertyKeys;
import io.annot8.testing.testimpl.TestItem;
import io.annot8.testing.testimpl.content.TestStringContent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IBANTest {

  @Test
  public void testInvalidIBAN() throws Annot8Exception {
    try (Processor p = new IBAN.Processor()) {
      Item item = new TestItem();

      Text content =
          item.createContent(TestStringContent.class)
              .withData(
                  "These are not valid IBANs: DE45 5001 0517 5407 3249 31 or GB29NWBK60161331926818")
              .save();

      p.process(item);

      AnnotationStore store = content.getAnnotations();

      List<Annotation> annotations = store.getAll().collect(Collectors.toList());
      Assertions.assertEquals(0, annotations.size());
    }
  }

  @Test
  public void testIBAN() throws Annot8Exception {
    try (Processor p = new IBAN.Processor()) {
      Item item = new TestItem();

      Text content =
          item.createContent(TestStringContent.class)
              .withData(
                  "Some valid IBANs given by Wikipedia are: DE44 5001 0517 5407 3249 31, gr 16 0110 1250 0000 0001 2300 695, GB29NWBK60161331926819 and SA03 8000 0000 6080 1016 7519.")
              .save();

      p.process(item);

      AnnotationStore store = content.getAnnotations();

      List<Annotation> annotations = store.getAll().collect(Collectors.toList());
      Assertions.assertEquals(4, annotations.size());

      List<String> ibans =
          new ArrayList<>(
              Arrays.asList(
                  "DE44 5001 0517 5407 3249 31",
                  "gr 16 0110 1250 0000 0001 2300 695",
                  "GB29NWBK60161331926819",
                  "SA03 8000 0000 6080 1016 7519"));

      for (Annotation a : annotations) {
        Assertions.assertEquals(AnnotationTypes.ANNOTATION_TYPE_FINANCIALACCOUNT, a.getType());
        Assertions.assertEquals(content.getId(), a.getContentId());
        Assertions.assertEquals(4, a.getProperties().getAll().size());

        String code = a.getBounds().getData(content).get();

        Assertions.assertTrue(ibans.remove(code));

        Properties props = a.getProperties();

        if (code.startsWith("DE")) {
          Assertions.assertEquals(
              "5407324931", props.get(PropertyKeys.PROPERTY_KEY_ACCOUNTNUMBER).get());
          Assertions.assertFalse(props.get(PropertyKeys.PROPERTY_KEY_BRANCHCODE).isPresent());
          Assertions.assertEquals("50010517", props.get(PropertyKeys.PROPERTY_KEY_BANKCODE).get());
          Assertions.assertEquals("DE", props.get(PropertyKeys.PROPERTY_KEY_COUNTRY).get());
        } else if (code.startsWith("gr")) {
          Assertions.assertEquals(
              "0000000012300695", props.get(PropertyKeys.PROPERTY_KEY_ACCOUNTNUMBER).get());
          Assertions.assertEquals("0125", props.get(PropertyKeys.PROPERTY_KEY_BRANCHCODE).get());
          Assertions.assertEquals("011", props.get(PropertyKeys.PROPERTY_KEY_BANKCODE).get());
          Assertions.assertEquals("GR", props.get(PropertyKeys.PROPERTY_KEY_COUNTRY).get());
        } else if (code.startsWith("GB")) {
          Assertions.assertEquals(
              "31926819", props.get(PropertyKeys.PROPERTY_KEY_ACCOUNTNUMBER).get());
          Assertions.assertEquals("601613", props.get(PropertyKeys.PROPERTY_KEY_BRANCHCODE).get());
          Assertions.assertEquals("NWBK", props.get(PropertyKeys.PROPERTY_KEY_BANKCODE).get());
          Assertions.assertEquals("GB", props.get(PropertyKeys.PROPERTY_KEY_COUNTRY).get());
        } else if (code.startsWith("SA")) {
          Assertions.assertEquals(
              "000000608010167519", props.get(PropertyKeys.PROPERTY_KEY_ACCOUNTNUMBER).get());
          Assertions.assertFalse(props.get(PropertyKeys.PROPERTY_KEY_BRANCHCODE).isPresent());
          Assertions.assertEquals("80", props.get(PropertyKeys.PROPERTY_KEY_BANKCODE).get());
          Assertions.assertEquals("SA", props.get(PropertyKeys.PROPERTY_KEY_COUNTRY).get());
        } else {
          Assertions.fail("Unexpected IBAN code");
        }
      }

      Assertions.assertEquals(0, ibans.size());
    }
  }
}
