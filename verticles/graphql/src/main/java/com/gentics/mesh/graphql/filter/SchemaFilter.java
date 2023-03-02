package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gentics.graphqlfilter.filter.BooleanFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StartMainFilter;
import com.gentics.graphqlfilter.filter.operation.Comparison;
import com.gentics.graphqlfilter.filter.operation.FieldOperand;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.json.JsonUtil;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;

/**
 * Filter schemas.
 */
public class SchemaFilter extends EntityFilter<HibSchema> {

	private static final ElementType ELEMENT = ElementType.SCHEMA;
	private static final String NAME = "SchemaFilter";

	public static SchemaFilter filter(GraphQLContext context) {
		return context.getOrStore(NAME, () -> new SchemaFilter(context));
	}

	private final GraphQLContext context;

	private SchemaFilter(GraphQLContext context) {
		super(NAME, "Filters schemas", Optional.of(ELEMENT.name()));
		this.context = context;
	}

	private GraphQLEnumType schemaEnum() {
		Tx tx = Tx.get();
		SchemaDao schemaDao = tx.schemaDao();
		HibProject project = tx.getProject(context);
		List<GraphQLEnumValueDefinition> values = schemaDao.findAll(project).stream()
			.map(schema -> {
				String name = schema.getName();
				return GraphQLEnumValueDefinition
						.newEnumValueDefinition()
						.name(name)
						.description(name)
						.value(schema.getUuid())
						.build();
			}).collect(Collectors.toList());

		return GraphQLEnumType
				.newEnum()
				.name("SChemaEnum")
				.description("Enumerates all schemas")
				.values(values)
				.build();
	}

	@Override
	protected List<FilterField<HibSchema, ?>> getFilters() {
		String owner = ELEMENT.name();
		List<FilterField<HibSchema, ?>> filters = new ArrayList<>();
		filters.add(FilterField.create("is", "Filters by schema", schemaEnum(), uuid -> schema -> schema.getUuid().equals(uuid), 
				Optional.of(query -> Comparison.eq(new FieldOperand<>(ELEMENT, "uuid", query.getMaybeJoins(), Optional.empty()), query.makeValueOperand(true)))));
		filters.add(new MappedFilter<>(owner, "isContainer", "Filters by schema container flag", BooleanFilter.filter(), schema -> getLatestVersion(schema).getContainer()));
		filters.add(CommonFields.hibNameFilter(owner));
		filters.add(CommonFields.hibUuidFilter(owner));
		filters.addAll(CommonFields.hibUserTrackingFilter(owner));
		return filters;
	}

	private SchemaVersionModel getLatestVersion(HibSchema schema) {
		return JsonUtil.readValue(schema.getLatestVersion().getJson(), SchemaModelImpl.class);
	}

	@Override
	protected ElementType getEntityType() {
		return ELEMENT;
	}
}
