/* Annot8 (annot8.io) - Licensed under Apache-2.0. */
package io.annot8.components.easyocr.processors;

import io.annot8.api.capabilities.Capabilities;
import io.annot8.api.components.annotations.ComponentDescription;
import io.annot8.api.components.annotations.ComponentName;
import io.annot8.api.components.annotations.SettingsClass;
import io.annot8.api.context.Context;
import io.annot8.api.exceptions.BadConfigurationException;
import io.annot8.api.settings.Description;
import io.annot8.common.components.AbstractProcessorDescriptor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@ComponentName("Remote EasyOCR")
@ComponentDescription("Perform OCR using a remote EasyOCR Server")
@SettingsClass(RemoteEasyOCR.Settings.class)
public class RemoteEasyOCR
    extends AbstractProcessorDescriptor<RemoteEasyOCR.Processor, RemoteEasyOCR.Settings> {

  @Override
  public Capabilities capabilities() {
    return AbstractEasyOCRProcessor.capabilities();
  }

  @Override
  protected Processor createComponent(Context context, Settings settings) {
    return new Processor(settings);
  }

  public static class Processor extends AbstractEasyOCRProcessor {

    private final Settings settings;

    public Processor(Settings settings) {
      this(settings, 3, 500);
    }

    public Processor(Settings settings, int delay, int retries) {
      super(delay, retries);
      this.settings = settings;
      startEasyOCR();
    }

    @SuppressWarnings("java:S1141")
    private void startEasyOCR() {
      if (settings.isInitialize()) {
        try {
          log().info("Initializing EasyOCR server at {}", settings.getUrl());

          tryInitializeEasyOCR(
              new InitSettings(settings.getLang(), settings.isDownload(), settings.isGpu()));
        } catch (BadConfigurationException e) {
          log().error("Easy-OCR start error", e);
          throw e;
        }
      } else {
        log().debug("Not initializing EasyOCR server at {}", settings.getUrl());
      }
    }

    protected String getUrl(String string) {
      try {
        return new URL(new URL(settings.getUrl()), string).toString();
      } catch (MalformedURLException e) {
        throw new BadConfigurationException("Supplied URL invalid", e);
      }
    }
  }

  public static class Settings implements io.annot8.api.settings.Settings {

    private static final boolean DEFAULT_INITIALIZE = false;
    private static final boolean DEFAULT_DOWNLOAD = false;
    private static final boolean DEFAULT_GPU = false;
    private static final String DEFAULT_LANGS = "en";

    private String url;
    private boolean initialize = DEFAULT_INITIALIZE;
    private boolean download = DEFAULT_DOWNLOAD;
    private boolean gpu = DEFAULT_GPU;
    private String langs = DEFAULT_LANGS;

    @Override
    public boolean validate() {
      try {
        new URL(url);
      } catch (MalformedURLException e) {
        return false;
      }
      return langs != null && url != null;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    @Description("URL of the EasyOCR server")
    public String getUrl() {
      return url;
    }

    @Description(
        "Set true to initialize the remote server, otherwise we assume already initialized")
    public boolean isInitialize() {
      return initialize;
    }

    public void setInitialize(boolean initialize) {
      this.initialize = initialize;
    }

    @Description("Set true to allow language models to be downloaded")
    public boolean isDownload() {
      return download;
    }

    public void setDownload(boolean download) {
      this.download = download;
    }

    @Description("Set true to use GPU")
    public boolean isGpu() {
      return gpu;
    }

    public void setGpu(boolean gpu) {
      this.gpu = gpu;
    }

    public void setLangs(String langs) {
      this.langs = langs;
    }

    @Description(
        "Comma separated list of languages expected in the text, see https://www.jaided.ai/easyocr/ for supported languages")
    public String getLang() {
      return langs;
    }

    public static class Builder {
      private List<String> langs = new ArrayList<>();
      private boolean initialize = DEFAULT_INITIALIZE;
      private boolean download = DEFAULT_DOWNLOAD;
      private boolean gpu = DEFAULT_GPU;
      private String url;

      public Builder initialize() {
        this.initialize = true;
        return this;
      }

      public Builder allowDownload() {
        this.download = true;
        return this;
      }

      public Builder useGpu() {
        this.gpu = true;
        return this;
      }

      public Builder withLang(String lang) {
        this.langs.add(lang);
        return this;
      }

      public Builder withUrl(String url) {
        this.url = url;
        return this;
      }

      public Settings build() {
        Settings settings = new Settings();
        settings.setInitialize(initialize);
        settings.setDownload(download);
        settings.setGpu(gpu);
        if (!langs.isEmpty()) {
          settings.setLangs(String.join(",", langs));
        }
        settings.setUrl(url);
        return settings;
      }
    }

    public static Builder builder() {
      return new Builder();
    }
  }
}
