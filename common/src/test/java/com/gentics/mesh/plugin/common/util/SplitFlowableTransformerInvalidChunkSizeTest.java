package com.gentics.mesh.plugin.common.util;

import io.reactivex.Flowable;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class SplitFlowableTransformerInvalidChunkSizeTest {

	@Test
	public void testInvalidChunkSize() {
		try {
			Flowable.just("abc".getBytes(StandardCharsets.UTF_8))
				.compose(RxUtil.split(0))
				.ignoreElements();

			Assertions.fail("RxUtil.split(0) should throw an IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// This is supposed to happen.
		}
	}
}
