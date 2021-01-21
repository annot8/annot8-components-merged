/* Annot8 (annot8.io) - Licensed under Apache-2.0. */
package io.annot8.components.documents.processors;

import static org.junit.jupiter.api.Assertions.*;

import io.annot8.api.capabilities.Capabilities;
import io.annot8.api.capabilities.ContentCapability;
import io.annot8.api.components.annotations.ComponentDescription;
import io.annot8.api.components.annotations.ComponentName;
import io.annot8.api.components.annotations.SettingsClass;
import io.annot8.api.components.responses.ProcessorResponse;
import io.annot8.api.context.Context;
import io.annot8.api.data.Item;
import io.annot8.api.properties.Properties;
import io.annot8.common.data.content.FileContent;
import io.annot8.common.data.content.Image;
import io.annot8.common.data.content.InputStreamContent;
import io.annot8.common.data.content.Text;
import io.annot8.implementations.support.context.SimpleContext;
import io.annot8.testing.testimpl.TestItem;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public abstract class AbstractDocumentExtractorTest {
  protected abstract Class<? extends AbstractDocumentExtractorDescriptor<?>> getDescriptor();

  protected abstract File getTestFile();

  protected abstract void validateMetadata(Properties itemProperties);

  protected abstract void validateText(Collection<Text> textContents);

  protected abstract void validateImages(Collection<Image> imageContents);

  @Test
  public void testForAnnotations() {
    Class<? extends AbstractDocumentExtractorDescriptor<?>> desc = getDescriptor();

    assertNotNull(desc.getAnnotation(ComponentName.class));
    assertNotNull(desc.getAnnotation(ComponentDescription.class));
    assertNotNull(desc.getAnnotation(SettingsClass.class));
  }

  @Test
  public void testDefaultCapabilities() throws Exception {
    AbstractDocumentExtractorDescriptor<?> desc = getInstantiatedDescriptor();
    desc.setSettings(new DocumentExtractorSettings());

    Capabilities capabilities = desc.capabilities();
    assertNotNull(capabilities);
    assertEquals(2, capabilities.processes().count()); // FileContent and InputStreamContent
    assertTrue(capabilities.creates().count() > 0);
  }

  @Test
  public void testTextCapabilities() throws Exception {
    AbstractDocumentExtractorDescriptor<?> desc = getInstantiatedDescriptor();

    DocumentExtractorSettings settings = new DocumentExtractorSettings();

    settings.setExtractText(true);
    desc.setSettings(settings);
    Capabilities capabilities = desc.capabilities();

    assertEquals(
        1,
        capabilities
            .creates(ContentCapability.class)
            .filter(cc -> cc.getType() == Text.class)
            .count());

    settings.setExtractText(false);
    desc.setSettings(settings);
    capabilities = desc.capabilities();

    assertEquals(
        0,
        capabilities
            .creates(ContentCapability.class)
            .filter(cc -> cc.getType() == Text.class)
            .count());
  }

  @Test
  public void testImageCapabilities() throws Exception {
    AbstractDocumentExtractorDescriptor<?> desc = getInstantiatedDescriptor();

    DocumentExtractorSettings settings = new DocumentExtractorSettings();

    settings.setExtractImages(true);
    desc.setSettings(settings);
    Capabilities capabilities = desc.capabilities();

    assertEquals(
        1,
        capabilities
            .creates(ContentCapability.class)
            .filter(cc -> cc.getType() == Image.class)
            .count());

    settings.setExtractImages(false);
    desc.setSettings(settings);
    capabilities = desc.capabilities();

    assertEquals(
        0,
        capabilities
            .creates(ContentCapability.class)
            .filter(cc -> cc.getType() == Image.class)
            .count());
  }

  @Test
  public void testMetadata() {
    DocumentExtractorSettings settings = new DocumentExtractorSettings();
    settings.setExtractMetadata(true);
    settings.setExtractText(false);
    settings.setExtractImages(false);

    AbstractDocumentExtractorProcessor<?> processor = getInstantiatedProcessor(settings);

    if (!processor.isMetadataSupported()) return; // No metadata for this

    Item item = createTestItem();

    ProcessorResponse pr = processor.process(item);
    if (pr.hasExceptions()) {
      pr.getExceptions().forEach(Throwable::printStackTrace);
    }
    assertEquals(ProcessorResponse.ok(), pr);
    assertEquals(1, item.getContents().count()); // No additional Content should have been created

    validateMetadata(item.getProperties());
  }

  @Test
  public void testText() {
    DocumentExtractorSettings settings = new DocumentExtractorSettings();
    settings.setExtractMetadata(false);
    settings.setExtractText(true);
    settings.setExtractImages(false);

    AbstractDocumentExtractorProcessor<?> processor = getInstantiatedProcessor(settings);

    Item item = createTestItem();

    ProcessorResponse pr = processor.process(item);
    if (pr.hasExceptions()) {
      pr.getExceptions().forEach(Throwable::printStackTrace);
    }
    assertEquals(ProcessorResponse.ok(), pr);

    if (!processor.isTextSupported()) {
      assertEquals(1, item.getContents().count());
      assertEquals(0, item.getContents(Text.class).count());
    }

    validateText(item.getContents(Text.class).collect(Collectors.toList()));
  }

  @Test
  public void testImages() {
    DocumentExtractorSettings settings = new DocumentExtractorSettings();
    settings.setExtractMetadata(false);
    settings.setExtractText(false);
    settings.setExtractImages(true);

    AbstractDocumentExtractorProcessor<?> processor = getInstantiatedProcessor(settings);

    Item item = createTestItem();

    ProcessorResponse pr = processor.process(item);
    if (pr.hasExceptions()) {
      pr.getExceptions().forEach(Throwable::printStackTrace);
    }
    assertEquals(ProcessorResponse.ok(), pr);

    if (!processor.isImagesSupported()) {
      assertEquals(1, item.getContents().count());
      assertEquals(0, item.getContents(Image.class).count());
    }

    validateImages(item.getContents(Image.class).collect(Collectors.toList()));
  }

  @Test
  public void testInputStream() {
    DocumentExtractorSettings settings = new DocumentExtractorSettings();
    settings.setExtractMetadata(false);
    settings.setExtractText(true);
    settings.setExtractImages(true);

    AbstractDocumentExtractorProcessor<?> processor = getInstantiatedProcessor(settings);

    Item item = createTestItemInputStream();

    ProcessorResponse pr = processor.process(item);
    if (pr.hasExceptions()) {
      pr.getExceptions().forEach(Throwable::printStackTrace);
    }
    assertEquals(ProcessorResponse.ok(), pr);

    if (!processor.isTextSupported()) {
      assertEquals(0, item.getContents(Text.class).count());
    }

    validateText(item.getContents(Text.class).collect(Collectors.toList()));

    if (!processor.isImagesSupported()) {
      assertEquals(0, item.getContents(Image.class).count());
    }

    validateImages(item.getContents(Image.class).collect(Collectors.toList()));
  }

  @Test
  public void testBadFile() {
    DocumentExtractorSettings settings = new DocumentExtractorSettings();
    settings.setExtractMetadata(false);
    settings.setExtractText(true);
    settings.setExtractImages(true);

    AbstractDocumentExtractorProcessor<?> processor = getInstantiatedProcessor(settings);

    Item item = createTestItemBadFile();

    ProcessorResponse pr = processor.process(item);
    if (pr.hasExceptions()) {
      pr.getExceptions().forEach(Throwable::printStackTrace);
    }
    assertEquals(ProcessorResponse.ok(), pr);

    assertEquals(1, item.getContents().count());
  }

  @Test
  public void testBadInputStream() {
    DocumentExtractorSettings settings = new DocumentExtractorSettings();
    settings.setExtractMetadata(false);
    settings.setExtractText(true);
    settings.setExtractImages(true);

    AbstractDocumentExtractorProcessor<?> processor = getInstantiatedProcessor(settings);

    Item item = createTestItemBadInputStream();

    ProcessorResponse pr = processor.process(item);
    if (pr.hasExceptions()) {
      pr.getExceptions().forEach(Throwable::printStackTrace);
    }
    assertEquals(ProcessorResponse.ok(), pr);

    assertEquals(1, item.getContents().count());
  }

  protected AbstractDocumentExtractorDescriptor<?> getInstantiatedDescriptor() throws Exception {
    return getDescriptor().getConstructor().newInstance();
  }

  protected AbstractDocumentExtractorProcessor<?> getInstantiatedProcessor(
      DocumentExtractorSettings settings) {
    try {
      Context context = new SimpleContext();

      AbstractDocumentExtractorDescriptor<?> desc = getInstantiatedDescriptor();
      desc.setSettings(settings);

      return desc.create(context);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected Item createTestItem() {
    TestItem item = new TestItem();

    item.createContent(FileContent.class)
        .withDescription("Test Document")
        .withData(this::getTestFile)
        .save();

    return item;
  }

  protected Item createTestItemInputStream() {
    TestItem item = new TestItem();

    Supplier<InputStream> supplier =
        () -> {
          try {
            return new FileInputStream(getTestFile());
          } catch (FileNotFoundException e) {
            return InputStream.nullInputStream();
          }
        };

    item.createContent(InputStreamContent.class)
        .withDescription("Test Document")
        .withData(supplier)
        .save();

    return item;
  }

  private Item createTestItemBadFile() {
    TestItem item = new TestItem();

    File f;
    try {
      f = Paths.get(getClass().getResource("badFile.txt").toURI()).toFile();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    item.createContent(FileContent.class).withDescription("Test Document").withData(f).save();

    return item;
  }

  protected Item createTestItemBadInputStream() {
    TestItem item = new TestItem();

    Supplier<InputStream> supplier = () -> getClass().getResourceAsStream("badFile.txt");
    ;

    item.createContent(InputStreamContent.class)
        .withDescription("Test Document")
        .withData(supplier)
        .save();

    return item;
  }
}