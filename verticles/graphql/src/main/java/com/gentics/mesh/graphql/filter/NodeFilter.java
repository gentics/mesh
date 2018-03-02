package com.gentics.mesh.graphql.filter;

import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphqlfilter.filter.FilterField;
import com.gentics.mesh.graphqlfilter.filter.MappedFilter;
import com.gentics.mesh.graphqlfilter.filter.StartMainFilter;
import com.gentics.mesh.graphqlfilter.filter.StringFilter;

import java.util.Arrays;
import java.util.List;

public class NodeFilter extends StartMainFilter<NodeContent> {

    private static final String NAME = "NodeFilter";

    public static NodeFilter filter(GraphQLContext context) {
        return context.getOrStore(NAME, () -> new NodeFilter(context));
    }

    private final GraphQLContext context;

    private NodeFilter(GraphQLContext context) {
        super(NAME, "Filters Nodes");
        this.context = context;
    }

    @Override
    protected List<FilterField<NodeContent, ?>> getFilters() {
        return Arrays.asList(
            new MappedFilter<>("uuid", "Filters by uuid", StringFilter.filter(), content -> content.getNode().getUuid()),
            new MappedFilter<>("schema", "Filters by schema", SchemaFilter.filter(context), content -> content.getNode().getSchemaContainer())
        );
    }
}
