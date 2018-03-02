package com.gentics.mesh.graphql.filter;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.graphqlfilter.filter.Filter;
import com.gentics.mesh.graphqlfilter.filter.MappedFilter;

import java.util.function.Function;
import java.util.function.Predicate;

public class FieldMappedFilter<T, Q> extends MappedFilter<GraphFieldContainer, T, Q> {
    private final String schemaName;

    public FieldMappedFilter(String name, String description, Filter<T, Q> delegate, Function<GraphFieldContainer, T> mapper, String schemaName) {
        super(name, description, delegate, mapper);
        this.schemaName = schemaName;
    }

    @Override
    public Predicate<GraphFieldContainer> createPredicate(Q query) {
        // Return always true if the node is not of the provided schema.
        Predicate<GraphFieldContainer> schemaCheck = node -> !node.getSchemaContainerVersion().getName().equals(schemaName);
        Predicate<GraphFieldContainer> predicate = super.createPredicate(query);
        return schemaCheck.or(predicate);
    }
}
