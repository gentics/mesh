package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gentics.graphqlfilter.filter.DateFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.graphqlfilter.filter.operation.JoinPart;
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
public class NodeFilter extends EntityFilter<NodeContent> {

	private static final ElementType ELEMENT = ElementType.NODE;
	private static final String NAME = "NodeFilter";
	private static final String OWNER = ELEMENT.name();

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
		filters.add(new MappedFilter<>(OWNER, "schema", "Filters by schema", SchemaFilter.filter(context), 
			content -> content.getNode().getSchemaContainer(), Pair.pair("schema", new JoinPart(ElementType.SCHEMA.name(), "uuid"))));
		filters.add(new MappedFilter<>(OWNER, "created", "Filters by node creation timestamp", DateFilter.filter(),
			content -> content.getNode().getCreationTimestamp()));
		filters.add(new MappedFilter<>(OWNER, "creator", "Filters by creator", UserFilter.filter(),
			content -> content.getNode().getCreator(), Pair.pair("creator", new JoinPart(ElementType.USER.name(), "uuid"))));
		filters.add(new MappedFilter<>(OWNER, "edited", "Filters by node update timestamp", DateFilter.filter(),
			content -> content.getContainer().getLastEditedTimestamp(), Pair.pair("edited", new JoinPart("CONTENT", "edited"))));
		filters.add(new MappedFilter<>(OWNER, "editor", "Filters by editor", UserFilter.filter(),
			content -> content.getContainer().getEditor(), Pair.pair("content", new JoinPart("CONTENT", "editor"))));
		filters.add(new MappedFilter<>(OWNER, "fields", "Filters by fields", createAllFieldFilters(), Function.identity(), Pair.pair("content", new JoinPart("CONTENT", "fields"))));

		return filters;
	}

	private MainFilter<NodeContent> createAllFieldFilters() {
		HibProject project = Tx.get().getProject(context);
		SchemaDao schemaDao = Tx.get().schemaDao();
		List<FilterField<NodeContent, ?>> schemaFields = schemaDao.findAll(project)
			.stream()
			.map(this::createFieldFilter)
			.collect(Collectors.toList());
		return MainFilter.mainFilter("FieldFilter", "Filters by fields", schemaFields, false, Optional.of("CONTENT"));
	}

	private FilterField<NodeContent, ?> createFieldFilter(HibSchema schema) {
		return new MappedFilter<>(OWNER, schema.getName(), "Filters by fields of the " + schema.getName() + " schema",
			FieldFilter.filter(context, schema.getLatestVersion().getSchema()),
			NodeContent::getContainer);
	}

	@Override
	protected ElementType getEntityType() {
		return ELEMENT;
	}
}
