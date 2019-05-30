package com.gentics.mesh.util;

import org.junit.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

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
}
