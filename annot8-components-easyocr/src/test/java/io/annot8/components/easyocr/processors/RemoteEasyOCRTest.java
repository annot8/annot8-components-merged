/* Annot8 (annot8.io) - Licensed under Apache-2.0. */
package io.annot8.components.easyocr.processors;

import static io.annot8.components.easyocr.processors.TestUtil.checkCanProcessFile;
import static io.annot8.components.easyocr.processors.TestUtil.checkCanProcessImage;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.annot8.api.components.Processor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class RemoteEasyOCRTest {

  String url = "http://localhost:8080";

  @Test
  @Disabled("Disabled as it requires EasyOCR to be installed, see README.md")
  public void integrationTest() throws Exception {
    RemoteEasyOCR desc = new RemoteEasyOCR();
    try (Processor ocr =
        desc.createComponent(
            null, RemoteEasyOCR.Settings.builder().withUrl(url).initialize().build())) {
      checkCanProcessFile(ocr);
      checkCanProcessImage(ocr);
    }
  }

  @Test
  public void hasCapabilitites() throws Exception {
    RemoteEasyOCR desc = new RemoteEasyOCR();
    assertNotNull(desc.capabilities());
  }
}
