package com.gentics.mesh.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.AbstractMap;
import java.util.stream.Stream;

import org.junit.Test;

public class StreamUtilTest {
	@Test
	public void testOfNullable() {
		Stream<String> stream = StreamUtil.ofNullable("test", "1", null, "2", null);

		assertThat(stream).containsExactly("test", "1", "2");
	}

	@Test
	public void assertSingleNull() {
		assertThat(StreamUtil.ofNullable((String) null)).isEmpty();
	}

	@Test
	public void testUnique() {
		assertThat(
			Stream.of(1,2,3,3,4,2).filter(StreamUtil.unique())
		).containsExactly(1,2,3,4);
	}

	@Test
	public void testUniqueBy() {
		assertThat(Stream.of(
			new AbstractMap.SimpleImmutableEntry<>("a", 1),
			new AbstractMap.SimpleImmutableEntry<>("b", 2),
			new AbstractMap.SimpleImmutableEntry<>("c", 3),
			new AbstractMap.SimpleImmutableEntry<>("c", 5),
			new AbstractMap.SimpleImmutableEntry<>("d", 2),
			new AbstractMap.SimpleImmutableEntry<>("a", 123)
		).filter(StreamUtil.uniqueBy(AbstractMap.SimpleImmutableEntry::getKey))
		.map(AbstractMap.SimpleImmutableEntry::getKey))
		.containsExactly("a", "b", "c", "d");
	}
}
