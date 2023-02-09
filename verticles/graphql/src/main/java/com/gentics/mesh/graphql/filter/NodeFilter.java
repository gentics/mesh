package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.gentics.graphqlfilter.filter.DateFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StartMainFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.graphqlfilter.filter.operation.FilterQuery;
import com.gentics.graphqlfilter.filter.operation.UnformalizableQuery;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.util.Pair;

/**
 * Filters nodes.
 */
public class NodeFilter extends StartMainFilter<NodeContent> {

	private static final String NAME = "NodeFilter";
	private static final String OWNER = ElementType.NODE.name();

	/**
	 * Create a node filter for the given context.
	 * 
	 * @param context
	 * @return
	 */
	public static NodeFilter filter(GraphQLContext context) {
		return context.getOrStore(NAME, () -> new NodeFilter(context));
	}

	private final GraphQLContext context;

	private NodeFilter(GraphQLContext context) {
		super(NAME, "Filters Nodes", Optional.of(OWNER));
		this.context = context;
	}

	@Override
	protected List<FilterField<NodeContent, ?>> getFilters() {
		List<FilterField<NodeContent, ?>> filters = new ArrayList<>();
		filters.add(new MappedFilter<>(OWNER, "uuid", "Filters by uuid", StringFilter.filter(), content -> content.getNode().getUuid()));
		filters
			.add(new MappedFilter<>(OWNER, "schema", "Filters by schema", SchemaFilter.filter(context), content -> content.getNode().getSchemaContainer(), Pair.pair(OWNER, "schema")));
		filters.add(new MappedFilter<>(OWNER, "created", "Filters by node creation timestamp", DateFilter.filter(),
			content -> content.getNode().getCreationTimestamp()));
		filters.add(new MappedFilter<>(OWNER, "creator", "Filters by creator", UserFilter.filter(),
			content -> content.getNode().getCreator()));
		filters.add(new MappedFilter<>(OWNER, "edited", "Filters by node update timestamp", DateFilter.filter(),
			content -> content.getContainer().getLastEditedTimestamp()));
		filters.add(new MappedFilter<>(OWNER, "editor", "Filters by editor", UserFilter.filter(),
			content -> content.getContainer().getEditor()));
		filters.add(new MappedFilter<>(OWNER, "fields", "Filters by fields", createAllFieldFilters(), Function.identity()));

		return filters;
	}

	private MainFilter<NodeContent> createAllFieldFilters() {
		HibProject project = Tx.get().getProject(context);
		SchemaDao schemaDao = Tx.get().schemaDao();
		List<FilterField<NodeContent, ?>> schemaFields = StreamSupport
			.stream(schemaDao.findAll(project).spliterator(), false)
			.map(this::createFieldFilter)
			.collect(Collectors.toList());
		return MainFilter.mainFilter("FieldFilter", "Filters by fields", schemaFields, false, Optional.of(ElementType.NODE.name()));
	}

	private FilterField<NodeContent, ?> createFieldFilter(HibSchema schema) {
		return new MappedFilter<>(OWNER, schema.getName(), "Filters by fields of the " + schema.getName() + " schema",
			FieldFilter.filter(context, schema.getLatestVersion().getSchema()),
			NodeContent::getContainer);
	}

	public FilterOperation<?> createFilterOperation(Map<String, ?> filterArgument) throws UnformalizableQuery  {
		return createFilterOperation(new FilterQuery<>(ElementType.NODE, "", filterArgument));
	}
}
