package com.gentics.mesh.graphql.filter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.graphqlfilter.filter.operation.JoinPart;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.node.field.HibImageDataField;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.util.Pair;

public class BinaryFieldFilter extends ImageDataFieldFilter<HibBinary, HibBinaryField> {

	private static final Map<String, BinaryFieldFilter> instances = Collections.synchronizedMap(new HashMap<>());

	private final GraphQLContext context;

	/**
	 * Create a binary filter.
	 * 
	 * @param context
	 * @return
	 */
	public static BinaryFieldFilter filter(String owner, GraphQLContext context) {
		return instances.computeIfAbsent(owner, o -> new BinaryFieldFilter(o, context));
	}

	private BinaryFieldFilter(String owner, GraphQLContext context) {
		super("BinaryFieldFilter", "Filters over binary field data, including the binary", "binary", BinaryFilter.filter(context), owner);
		this.context = context;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<FilterField<HibBinaryField, ?>> getFilters() {
		List<FilterField<HibBinaryField, ?>> filters = super.getFilters();
		filters.add(makeWrappedFieldFilter("filename", "Filters by filename", StringFilter.filter(), HibImageDataField::getFileName));
		filters.add(makeWrappedFieldFilter("mime", "Filters by MIME type", StringFilter.filter(), HibImageDataField::getMimeType));
		filters.add(new MappedFilter<>(owner, "variants", "Filters by image variants", 
				ListFilter.imageVariantListFilter(context, "BINARYFIELD"),
				content -> content == null ? null : (Collection<HibImageVariant>) CommonTx.get().imageVariantDao().getVariants(content, context).list(), Pair.pair("variants", new JoinPart("IMAGEVARIANT", "uuid"))));
		return filters;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected BinaryFilter getBinaryFilter() {
		return BinaryFilter.filter(context);
	}
}
