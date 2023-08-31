package com.gentics.mesh.graphql.filter;

import java.util.Collection;
import java.util.List;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.graphqlfilter.filter.operation.JoinPart;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.util.Pair;

/**
 * Binary filter.
 * 
 * @author plyhun
 *
 */
public class BinaryFilter extends ImageDataFilter<HibBinary> {

	private static final String NAME = "BinaryFilter";

	private static BinaryFilter instance;

	private final GraphQLContext context;

	/**
	 * Create a binary filter.
	 * 
	 * @param context
	 * @return
	 */
	public static BinaryFilter filter(GraphQLContext context) {
		if (instance == null) {
			instance = new BinaryFilter(NAME, "Filter over binaries", context);
		}
		return instance;
	}

	private BinaryFilter(String name, String description, GraphQLContext context) {
		super(name, description, "BINARY");
		this.context = context;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<FilterField<HibBinary, ?>> getFilters() {
		List<FilterField<HibBinary, ?>> filters = super.getFilters();
		filters.add(new MappedFilter<>(owner, "checksum", "Filters by SHA512 checksum", StringFilter.filter(),
				content -> content == null ? null : content.getSHA512Sum()));
		filters.add(new MappedFilter<>(owner, "variants", "Filters by image variants", 
				ListFilter.imageVariantListFilter(context),
				content -> content == null ? null : (Collection<HibImageVariant>) CommonTx.get().binaryDao().getVariants(content, context).list(), Pair.pair("variants", new JoinPart("IMAGEVARIANT", "value"))));
		return filters;
	}
}
