package com.gentics.mesh.graphql.filter;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;

public class S3BinaryFieldFilter extends ImageDataFieldFilter<S3HibBinaryField> {

	private static Map<String, S3BinaryFieldFilter> instances = new HashMap<>();

	/**
	 * Create a binary filter.
	 * 
	 * @param context
	 * @return
	 */
	public static S3BinaryFieldFilter filter(String owner) {
		return instances.computeIfAbsent(owner, o -> new S3BinaryFieldFilter(o));
	}

	private S3BinaryFieldFilter(String owner) {
		super("S3BinaryFieldFilter", "Filters over S3 binary field data, including the binary", owner);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected S3BinaryFilter getBinaryFilter() {
		return S3BinaryFilter.filter();
	}
}
