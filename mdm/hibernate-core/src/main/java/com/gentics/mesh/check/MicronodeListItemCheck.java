package com.gentics.mesh.check;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Test for checking consistency of "micronodelistitem"
 */
public class MicronodeListItemCheck extends AbstractListItemTableCheck {
	@Override
	public String getName() {
		return "micronodelistitem";
	}

	@Override
	protected List<Pair<String, String>> optMicroNodeReferences() {
		return Arrays.asList(Pair.of("valueoruuid", "microschemaversion_dbuuid"));
	}
}
