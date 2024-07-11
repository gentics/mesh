package com.gentics.mesh.graphql.filter;

import static graphql.schema.GraphQLEnumType.newEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.model.NodeReferenceIn;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLTypeReference;
import graphql.util.Pair;

/**
 * Filters nodes.
 */
public class NodeFilter extends EntityFilter<NodeContent> implements TypeReferencedFilter<NodeContent, Map<String, ?>> {

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
		createAllFieldFilters().ifPresent(fieldFilters -> filters.add(new MappedFilter<>(OWNER, "fields", "Filters by fields", fieldFilters, Function.identity(), Pair.pair("content", new JoinPart("CONTENT", "fields")))));
		filters.add(new MappedFilter<>(OWNER, "referencedBy", "Filters by all referenced entities", ListFilter.nodeReferenceListFilter(context),
				content -> content == null ? null : NodeReferenceIn.fromContent(context, content, content.getType()).collect(Collectors.toList()) , Pair.pair("references", new JoinPart("REFERENCE", "value"))));
		filters.add(new MappedFilter<>(OWNER, "referencedByNodes", "Filters by referenced nodes (no micronodes)", 
				ListFilter.nodeReferenceListFilter(context, true, true, true, false, String.valueOf(NodeReferenceFilter.createLookupChange(true, true, true, false))),
				content -> content == null ? null : NodeReferenceIn.fromContent(context, content, content.getType(), true, true, true, false).collect(Collectors.toList()) , Pair.pair("nodereferences", new JoinPart("NODESREFERENCE", "value"))));
		filters.add(new MappedFilter<>(OWNER, "referencedByMicronodes", "Filters by referenced micronodes (no nodes)", 
				ListFilter.nodeReferenceListFilter(context, true, true, false, true, String.valueOf(NodeReferenceFilter.createLookupChange(true, true, false, true))),
				content -> content == null ? null : NodeReferenceIn.fromContent(context, content, content.getType(), true, true, false, true).collect(Collectors.toList()) , Pair.pair("micronodereferences", new JoinPart("MICRONODESREFERENCE", "value"))));
		filters.add(new MappedFilter<>(OWNER, "referencedByContent", "Filters by referenced direct content (no lists)", 
				ListFilter.nodeReferenceListFilter(context, true, false, true, true, String.valueOf(NodeReferenceFilter.createLookupChange(true, false, true, true))),
				content -> content == null ? null : NodeReferenceIn.fromContent(context, content, content.getType(), true, false, true, true).collect(Collectors.toList()) , Pair.pair("contentreferences", new JoinPart("CONTENTREFERENCE", "value"))));
		filters.add(new MappedFilter<>(OWNER, "referencedByLists", "Filters by referenced lists (no direct content)", 
				ListFilter.nodeReferenceListFilter(context, false, true, true, true, String.valueOf(NodeReferenceFilter.createLookupChange(false, true, true, true))),
				content -> content == null ? null : NodeReferenceIn.fromContent(context, content, content.getType(), false, true, true, true).collect(Collectors.toList()) , Pair.pair("listreferences", new JoinPart("LISTSREFERENCE", "value"))));
		return filters;
	}

	private Optional<MainFilter<NodeContent>> createAllFieldFilters() {
		Project project = Tx.get().getProject(context);
		SchemaDao schemaDao = Tx.get().schemaDao();
		List<FilterField<NodeContent, ?>> schemaFields = schemaDao.findAll(project)
			.stream()
			.map(this::createFieldFilter)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		return Optional.ofNullable(schemaFields).filter(fields -> !fields.isEmpty()).map(fields -> MainFilter.mainFilter("NodeFieldFilter", "Filters by fields", fields, false, Optional.of("CONTENT")));
	}

	private FilterField<NodeContent, ?> createFieldFilter(Schema schema) {
		String uuid = schema.getLatestVersion().getUuid();
		return Optional.ofNullable(FieldFilter.filter(context, schema.getLatestVersion()))
				.filter(fieldFilter -> !fieldFilter.getFilters().isEmpty())
				.map(fieldFilter -> new MappedFilter<NodeContent, FieldContainer, Map<String, ?>>(OWNER, schema.getName(), "Filters by fields of the " + schema.getName() + " schema",
						fieldFilter, content -> (content == null ? null : ((NodeContent) content).getContainer()), 
								Pair.pair(schema.getUuid(), new JoinPart(schema.getName(), uuid)), Optional.of(uuid)))
				.orElse(null);
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
		CommonTx tx = CommonTx.get();
		if (tx.count(tx.schemaDao().getPersistenceClass()) < 1) {
			return newEnum().name(getName()).description("Empty placeholder for " + getName() + ". Currently no nodes available").value("EMPTY").build();
		} else {
			return super.getType();
		}		
	}

	@Override
	public GraphQLInputType createSortingType() {
		CommonTx tx = CommonTx.get();
		if (tx.count(tx.schemaDao().getPersistenceClass()) < 1) {
			return newEnum().name(getSortingName()).description("Empty placeholder for " + getSortingName() + ". Currently no nodes available").value("EMPTY").build();
		} else {
			return super.getSortingType();
		}
	}
}
