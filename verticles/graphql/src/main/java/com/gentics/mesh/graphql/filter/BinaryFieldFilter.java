package com.gentics.mesh.graphql.filter;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.data.node.field.HibBinaryField;

public class BinaryFieldFilter extends ImageDataFieldFilter<HibBinaryField> {

	private static final Map<String, BinaryFieldFilter> instances = new HashMap<>();

	/**
	 * Create a binary filter.
	 * 
	 * @param context
	 * @return
	 */
	public static BinaryFieldFilter filter(String owner) {
		return instances.computeIfAbsent(owner, o -> new BinaryFieldFilter(o));
	}

	private BinaryFieldFilter(String owner) {
		super("BinaryFieldFilter", "Filters over binary field data, including the binary", owner);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected BinaryFilter getBinaryFilter() {
		return BinaryFilter.filter();
	}
}
