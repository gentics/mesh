package com.gentics.mesh.check;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.core.rest.common.FieldTypes;

/**
 * Test for checking consistency of "s3binaryfieldref"
 */
public class S3BinaryFieldRefCheck extends AbstractListItemTableCheck {
	@Override
	public String getName() {
		return "s3binaryfieldref";
	}

	@Override
	protected List<Pair<String, String>> optValueReferencesToCheck() {
		return Arrays.asList(Pair.of("valueoruuid", "s3binary"));
	}

	@Override
	protected FieldTypes getListFieldType() {
		return null;
	}
}
