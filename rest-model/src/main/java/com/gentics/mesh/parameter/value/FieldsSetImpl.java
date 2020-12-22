package com.gentics.mesh.parameter.value;

import java.util.Arrays;
import java.util.HashSet;

/**
 * @see FieldsSet
 */
public class FieldsSetImpl extends HashSet<String> implements FieldsSet {

	private static final long serialVersionUID = 6436259595505383777L;

	public FieldsSetImpl(String value) {
		super(Arrays.asList(value.split(",")));
	}

	public FieldsSetImpl() {
	}

}
