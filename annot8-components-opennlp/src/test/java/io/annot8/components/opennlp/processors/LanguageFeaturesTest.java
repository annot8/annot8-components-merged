/*
 * Crown Copyright (C) 2019 Dstl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.annot8.components.opennlp.processors;

import io.annot8.api.annotations.Annotation;
import io.annot8.api.annotations.Group;
import io.annot8.api.components.Processor;
import io.annot8.api.data.Item;
import io.annot8.api.settings.NoSettings;
import io.annot8.common.data.bounds.SpanBounds;
import io.annot8.common.data.utils.SortUtils;
import io.annot8.conventions.AnnotationTypes;
import io.annot8.conventions.GroupTypes;
import io.annot8.conventions.PropertyKeys;
import io.annot8.testing.testimpl.TestItem;
import io.annot8.testing.testimpl.content.TestStringContent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LanguageFeaturesTest {
  @Test
  public void test(){
    Item item = new TestItem();
    TestStringContent content = item.createContent(TestStringContent.class)
        .withData("This is some text. It has three sentences. The first sentence has four words.")
        .save();

    LanguageFeatures desc = new LanguageFeatures();
    Processor p = desc.createComponent(null, NoSettings.getInstance());
    p.process(item);

    assertEquals(3, content.getAnnotations().getByType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).count());

    Annotation sentence = content.getAnnotations().getByType(AnnotationTypes.ANNOTATION_TYPE_SENTENCE).min(SortUtils.SORT_BY_SPANBOUNDS).get();
    SpanBounds sentenceBounds = sentence.getBounds(SpanBounds.class).get();

    List<Annotation> tokens = content.getBetween(sentenceBounds.getBegin(), sentenceBounds.getEnd())
        .filter(a -> AnnotationTypes.ANNOTATION_TYPE_WORDTOKEN.equals(a.getType()))
        .sorted(SortUtils.SORT_BY_SPANBOUNDS)
        .collect(Collectors.toList());
    assertEquals(5, tokens.size());

    assertEquals("NN", tokens.get(3).getProperties().get(PropertyKeys.PROPERTY_KEY_PARTOFSPEECH).get());

    List<Group> chunks =
    item.getGroups().getByType(GroupTypes.GROUP_TYPE_GRAMMAR_PHRASE).filter(g -> {
      int start = g.getAnnotationsForContent(content).mapToInt(a -> a.getBounds(SpanBounds.class).get().getBegin()).min().getAsInt();
      int end = g.getAnnotationsForContent(content).mapToInt(a -> a.getBounds(SpanBounds.class).get().getBegin()).max().getAsInt();

      return start >= sentenceBounds.getBegin() && end <= sentenceBounds.getEnd();
    }).collect(Collectors.toList());

    assertEquals(3, chunks.size());

    List<String> phrases = chunks.stream().map(g ->
      g.getAnnotationsForContent(content).sorted(SortUtils.SORT_BY_SPANBOUNDS).map(a -> content.getText(a).get()).collect(Collectors.joining(" "))
    ).collect(Collectors.toList());

    assertTrue(phrases.contains("some text ."));
  }
}