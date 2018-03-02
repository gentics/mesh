package com.gentics.mesh.graphql.filter;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphqlfilter.filter.FilterField;
import com.gentics.mesh.graphqlfilter.filter.MainFilter;
import com.gentics.mesh.graphqlfilter.filter.MappedFilter;
import com.gentics.mesh.graphqlfilter.filter.StringFilter;

import java.util.List;
import java.util.stream.Collectors;

public class FieldFilter extends MainFilter<GraphFieldContainer> {
    private static final String NAME_PREFIX = "FieldFilter.";

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
            case NUMBER:
            case BOOLEAN:
            case BINARY:
            case LIST:
            case NODE:
            case MICRONODE:
                // TODO correctly implement other types
                return new FieldMappedFilter<>(name, description, StringFilter.filter(), node -> "bogus", schemaName);
            default:
                throw new RuntimeException("Unexpected type " + type);
        }
    }
}
