package com.gentics.mesh.graphql.filter;

import com.gentics.graphqlfilter.filter.BooleanFilter;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.graphqlfilter.filter.DateFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.json.JsonUtil;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Filter schemas.
 */
public class SchemaFilter extends MainFilter<SchemaContainer> {

	private static final String NAME = "SchemaFilter";

	public static SchemaFilter filter(GraphQLContext context) {
		return context.getOrStore(NAME, () -> new SchemaFilter(context));
	}

	private final GraphQLContext context;

	private SchemaFilter(GraphQLContext context) {
		super(NAME, "Filters schemas");
		this.context = context;
	}

	private GraphQLEnumType schemaEnum() {
		Project project = context.getProject();
		List<GraphQLEnumValueDefinition> values = StreamSupport.stream(project.getSchemaContainerRoot().findAllIt().spliterator(), false)
			.map(schema -> {
				String name = schema.getName();
				return new GraphQLEnumValueDefinition(name, name, schema.getUuid());
			}).collect(Collectors.toList());

		return new GraphQLEnumType("SchemaEnum", "Enumerates all schemas", values);
	}

	@Override
	protected List<FilterField<SchemaContainer, ?>> getFilters() {
		return Arrays.asList(
			new MappedFilter<>("uuid", "Filters by uuid", StringFilter.filter(), SchemaContainer::getUuid),
			new MappedFilter<>("name", "Filters by name", StringFilter.filter(), SchemaContainer::getName),
			FilterField.create("is", "Filters by schema", schemaEnum(), uuid -> schema -> schema.getUuid().equals(uuid)),
			new MappedFilter<>("created", "Filters by schema creation timestamp", DateFilter.filter(), SchemaContainer::getCreationTimestamp),
			new MappedFilter<>("isContainer", "Filters by schema container flag", BooleanFilter.filter(), schema -> getLatestVersion(schema).isContainer()),
			new MappedFilter<>("edited", "Filters by schema update timestamp", DateFilter.filter(), SchemaContainer::getLastEditedTimestamp));
	}

	private SchemaModel getLatestVersion(SchemaContainer schema) {
		return JsonUtil.readValue(schema.getLatestVersion().getJson(), SchemaModelImpl.class);
	}
}
