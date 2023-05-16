package com.gentics.mesh.graphql.filter;

import com.gentics.mesh.core.data.node.field.HibBinaryField;

public class BinaryFieldFilter extends ImageDataFieldFilter<HibBinaryField> {

	private static BinaryFieldFilter instance;

	/**
	 * Create a binary filter.
	 * 
	 * @param context
	 * @return
	 */
	public static BinaryFieldFilter filter() {
		if (instance == null) {
			instance = new BinaryFieldFilter();
		}
		return instance;
	}

	private BinaryFieldFilter() {
		super("BinaryFieldFilter", "Filters over binary field data, including the binary");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected BinaryFilter getBinaryFilter() {
		return BinaryFilter.filter();
	}
}
