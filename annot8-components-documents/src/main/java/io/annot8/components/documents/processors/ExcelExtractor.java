/* Annot8 (annot8.io) - Licensed under Apache-2.0. */
package io.annot8.components.documents.processors;

import io.annot8.api.capabilities.Capabilities;
import io.annot8.api.components.annotations.ComponentDescription;
import io.annot8.api.components.annotations.ComponentName;
import io.annot8.api.components.annotations.ComponentTags;
import io.annot8.api.components.annotations.SettingsClass;
import io.annot8.api.components.responses.ProcessorResponse;
import io.annot8.api.context.Context;
import io.annot8.api.data.Item;
import io.annot8.api.settings.Description;
import io.annot8.common.components.AbstractProcessor;
import io.annot8.common.components.AbstractProcessorDescriptor;
import io.annot8.common.components.capabilities.SimpleCapabilities;
import io.annot8.common.data.content.FileContent;
import io.annot8.common.data.content.InputStreamContent;
import io.annot8.common.data.content.Row;
import io.annot8.common.data.content.Table;
import io.annot8.common.data.content.TableContent;
import io.annot8.components.documents.data.SimpleTable;
import io.annot8.components.documents.data.WorksheetRow;
import io.annot8.conventions.PropertyKeys;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetVisibility;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.javatuples.Pair;

