/* Annot8 (annot8.io) - Licensed under Apache-2.0. */
package io.annot8.components.db.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.annot8.api.components.responses.ProcessorResponse;
import io.annot8.api.components.responses.ProcessorResponse.Status;
import io.annot8.api.data.Content;
import io.annot8.api.data.Item;
import io.annot8.common.data.content.ColumnMetadata;
import io.annot8.common.data.content.FileContent;
import io.annot8.common.data.content.TableContent;
import io.annot8.common.data.content.TableMetadata;
import io.annot8.testing.testimpl.TestItem;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class SQLiteDatabaseTableExtractorTest extends AbstractSQLiteDataTest {

  @Test
  public void testProcess() {
    Item item = new TestItem();
    FileContent content = mockFileContent("test.db");
    ((TestItem) item).save(content);
    ProcessorResponse response = null;
    try (SQLiteDatabaseTableExtractor.Processor extractor =
        new SQLiteDatabaseTableExtractor.Processor()) {
      response = extractor.process(item);
    } catch (Exception e) {
      fail("Test should not throw an exception here", e);
    }

    assertEquals(Status.OK, response.getStatus());

    long contentCount = item.getContents(TableContent.class).count();

    assertEquals(2, contentCount);

    item.getContents(TableContent.class)
        .forEach(
            c ->
                assertTrue(
                    c.getProperties().has(SQLiteDatabaseTableExtractor.Processor.PROPERTY_TYPE)));

    String[] expectedTables = new String[] {"test", "test2"};

    List<String> tableNames =
        item.getContents(TableContent.class)
            .map(
                c ->
                    c.getProperties()
                        .get(SQLiteDatabaseTableExtractor.Processor.PROPERTY_NAME, String.class)
                        .orElse(null))
            .collect(Collectors.toList());

    assertThat(tableNames).containsExactlyInAnyOrder(expectedTables);

    List<TableMetadata> metadata =
        item.getContents(TableContent.class)
            .map(Content::getProperties)
            .map(p -> p.get(SQLiteDatabaseTableExtractor.Processor.PROPERTY_TYPE).get())
            .map(TableMetadata.class::cast)
            .collect(Collectors.toList());

    assertThat(metadata.stream().map(TableMetadata::getName))
        .containsExactlyInAnyOrder(expectedTables);

    TableMetadata tableMetadata = getMetadata(metadata, "test");
    assertEquals("TABLE", tableMetadata.getType());

    Collection<ColumnMetadata> testTablecolumns = tableMetadata.getColumns();
    ColumnMetadata test = new ColumnMetadata("test", 2000000000);
    ColumnMetadata id = new ColumnMetadata("id", 2000000000);
    ColumnMetadata someValue = new ColumnMetadata("someValue", 2000000000);

    assertThat(testTablecolumns).containsExactlyInAnyOrder(test, id, someValue);
  }

  private TableMetadata getMetadata(List<TableMetadata> metadata, String tableName) {
    return metadata.stream().filter(m -> m.getName().equals(tableName)).findFirst().get();
  }
}
