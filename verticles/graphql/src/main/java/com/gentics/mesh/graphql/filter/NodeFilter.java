package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.gentics.mesh.graphql.model.NodeReferenceIn;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLTypeReference;
import graphql.util.Pair;

/**
 * Filters nodes.
 */
public class NodeFilter extends EntityFilter<NodeContent> implements ReferencedFilter<NodeContent, Map<String, ?>> {

	private static final ElementType ELEMENT = ElementType.NODE;
	private static final String OWNER = ELEMENT.name();
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
		super(NAME, "Filters Nodes", Optional.of(OWNER));
		this.context = context;
	}

	@Override
	protected List<FilterField<NodeContent, ?>> getFilters() {
		List<FilterField<NodeContent, ?>> filters = new ArrayList<>();
		filters.add(new MappedFilter<>(OWNER, "uuid", "Filters by uuid", StringFilter.filter(), content -> content.getNode().getUuid()));
		filters.add(new MappedFilter<>(OWNER, "schema", "Filters by schema", SchemaFilter.filter(context), 
			content -> content == null ? null : content.getNode().getSchemaContainer(), Pair.pair("schema", new JoinPart(ElementType.SCHEMA.name(), "uuid"))));
		filters.add(new MappedFilter<>(OWNER, "created", "Filters by node creation timestamp", DateFilter.filter(),
			content -> content == null ? null : content.getNode().getCreationTimestamp()));
		filters.add(new MappedFilter<>(OWNER, "creator", "Filters by creator", UserFilter.filter(),
			content -> content == null ? null : content.getNode().getCreator(), Pair.pair("creator", new JoinPart(ElementType.USER.name(), "uuid"))));
		filters.add(new MappedFilter<>(OWNER, "edited", "Filters by node update timestamp", DateFilter.filter(),
			content -> content == null ? null : content.getContainer().getLastEditedTimestamp(), Pair.pair("edited", new JoinPart("CONTENT", "edited"))));
		filters.add(new MappedFilter<>(OWNER, "editor", "Filters by editor", UserFilter.filter(),
			content -> content == null ? null : content.getContainer().getEditor(), Pair.pair("editor", new JoinPart("CONTENT", "uuid"))));
		filters.add(new MappedFilter<>(OWNER, "fields", "Filters by fields", createAllFieldFilters(), Function.identity(), Pair.pair("content", new JoinPart("CONTENT", "fields"))));
		// TODO referencedBy is not yet ready for native filtering
		filters.add(new MappedFilter<>(OWNER, "referencedBy", "Filters by referenced entities", ListFilter.nodeReferenceListFilter(context),
				content -> content == null ? null : NodeReferenceIn.fromContent(context, content, content.getType()).collect(Collectors.toList()) /*, Pair.pair("references", new JoinPart("REFERENCE", "value"))*/));
		return filters;
	}

	private MainFilter<NodeContent> createAllFieldFilters() {
		HibProject project = Tx.get().getProject(context);
		SchemaDao schemaDao = Tx.get().schemaDao();
		List<FilterField<NodeContent, ?>> schemaFields = schemaDao.findAll(project)
			.stream()
			.map(this::createFieldFilter)
			.collect(Collectors.toList());
		return MainFilter.mainFilter("NodeFieldFilter", "Filters by fields", schemaFields, false, Optional.of("CONTENT"));
	}

	private FilterField<NodeContent, ?> createFieldFilter(HibSchema schema) {
		return new MappedFilter<>(OWNER, schema.getName(), "Filters by fields of the " + schema.getName() + " schema",
			FieldFilter.filter(context, schema.getLatestVersion().getSchema()),
			content -> content == null ? null : content.getContainer());
	}

	@Override
	protected ElementType getEntityType() {
		return ELEMENT;
	}

	@Override
	public GraphQLInputType getType() {
		return GraphQLTypeReference.typeRef(getName());
	}

	@Override
	public GraphQLInputType getSortingType() {
		return GraphQLTypeReference.typeRef(getSortingName());
	}

	@Override
	public GraphQLInputType createType() {
		return super.getType();
	}

	@Override
	public GraphQLInputType createSortingType() {
		return super.getSortingType();
	}
}
