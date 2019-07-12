package com.gentics.mesh.image;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gentics.mesh.core.data.binary.Binary;

public final class ImageTestUtil {
	private ImageTestUtil() {
	}

	public static Binary createMockedBinary(String filePath) {
		Binary mock = mock(Binary.class);
		when(mock.openBlockingStream()).thenReturn(() -> ImageTestUtil.class.getResourceAsStream(filePath));
		when(mock.getSHA512Sum()).thenReturn(filePath);
		return mock;
	}
}
