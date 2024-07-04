package com.gentics.mesh.plugin.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.error.InvalidArgumentException;

import io.reactivex.Flowable;

@RunWith(Parameterized.class)
public class SplitFlowableTransformerTest {

	@Parameters(name = "{index} input: {0}, chunkSize: {1}, expected result: {2}")
	public static Collection<Object[]> data() {
		// We use strings as input, because the test data is easier to define this way. Since the split operator
		// works with byte arrays anyways, only ASCII characters are used.
		return Arrays.asList(
			new Object[] { "abc", 2, new String[] { "ab", "c" } },
			new Object[] { "abcd", 2, new String[] { "ab", "cd" } },
			new Object[] { "abcde", 2, new String[] { "ab", "cd", "e" } },
			new Object[] { "abcde", 4, new String[] { "abcd", "e" } },
			new Object[] { "abcde", 10, new String[] { "abcde" } });
	}

	@Parameter
	public String input;

	@Parameter(1)
	public int chunkSize;

	@Parameter(2)
	public String[] expected;

	@Test
	public void testSplitFlowableTransformer() throws InvalidArgumentException {
		test(Collections.singletonList(input.getBytes(StandardCharsets.UTF_8)));
	}

	/**
	 * Tests with multiple byte arrays as input.
	 */
	@Test
	public void testChunkedInput() {
		IntStream.rangeClosed(2, 5).forEach(i ->
			test(MiscUtil.splitBytes(input.getBytes(StandardCharsets.UTF_8), i)));
	}

	private void test(List<byte[]> inputBytes) {
		List<String> chunks = Flowable.fromIterable(inputBytes)
			.compose(RxUtil.split(chunkSize))
			.map(String::new)
			.toList()
			.blockingGet();

		assertThat(chunks)
			.as("Created chunks")
			.containsExactly(expected);
	}
}
