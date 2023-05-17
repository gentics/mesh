package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.operation.JoinPart;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
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
public class MicronodeFilter extends MainFilter<HibMicronode> {

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
	protected List<FilterField<HibMicronode, ?>> getFilters() {
		List<FilterField<HibMicronode, ?>> filters = new ArrayList<>();
		filters.add(FilterField.isNull());
		filters.add(new MappedFilter<>(OWNER, "microschema", "Filters by microschema", MicroschemaFilter.filter(context), 
			content -> content == null ? null : content.getSchemaContainerVersion().getSchemaContainer(), Pair.pair("microschema", new JoinPart(ElementType.MICROSCHEMA.name(), "uuid"))));
		filters.add(new MappedFilter<>(OWNER, "fields", "Filters by fields", createAllFieldFilters(), Function.identity(), Pair.pair("microcontent", new JoinPart("MICROCONTENT", "fields"))));
		return filters;
	}

	private MainFilter<HibMicronode> createAllFieldFilters() {
		HibProject project = Tx.get().getProject(context);
		MicroschemaDao schemaDao = Tx.get().microschemaDao();
		List<FilterField<HibMicronode, ?>> schemaFields = schemaDao.findAll(project)
			.stream()
			.map(this::createFieldFilter)
			.collect(Collectors.toList());
		return MainFilter.mainFilter("MicronodeFieldFilter", "Filters by fields", schemaFields, true, Optional.of("MICROCONTENT"));
	}

	private FilterField<HibMicronode, ?> createFieldFilter(HibMicroschema schema) {
		return new MappedFilter<>(OWNER, schema.getName(), "Filters by fields of the " + schema.getName() + " microschema",
			FieldFilter.filter(context, schema.getLatestVersion().getSchema()),
			content -> content);
	}

	@Override
	public GraphQLInputType getType() {
		return GraphQLTypeReference.typeRef(getName());
	}

	@Override
	public GraphQLInputType getSortingType() {
		return GraphQLTypeReference.typeRef(getSortingName());
	}

	public final GraphQLInputType createType() {
		return super.getType();
	}

	public final GraphQLInputType createSortingType() {
		return super.getSortingType();
	}
}
