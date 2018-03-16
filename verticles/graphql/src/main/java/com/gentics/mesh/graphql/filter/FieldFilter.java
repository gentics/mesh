package com.gentics.mesh.graphql.filter;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphqlfilter.filter.BooleanFilter;
import com.gentics.mesh.graphqlfilter.filter.DateFilter;
import com.gentics.mesh.graphqlfilter.filter.FilterField;
import com.gentics.mesh.graphqlfilter.filter.MainFilter;
import com.gentics.mesh.graphqlfilter.filter.StringFilter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FieldFilter extends MainFilter<GraphFieldContainer> {
    private static final String NAME_PREFIX = "FieldFilter.";

    // TODO Remove this after all types are supported
    private static final Set<String> availableTypes = Stream.of(
        FieldTypes.STRING, FieldTypes.HTML, FieldTypes.DATE, FieldTypes.BOOLEAN
    ).map(FieldTypes::toString).collect(Collectors.toSet());

    public static FieldFilter filter(GraphQLContext context, SchemaModel container) {
        return context.getOrStore(NAME_PREFIX + container.getName(), () -> new FieldFilter(container));
    }

    private final SchemaModel schema;

    private FieldFilter(SchemaModel container) {
        super(container.getName() + "FieldFilter", "Filters by fields");
        this.schema = container;
    }

    @Override
    protected List<FilterField<GraphFieldContainer, ?>> getFilters() {
        return schema.getFields().stream()
            // filters fields where the Filter is not implemented
            // TODO remove this after all types are supported
            .filter(field -> availableTypes.contains(field.getType()))
            .map(this::createFieldFilter)
            .collect(Collectors.toList());
    }

    private FilterField<GraphFieldContainer, ?> createFieldFilter(FieldSchema fieldSchema) {
        String schemaName = schema.getName();
        String name = fieldSchema.getName();
        String description = "Filters by the field " + name;
        FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
        switch (type) {
            case STRING:
                return new FieldMappedFilter<>(name, description, StringFilter.filter(), node -> node.getString(name).getString(), schemaName);
            case HTML:
                return new FieldMappedFilter<>(name, description, StringFilter.filter(), node -> node.getHtml(name).getHTML(), schemaName);
            case DATE:
                return new FieldMappedFilter<>(name, description, DateFilter.filter(), node -> node.getDate(name).getDate(), schemaName);
            case BOOLEAN:
                return new FieldMappedFilter<>(name, description, BooleanFilter.filter(), node -> node.getBoolean(name).getBoolean(), schemaName);
            // TODO correctly implement other types
            case NUMBER:
            case BINARY:
            case LIST:
            case NODE:
            case MICRONODE:
                return new FieldMappedFilter<>(name, description, StringFilter.filter(), node -> "bogus", schemaName);
            default:
                throw new RuntimeException("Unexpected type " + type);
        }
    }
}
