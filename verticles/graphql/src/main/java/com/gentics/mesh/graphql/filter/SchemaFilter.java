package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gentics.graphqlfilter.filter.BooleanFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.MappedFilter;
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
public class SchemaFilter extends MainFilter<HibSchema> {

	private static final String NAME = "SchemaFilter";

	public static SchemaFilter filter(GraphQLContext context) {
		return context.getOrStore(NAME, () -> new SchemaFilter(context));
	}

	private final GraphQLContext context;

	private SchemaFilter(GraphQLContext context) {
		super(NAME, "Filters schemas");
		this.context = context;
	}

	private Optional<GraphQLEnumType> schemaEnum() {
		Tx tx = Tx.get();
		SchemaDao schemaDao = tx.schemaDao();
		HibProject project = tx.getProject(context);
		List<GraphQLEnumValueDefinition> values = schemaDao.findAll(project)
			.stream()
			.map(schema -> {
				String name = schema.getName();
				return GraphQLEnumValueDefinition
						.newEnumValueDefinition()
						.name(name)
						.description(name)
						.value(schema.getUuid())
						.build();
			}).collect(Collectors.toList());

		return Optional.ofNullable(values).filter(v -> !v.isEmpty()).map(v -> GraphQLEnumType
				.newEnum()
				.name("SchemaEnum")
				.description("Enumerates all schemas")
				.values(v)
				.build());
	}

	@Override
	protected List<FilterField<HibSchema, ?>> getFilters() {
		List<FilterField<HibSchema, ?>> filters = new ArrayList<>();
		schemaEnum().ifPresent(schemaEnum -> filters.add(FilterField.create("is", "Filters by schema", schemaEnum, uuid -> schema -> schema.getUuid().equals(uuid))));
		filters.add(new MappedFilter<>("isContainer", "Filters by schema container flag", BooleanFilter.filter(), schema -> getLatestVersion(schema).getContainer()));
		filters.add(CommonFields.hibNameFilter());
		filters.add(CommonFields.hibUuidFilter());
		filters.addAll(CommonFields.hibUserTrackingFilter());
		return filters;
	}

	private SchemaVersionModel getLatestVersion(HibSchema schema) {
		return JsonUtil.readValue(schema.getLatestVersion().getJson(), SchemaModelImpl.class);
	}
}
