package com.gentics.mesh.graphql.filter;

import java.util.List;

import com.gentics.graphqlfilter.filter.BooleanFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.operation.JoinPart;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.util.Pair;

/**
 * Filter schemas.
 */
public class SchemaFilter extends SchemaElementFilter<SchemaResponse, SchemaVersionModel, SchemaReference, HibSchema, HibSchemaVersion> {

	private static final ElementType ELEMENT = ElementType.SCHEMA;
	private static final String NAME = "SchemaFilter";

	public static SchemaFilter filter(GraphQLContext context) {
		return context.getOrStore(NAME, () -> new SchemaFilter(context));
	}

	private SchemaFilter(GraphQLContext context) {
		super(context, NAME, "Filters schemas", ELEMENT);
	}

	@Override
	protected List<FilterField<HibSchema, ?>> getFilters() {
		List<FilterField<HibSchema, ?>> filters = super.getFilters();
		filters.add(new MappedFilter<>(getEntityType().name(), "isContainer", "Filters by schema container flag", BooleanFilter.filter(), schema -> getLatestVersion(schema).getContainer(), Pair.pair("latestVersion", new JoinPart("LATESTVERSION", "value"))));
		return filters;
	}

	@Override
	protected ElementType getEntityType() {
		return ELEMENT;
	}

	@Override
	protected Class<? extends SchemaVersionModel> getSchemaModelVersionClass() {
		return SchemaModelImpl.class;
	}

	@Override
	protected SchemaDao getSchemaElementDao() {
		return Tx.get().schemaDao();
	}
}
