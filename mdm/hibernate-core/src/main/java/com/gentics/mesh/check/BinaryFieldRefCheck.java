package com.gentics.mesh.check;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Test for checking consistency of "binaryfieldref"
 */
public class BinaryFieldRefCheck extends AbstractListItemTableCheck {
	@Override
	public String getName() {
		return "binaryfieldref";
	}

	@Override
	protected List<Pair<String, String>> optValueReferencesToCheck() {
		return Arrays.asList(Pair.of("valueoruuid", "binary"));
	}
}
