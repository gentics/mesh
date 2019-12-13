package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.gentics.graphqlfilter.filter.DateFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StartMainFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.core.data.EditorTrackingVertex;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.graphql.context.GraphQLContext;

/**
 * Filters nodes.
 */
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
		List<FilterField<NodeContent, ?>> filters = new ArrayList<>();
		filters.add(new MappedFilter<>("uuid", "Filters by uuid", StringFilter.filter(), content -> content.getNode().getUuid()));
		filters
			.add(new MappedFilter<>("schema", "Filters by schema", SchemaFilter.filter(context), content -> content.getNode().getSchemaContainer()));
		filters.add(new MappedFilter<>("created", "Filters by node creation timestamp", DateFilter.filter(),
			content -> content.getNode().getCreationTimestamp()));
		filters.add(new MappedFilter<>("creator", "Filters by creator", UserFilter.filter(),
			content -> content.getNode().getCreator()));
		filters.add(new MappedFilter<>("edited", "Filters by node update timestamp", DateFilter.filter(),
			content -> content.getContainer().map(EditorTrackingVertex::getLastEditedTimestamp).orElse(null)));
		filters.add(new MappedFilter<>("editor", "Filters by editor", UserFilter.filter(),
			content -> content.getContainer().map(EditorTrackingVertex::getEditor).orElse(null)));
		filters.add(new MappedFilter<>("fields", "Filters by fields", createAllFieldFilters(), Function.identity()));

		return filters;
	}

	private MainFilter<NodeContent> createAllFieldFilters() {
		List<FilterField<NodeContent, ?>> schemaFields = StreamSupport
			.stream(context.getProject().getSchemaContainerRoot().findAll().spliterator(), false)
			.map(this::createFieldFilter)
			.collect(Collectors.toList());
		return MainFilter.mainFilter("FieldFilter", "Filters by fields", schemaFields, false);
	}

	private FilterField<NodeContent, ?> createFieldFilter(SchemaContainer schema) {
		return new MappedFilter<>(schema.getName(), "Filters by fields of the " + schema.getName() + " schema",
			FieldFilter.filter(context, schema.getLatestVersion().getSchema()),
			content -> content.getContainer().orElse(null));
	}
}
