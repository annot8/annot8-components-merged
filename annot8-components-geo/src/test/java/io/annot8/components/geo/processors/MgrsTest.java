/* Annot8 (annot8.io) - Licensed under Apache-2.0. */
package io.annot8.components.geo.processors;

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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MgrsTest {
  @Test
  public void testMgrs() throws Annot8Exception {

    try (Processor p = new Mgrs.Processor(true)) {
      Item item = new TestItem();

      Text content =
          item.createContent(TestStringContent.class)
              .withData("Honolulu is in the 10 km square that is called 4QFJ 1 5.")
              .save();

      p.process(item);

      AnnotationStore store = content.getAnnotations();

      List<Annotation> annotations = store.getAll().collect(Collectors.toList());
      Assertions.assertEquals(1, annotations.size());

      Annotation a = annotations.get(0);
      Assertions.assertEquals(AnnotationTypes.ANNOTATION_TYPE_COORDINATE, a.getType());
      Assertions.assertEquals(content.getId(), a.getContentId());
      Assertions.assertEquals("4QFJ 1 5", a.getBounds().getData(content).get());

      Assertions.assertEquals(3, a.getProperties().getAll().size());

      Optional<Object> optValue = a.getProperties().get(PropertyKeys.PROPERTY_KEY_VALUE);
      Assertions.assertTrue(optValue.isPresent());
      Assertions.assertEquals("4QFJ15", optValue.get());

      Optional<Object> optType = a.getProperties().get(PropertyKeys.PROPERTY_KEY_COORDINATETYPE);
      Assertions.assertTrue(optType.isPresent());
      Assertions.assertEquals("MGRS", optType.get());

      Optional<Object> optGeojson = a.getProperties().get(PropertyKeys.PROPERTY_KEY_GEOJSON);
      Assertions.assertTrue(optGeojson.isPresent());
    }
  }

  @Test
  public void testMgrsDates() throws Annot8Exception {

    try (Processor p = new Mgrs.Processor(true)) {
      Item item = new TestItem();

      Text content =
          item.createContent(TestStringContent.class)
              .withData("Bob WAS BORN ON 19 MAR 1968")
              .save();

      p.process(item);

      AnnotationStore store = content.getAnnotations();

      List<Annotation> annotations = store.getAll().collect(Collectors.toList());
      Assertions.assertEquals(0, annotations.size());
    }
  }

  @Test
  public void testMgrsAllowDates() throws Annot8Exception {
    try (Processor p = new Mgrs.Processor(false)) {
      Item item = new TestItem();

      Text content =
          item.createContent(TestStringContent.class)
              .withData("Bob WAS BORN ON 19 MAR 1968")
              .save();

      p.process(item);

      AnnotationStore store = content.getAnnotations();

      List<Annotation> annotations = store.getAll().collect(Collectors.toList());
      Assertions.assertEquals(1, annotations.size());
    }
  }
}
