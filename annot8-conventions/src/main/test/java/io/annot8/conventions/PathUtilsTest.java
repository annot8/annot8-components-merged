package io.annot8.conventions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PathUtilsTest {

  private String TEST_PATH = "hello/world";
  private String[] TEST_PARTS = new String[] {"hello", "world"};

  @Test
  public void joinIterator() {
    String path = PathUtils.join(Arrays.asList(TEST_PARTS).iterator());
    assertThat(path).isEqualTo(TEST_PATH);
  }

  @Test
  public void joinStream() {
    String path = PathUtils.join(Arrays.asList(TEST_PARTS).stream());
    assertThat(path).isEqualTo(TEST_PATH);
  }

  @Test
  public void joinArray() {
    String path = PathUtils.join(TEST_PARTS);
    assertThat(path).isEqualTo(TEST_PATH);
  }

  @Test
  public void joinIterable() {
    String path = PathUtils.join(Arrays.asList(TEST_PARTS));
    assertThat(path).isEqualTo(TEST_PATH);
  }

  @Test
  public void split() {
    String[] path = PathUtils.split(TEST_PATH);
    assertThat(path).isEqualTo(TEST_PARTS);
  }

  @Test
  public void splitToIterable() {
    Iterable<String> path = PathUtils.splitToIterable(TEST_PATH);
    assertThat(path).containsExactly(TEST_PARTS);
  }

  @Test
  public void splitToStream() {
    Stream<String> path = PathUtils.splitToStream(TEST_PATH);
    assertThat(path).containsExactly(TEST_PARTS);
  }

  @Test
  public void splitToList() {
    List<String> path = PathUtils.splitToList(TEST_PATH);
    assertThat(path).containsExactly(TEST_PARTS);
  }
}
