package com.gentics.mesh.graphql.filter;

import java.util.List;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.core.data.binary.HibBinary;

/**
 * Binary filter.
 * 
 * @author plyhun
 *
 */
public class BinaryFilter extends ImageDataFilter<HibBinary> {

	private static final String NAME = "BinaryFilter";

	private static BinaryFilter instance;

	/**
	 * Create a binary filter.
	 * 
	 * @param context
	 * @return
	 */
	public static BinaryFilter filter() {
		if (instance == null) {
			instance = new BinaryFilter(NAME, "Filter over binaries");
		}
		return instance;
	}

	private BinaryFilter(String name, String description) {
		super(name, description, "BINARY");
	}

	@Override
	public List<FilterField<HibBinary, ?>> getFilters() {
		List<FilterField<HibBinary, ?>> filters = super.getFilters();
		filters.add(new MappedFilter<>(owner, "sha512sum", "Filters by SHA512 checksum", StringFilter.filter(),
				content -> content == null ? null : content.getSHA512Sum()));
		return filters;
	}
}
