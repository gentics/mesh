package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.graphql.context.GraphQLContext;

/**
 * Filters nodes.
 */
public class NodeFilter extends StartMainFilter<NodeContent> {

	private static final String NAME = "NodeFilter";

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
			content -> content.getContainer().getLastEditedTimestamp()));
		filters.add(new MappedFilter<>("editor", "Filters by editor", UserFilter.filter(),
			content -> content.getContainer().getEditor()));
		createAllFieldFilters().ifPresent(fieldFilters -> filters.add(new MappedFilter<>("fields", "Filters by fields", fieldFilters, Function.identity())));

		return filters;
	}

	private Optional<MainFilter<NodeContent>> createAllFieldFilters() {
		HibProject project = Tx.get().getProject(context);
		SchemaDao schemaDao = Tx.get().schemaDao();
		List<FilterField<NodeContent, ?>> schemaFields = StreamSupport
			.stream(schemaDao.findAll(project).spliterator(), false)
			.map(this::createFieldFilter)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		return Optional.ofNullable(schemaFields).filter(fields -> !fields.isEmpty()).map(fields -> MainFilter.mainFilter("FieldFilter", "Filters by fields", fields, false));
	}

	private FilterField<NodeContent, ?> createFieldFilter(HibSchema schema) {
		return Optional.ofNullable(FieldFilter.filter(context, schema.getLatestVersion().getSchema()))
				.filter(fieldFilter -> !fieldFilter.getFilters().isEmpty())
				.map(fieldFilter -> new MappedFilter<>(schema.getName(), "Filters by fields of the " + schema.getName() + " schema",
							fieldFilter,
							NodeContent::getContainer))
				.orElse(null);
	}
}
