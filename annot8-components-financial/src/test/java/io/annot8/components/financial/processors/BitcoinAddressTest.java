/* Annot8 (annot8.io) - Licensed under Apache-2.0. */
package io.annot8.components.financial.processors;

import io.annot8.api.annotations.Annotation;
import io.annot8.api.components.Processor;
import io.annot8.api.data.Item;
import io.annot8.api.exceptions.Annot8Exception;
import io.annot8.api.stores.AnnotationStore;
import io.annot8.common.data.content.Text;
import io.annot8.conventions.AnnotationTypes;
import io.annot8.conventions.PropertyKeys;
import io.annot8.testing.testimpl.TestItem;
import io.annot8.testing.testimpl.content.TestStringContent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BitcoinAddressTest {

  @Test
  public void testBitcoinAddress() throws Annot8Exception {
    try (Processor p = new BitcoinAddress.Processor()) {
      Item item = new TestItem();

      Text content =
          item.createContent(TestStringContent.class)
              .withData(
                  "These are valid addresses: 17VZNX1SN5NtKa8UQFxwQbFeFc3iqRYhem and 3EktnHQD7RiAE6uzMj2ZifT9YgRrkSgzQX; "
                      + "whereas the following are not: 17VZNX1SN5Ntja8UQFxwQbFeFc3iqRYhem (bad checksum), 17VZNX1SN5NtKa8 (too short), 5Hwgr3u458GLafKBgxtssHSPqJnYoGrSzgQsPwLFhLNYskDPyyA (wrong prefix)")
              .save();

      p.process(item);

      AnnotationStore store = content.getAnnotations();

      List<Annotation> annotations = store.getAll().collect(Collectors.toList());
      Assertions.assertEquals(2, annotations.size());

      Map<String, Annotation> annotationMap = new HashMap<>();
      annotations.forEach(a -> annotationMap.put(a.getBounds().getData(content).get(), a));

      Annotation a1 = annotationMap.get("17VZNX1SN5NtKa8UQFxwQbFeFc3iqRYhem");
      Assertions.assertEquals(AnnotationTypes.ANNOTATION_TYPE_FINANCIALACCOUNT, a1.getType());
      Assertions.assertEquals(content.getId(), a1.getContentId());
      Assertions.assertEquals(1, a1.getProperties().getAll().size());
      Assertions.assertEquals(
          "bitcoin#P2PKH", a1.getProperties().get(PropertyKeys.PROPERTY_KEY_ACCOUNTTYPE).get());

      Annotation a2 = annotationMap.get("3EktnHQD7RiAE6uzMj2ZifT9YgRrkSgzQX");
      Assertions.assertEquals(AnnotationTypes.ANNOTATION_TYPE_FINANCIALACCOUNT, a2.getType());
      Assertions.assertEquals(content.getId(), a2.getContentId());
      Assertions.assertEquals(1, a2.getProperties().getAll().size());
      Assertions.assertEquals(
          "bitcoin#P2SH", a2.getProperties().get(PropertyKeys.PROPERTY_KEY_ACCOUNTTYPE).get());
    }
  }
}
