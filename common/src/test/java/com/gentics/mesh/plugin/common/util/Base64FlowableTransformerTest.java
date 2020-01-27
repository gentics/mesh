package com.gentics.mesh.plugin.common.util;


import io.reactivex.Flowable;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the Base64 encoding of Flowables.
 */
@RunWith(Parameterized.class)
public class Base64FlowableTransformerTest {

	@Parameters(name = "{index} input: {0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(
			new Object[] { "" },
			new Object[] { "a" },
			new Object[] { "ä" },
			new Object[] { "aä" },
			new Object[] { "äö" },
			new Object[] { "aäö" },
			new Object[] { "aäoö" },
			new Object[] { "äöü" },
			new Object[] { "aäöü" },
			new Object[] { "aäoöü" },
			new Object[] { "aäoöuü" },
			new Object[] { "ab" },
			new Object[] { "abc" },
			new Object[] { "abcd" },
			new Object[] { "abcde" },
			new Object[] { "abcdef" },
			new Object[] { "abcdefg" },
			new Object[] { "abcdefgh" });
	}

	@Parameter
	public String input;

	@Test
	public void testBase64FlowableTransformer() {
		byte[] expected = Base64.getEncoder().encode(input.getBytes(StandardCharsets.UTF_8));

		List<byte[]> encodedChunks = Flowable.just(input.getBytes(StandardCharsets.UTF_8))
			.compose(RxUtil.base64())
			.toList()
			.blockingGet();

		byte[] result = null;

		for (byte[] chunk : encodedChunks) {
			result = ArrayUtils.addAll(result, chunk);
		}

		assertThat(result)
			.as("Encoded bytes")
			.isEqualTo(expected);
	}
}
