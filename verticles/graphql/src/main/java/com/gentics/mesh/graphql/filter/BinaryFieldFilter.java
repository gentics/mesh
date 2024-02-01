package com.gentics.mesh.graphql.filter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.node.field.HibImageDataField;

public class BinaryFieldFilter extends ImageDataFieldFilter<HibBinary, HibBinaryField> {

	private static final Map<String, BinaryFieldFilter> instances = Collections.synchronizedMap(new HashMap<>());

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
		super("BinaryFieldFilter", "Filters over binary field data, including the binary", "binary", BinaryFilter.filter(), owner);
	}

	@Override
	protected List<FilterField<HibBinaryField, ?>> getFilters() {
		List<FilterField<HibBinaryField, ?>> filters = super.getFilters();
		filters.add(makeWrappedFieldFilter("fileName", "Filters by filename", StringFilter.filter(), HibImageDataField::getFileName));
		filters.add(makeWrappedFieldFilter("mimeType", "Filters by MIME type", StringFilter.filter(), HibImageDataField::getMimeType));
		return filters;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected BinaryFilter getBinaryFilter() {
		return BinaryFilter.filter();
	}
}
