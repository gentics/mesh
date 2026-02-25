package com.gentics.mesh.check;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.core.rest.common.FieldTypes;

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

	@Override
	protected FieldTypes getListFieldType() {
		return null;
	}
}
