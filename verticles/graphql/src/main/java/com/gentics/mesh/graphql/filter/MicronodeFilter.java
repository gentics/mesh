package com.gentics.mesh.graphql.filter;

import static graphql.schema.GraphQLEnumType.newEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.operation.JoinPart;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLTypeReference;
import graphql.util.Pair;

/**
 * Filter micronodes
 * 
 * @author plyhun
 *
 */
public class MicronodeFilter extends MainFilter<Micronode> implements TypeReferencedFilter<Micronode, Map<String, ?>> {

	private static final String NAME = "MicronodeFilter";
	private final static String OWNER = "MICRONODE";

	/**
	 * Create a micronode filter for the given context.
	 * 
	 * @param context
	 * @return
	 */
	public static MicronodeFilter filter(GraphQLContext context) {
		return context.getOrStore(NAME, () -> new MicronodeFilter(context));
	}

	private final GraphQLContext context;

	private MicronodeFilter(GraphQLContext context) {
		super(NAME, "Filters Micronodes", Optional.of(OWNER));
		this.context = context;
	}

	@Override
	protected List<FilterField<Micronode, ?>> getFilters() {
		List<FilterField<Micronode, ?>> filters = new ArrayList<>();
		filters.add(FilterField.isNull());
		filters.add(new MappedFilter<>(OWNER, "microschema", "Filters by microschema", MicroschemaFilter.filter(context), 
			content -> content == null ? null : content.getSchemaContainerVersion().getSchemaContainer(), Pair.pair("microschema", new JoinPart(ElementType.MICROSCHEMA.name(), "uuid"))));
		filters.add(new MappedFilter<>(OWNER, "fields", "Filters by fields", createAllFieldFilters(), Function.identity(), Pair.pair("microcontent", new JoinPart("MICROCONTENT", "fields"))));
		return filters;
	}

	private MainFilter<Micronode> createAllFieldFilters() {
		Project project = Tx.get().getProject(context);
		MicroschemaDao schemaDao = Tx.get().microschemaDao();
		List<FilterField<Micronode, ?>> schemaFields = schemaDao.findAll(project)
			.stream()
			.map(this::createFieldFilter)
			.collect(Collectors.toList());
		return MainFilter.mainFilter("MicronodeFieldFilter", "Filters by fields", schemaFields, true, Optional.of("MICROCONTENT"));
	}

	private FilterField<Micronode, ?> createFieldFilter(Microschema schema) {
		String uuid = schema.getLatestVersion().getUuid();
		return new MappedFilter<>(OWNER, schema.getName(), "Filters by fields of the " + schema.getName() + " microschema",
			FieldFilter.filter(context, schema.getLatestVersion()),
			content -> content, Pair.pair(schema.getUuid(), new JoinPart(schema.getName(), uuid)), Optional.of(uuid));
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
		if (tx.count(tx.microschemaDao().getPersistenceClass()) < 1) {
			return newEnum().name(getName()).description("Empty placeholder for " + getName() + ". Currently no micronodes available").value("EMPTY").build();
		} else {
			return super.getType();
		}		
	}

	@Override
	public GraphQLInputType createSortingType() {
		CommonTx tx = CommonTx.get();
		if (tx.count(tx.microschemaDao().getPersistenceClass()) < 1) {
			return newEnum().name(getSortingName()).description("Empty placeholder for " + getSortingName() + ". Currently no micronodes available").value("EMPTY").build();
		} else {
			return super.getSortingType();
		}
	}
}
