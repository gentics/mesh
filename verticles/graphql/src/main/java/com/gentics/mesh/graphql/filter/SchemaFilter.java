package com.gentics.mesh.graphql.filter;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphqlfilter.filter.FilterField;
import com.gentics.mesh.graphqlfilter.filter.MainFilter;
import com.gentics.mesh.graphqlfilter.filter.MappedFilter;
import com.gentics.mesh.graphqlfilter.filter.StringFilter;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
            FilterField.create("is", "Filters by schema", schemaEnum(), uuid -> schema -> schema.getUuid().equals(uuid))
        );
    }
}
