package com.gentics.mesh.graphql.filter;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.graphqlfilter.filter.FilterField;
import com.gentics.mesh.graphqlfilter.filter.MappedFilter;
import com.gentics.mesh.graphqlfilter.filter.StartMainFilter;
import com.gentics.mesh.graphqlfilter.filter.StringFilter;

import java.util.Arrays;
import java.util.List;

public class NodeFilter extends StartMainFilter<NodeContent> {
    private static NodeFilter instance;
    private static Project project;

    public static NodeFilter filter(Project project) {
        NodeFilter.project = project;
        if (instance == null) {
            instance = new NodeFilter();
        }
        return instance;
    }

    public NodeFilter() {
        super("NodeFilter", "Filters Nodes");
    }

    @Override
    protected List<FilterField<NodeContent, ?>> getFilters() {
        return Arrays.asList(
            new MappedFilter<>("uuid", "Filters by uuid", StringFilter.filter(), content -> content.getNode().getUuid()),
            new MappedFilter<>("schema", "Filters by schema", SchemaFilter.filter(project), content -> content.getNode().getSchemaContainer())
        );
    }
}
