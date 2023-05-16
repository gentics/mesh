package com.gentics.mesh.graphql.filter;

import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;

public class S3BinaryFieldFilter extends ImageDataFieldFilter<S3HibBinaryField> {

	private static S3BinaryFieldFilter instance;

	/**
	 * Create a binary filter.
	 * 
	 * @param context
	 * @return
	 */
	public static S3BinaryFieldFilter filter() {
		if (instance == null) {
			instance = new S3BinaryFieldFilter();
		}
		return instance;
	}

	private S3BinaryFieldFilter() {
		super("S3BinaryFieldFilter", "Filters over S3 binary field data, including the binary");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected S3BinaryFilter getBinaryFilter() {
		return S3BinaryFilter.filter();
	}
}
