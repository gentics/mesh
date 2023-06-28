package com.gentics.mesh.graphql.filter;

import java.util.List;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;

/**
 * Binary filter.
 * 
 * @author plyhun
 *
 */
public class S3BinaryFilter extends ImageDataFilter<S3HibBinary> {

	private static final String NAME = "S3BinaryFilter";

	private static S3BinaryFilter instance;

	/**
	 * Create a node filter for the given context.
	 * 
	 * @param context
	 * @return
	 */
	public static S3BinaryFilter filter() {
		if (instance == null) {
			instance = new S3BinaryFilter(NAME, "Filter over binaries");
		}
		return instance;
	}

	private S3BinaryFilter(String name, String description) {
		super(name, description, "S3BINARY");
	}

	@Override
	public List<FilterField<S3HibBinary, ?>> getFilters() {
		List<FilterField<S3HibBinary, ?>> filters = super.getFilters();
		filters.add(new MappedFilter<>(owner, "key", "Filters by S3 object key", StringFilter.filter(),	content -> content == null ? null : content.getS3ObjectKey()));
		filters.add(new MappedFilter<>(owner, "filename", "Filters by S3 object filename", StringFilter.filter(),	content -> content == null ? null : content.getFileName()));
		filters.add(new MappedFilter<>(owner, "mime", "Filters by S3 object MIME type", StringFilter.filter(),	content -> content == null ? null : content.getMimeType()));
		return filters;
	}
}