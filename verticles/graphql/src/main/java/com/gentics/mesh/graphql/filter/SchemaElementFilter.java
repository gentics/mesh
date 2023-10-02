package com.gentics.mesh.graphql.filter;

import static graphql.schema.GraphQLEnumType.newEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.gentics.graphqlfilter.filter.BooleanFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.operation.Comparison;
import com.gentics.graphqlfilter.filter.operation.FieldOperand;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.dao.RootDao;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.json.JsonUtil;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;

/**
 * Base schema element filter.
 * 
 * @author plyhun
 *
 * @param <R>
 * @param <RM>
 * @param <RE>
 * @param <SC>
 * @param <SCV>
 */
public abstract class SchemaElementFilter<
			R extends FieldSchemaContainer, 
			RM extends FieldSchemaContainerVersion, 
			RE extends NameUuidReference<RE>, 
			SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>, 
			SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>
		> extends EntityFilter<SC> {

	protected final GraphQLContext context;

	protected SchemaElementFilter(GraphQLContext context, String name, String description, ElementType element) {
		super(name, description, Optional.of(element.name()));
		this.context = context;
	}

	private GraphQLEnumType schemaElementEnum() {
		String elementName = getSchemaElementName();
		Tx tx = Tx.get();
		RootDao<HibProject, SC> dao = getSchemaElementDao();
		HibProject project = tx.getProject(context);
		List<GraphQLEnumValueDefinition> values = dao.findAll(project).stream()
			.map(schema -> {
				String name = schema.getName();
				return GraphQLEnumValueDefinition
						.newEnumValueDefinition()
						.name(name)
						.description(name)
						.value(schema.getUuid())
						.build();
			}).collect(Collectors.toList());

		if (values.isEmpty()) {
			return newEnum().name(elementName + "Enum").description("Empty placeholder for " + elementName + ".").value("EMPTY").build();
		}

		return GraphQLEnumType
				.newEnum()
				.name(elementName + "Enum")
				.description("Enumerates all " + elementName + "s")
				.values(values)
				.build();
	}

	@Override
	protected List<FilterField<SC, ?>> getFilters() {
		String owner = getEntityType().name();
		List<FilterField<SC, ?>> filters = new ArrayList<>();
		filters.add(FilterField.create("is", "Filters by " + getSchemaElementName(), schemaElementEnum(), uuid -> schema -> schema != null && schema.getUuid().equals(uuid), 
				Optional.of(query -> Comparison.eq(new FieldOperand<>(getEntityType(), "uuid", query.maybeGetJoins(), Optional.empty()), query.makeValueOperand(true), query.getInitiatingFilterName()))));
		filters.add(CommonFields.hibNameFilter(owner));
		filters.add(CommonFields.hibUuidFilter(owner));
		filters.addAll(CommonFields.hibUserTrackingFilter(owner));
		filters.add(new MappedFilter<>(owner, "noIndex", "Filters by schema 'excluded from index' flag", BooleanFilter.filter(), schema -> getLatestVersion(schema).getNoIndex()));
		return filters;
	}

	protected RM getLatestVersion(SC schema) {
		return JsonUtil.readValue(schema.getLatestVersion().getJson(), getSchemaModelVersionClass());
	}

	protected String getSchemaElementName() {
		return StringUtils.capitalize(getEntityType().name().toLowerCase());
	}

	protected abstract Class<? extends RM> getSchemaModelVersionClass();

	protected abstract RootDao<HibProject, SC> getSchemaElementDao();
}