@ComponentName("Excel (XLS and XLSX) Extractor")
@ComponentDescription("Extracts content from Excel (*.xls and *.xlsx) files into a table")
@ComponentTags({"documents", "excel", "xls", "xlsx", "extractor", "metadata"})
@SettingsClass(ExcelExtractor.Settings.class)
public class ExcelExtractor
    extends AbstractProcessorDescriptor<ExcelExtractor.Processor, ExcelExtractor.Settings> {

  @Override
  protected Processor createComponent(Context context, Settings settings) {
    return new Processor(settings);
  }

  @Override
  public Capabilities capabilities() {
    SimpleCapabilities.Builder builder =
        new SimpleCapabilities.Builder()
            .withProcessesContent(FileContent.class)
            .withProcessesContent(InputStreamContent.class)
            .withCreatesContent(TableContent.class);

    if (getSettings().isRemoveSourceContent())
      builder = builder.withDeletesContent(FileContent.class);

    return builder.build();
  }

  public static class Processor extends AbstractProcessor {
    private final Settings settings;

    public Processor(Settings settings) {
      this.settings = settings;
    }

    @Override
    public ProcessorResponse process(Item item) {
      Stream.concat(
              item.getContents(FileContent.class).map(this::mapToWorkbook),
              item.getContents(InputStreamContent.class).map(this::mapToWorkbook))
          .filter(Objects::nonNull)
          .forEach(
              f -> {
                processWorkbook(item, f.getValue0(), f.getValue1());

                if (settings.isRemoveSourceContent()) item.removeContent(f.getValue1());
              });

      return ProcessorResponse.ok();
    }

    protected Pair<Workbook, String> mapToWorkbook(FileContent fileContent) {
      try {
        return new Pair<>(WorkbookFactory.create(fileContent.getData()), fileContent.getId());
      } catch (IOException e) {
        log().warn("Unable to process file {}", fileContent.getData().getAbsolutePath(), e);
        return null;
      }
    }

    protected Pair<Workbook, String> mapToWorkbook(InputStreamContent inputStream) {
      try {
        return new Pair<>(WorkbookFactory.create(inputStream.getData()), inputStream.getId());
      } catch (IOException e) {
        log().warn("Unable to process InputStream {}", inputStream.getId(), e);
        return null;
      }
    }

    private void processWorkbook(Item item, Workbook workbook, String parentId) {
      item.getProperties()
          .set(PropertyKeys.PROPERTY_KEY_VERSION, workbook.getSpreadsheetVersion().name());

      for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
        if (settings.getSkipSheets().contains(workbook.getSheetName(i))) {
          log().info("Skipping sheet {}", workbook.getSheetName(i));
          continue;
        }

        processSheet(
            item,
            workbook.getSheetAt(i),
            i,
            i == workbook.getActiveSheetIndex(),
            workbook.getSheetVisibility(i) == SheetVisibility.VISIBLE,
            parentId);
      }
    }

    private void processSheet(
        Item item, Sheet sheet, int sheetIndex, boolean active, boolean visible, String parentId) {
      Table table = worksheetTable(sheet, settings.isFirstRowHeader(), settings.getSkipRows());

      item.createContent(TableContent.class)
          .withData(table)
          .withDescription(sheet.getSheetName())
          .withProperty(PropertyKeys.PROPERTY_KEY_PAGE, sheetIndex)
          .withProperty("active", active)
          .withProperty("visible", visible)
          .withPropertyIfPresent(PropertyKeys.PROPERTY_KEY_PARENT, Optional.ofNullable(parentId))
          .save();
    }

    private Table worksheetTable(Sheet sheet, boolean firstRowHeader, int skipRows) {
      Iterator<org.apache.poi.ss.usermodel.Row> rows = sheet.rowIterator();

      int rowIndex = 0;

      List<String> headerColumns;
      for (int i = 0; i < skipRows; i++) {
        if (rows.hasNext()) rows.next();
      }

      if (firstRowHeader && rows.hasNext()) {
        org.apache.poi.ss.usermodel.Row header = rows.next();
        rowIndex++;

        headerColumns = new ArrayList<>(header.getLastCellNum());
        for (int i = 0; i < header.getLastCellNum(); i++) {
          Cell cell = header.getCell(i);
          if (cell == null) {
            headerColumns.add("");
          } else {
            headerColumns.add(cell.getStringCellValue());
          }
        }

      } else {
        headerColumns = new ArrayList<>();
      }

      List<Row> lRows = new ArrayList<>(sheet.getLastRowNum());

      while (rows.hasNext()) {
        WorksheetRow row = new WorksheetRow(rows.next(), rowIndex, headerColumns);
        rowIndex++;

        if (row.isEmpty()) continue;

        lRows.add(row);
      }

      int columnCount = lRows.stream().mapToInt(Row::getColumnCount).max().orElse(-1);

      if (headerColumns.isEmpty()) {
        for (int i = 0; i < columnCount; i++) {
          headerColumns.add("Column " + CellReference.convertNumToColString(i));
        }
      } else if (columnCount > headerColumns.size()) {
        for (int i = headerColumns.size(); i < columnCount; i++) {
          headerColumns.add("");
        }
      }

      return new SimpleTable(headerColumns, lRows);
    }
  }

  public static class Settings implements io.annot8.api.settings.Settings {
    private List<String> extensions = List.of("xls", "xlsx");
    private boolean removeSourceContent = true;
    private boolean firstRowHeader = true;
    private int skipRows = 0;
    private List<String> skipSheets = Collections.emptyList();

    public boolean validate() {
      return extensions != null && skipSheets != null;
    }

    @Description(
        "The list of file extensions on which this processor will act (case insensitive). If empty, then the processor will act on all files.")
    public List<String> getExtensions() {
      return extensions;
    }

    public void setExtensions(List<String> extensions) {
      this.extensions = extensions.stream().map(String::toLowerCase).collect(Collectors.toList());
    }

    @Description(
        value = "Should the source Content be removed after successful processing?",
        defaultValue = "true")
    public boolean isRemoveSourceContent() {
      return removeSourceContent;
    }

    public void setRemoveSourceContent(boolean removeSourceContent) {
      this.removeSourceContent = removeSourceContent;
    }

    @Description(
        value = "Is the first row of the spreadsheet a header row, to be used for column names?",
        defaultValue = "true")
    public boolean isFirstRowHeader() {
      return firstRowHeader;
    }

    public void setFirstRowHeader(boolean firstRowHeader) {
      this.firstRowHeader = firstRowHeader;
    }

    @Description(
        value =
            "The number of rows to skip (prior to reading the header, if firstRowHeader is true)",
        defaultValue = "0")
    public int getSkipRows() {
      return skipRows;
    }

    public void setSkipRows(int skipRows) {
      this.skipRows = skipRows;
    }

    @Description(value = "The name of any spreadsheets within a workbook which should be skipped")
    public List<String> getSkipSheets() {
      return skipSheets;
    }

    public void setSkipSheets(List<String> skipSheets) {
      this.skipSheets = skipSheets;
    }
  }
}
