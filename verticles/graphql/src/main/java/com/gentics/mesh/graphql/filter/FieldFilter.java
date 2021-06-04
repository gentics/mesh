package com.gentics.mesh.graphql.filter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.graphqlfilter.filter.BooleanFilter;
import com.gentics.graphqlfilter.filter.DateFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.NumberFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.graphql.context.GraphQLContext;

/**
 * Filters by the fields of a node with a certain schema.
 */
public class FieldFilter extends MainFilter<GraphFieldContainer> {
	private static final String NAME_PREFIX = "FieldFilter.";

	// TODO Remove this after all types are supported
	private static final Set<String> availableTypes = Stream.of(
		FieldTypes.STRING, FieldTypes.HTML, FieldTypes.DATE, FieldTypes.BOOLEAN, FieldTypes.NUMBER).map(FieldTypes::toString)
		.collect(Collectors.toSet());

	/**
	 * Creates a new filter for the provided schema
	 * 
	 * @param context
	 *            The context of the current query
	 * @param container
	 *            The schema model to create the filter for
	 */
	public static FieldFilter filter(GraphQLContext context, SchemaVersionModel container) {
		return context.getOrStore(NAME_PREFIX + container.getName(), () -> new FieldFilter(container));
	}

	private final SchemaVersionModel schema;

	private FieldFilter(SchemaVersionModel container) {
		super(container.getName() + "FieldFilter", "Filters by fields");
		this.schema = container;
	}

	@Override
	protected List<FilterField<GraphFieldContainer, ?>> getFilters() {
		return schema.getFields().stream()
			// filters fields where the Filter is not implemented
			// TODO remove this after all types are supported
			.filter(field -> availableTypes.contains(field.getType()))
			.map(this::createFieldFilter)
			.collect(Collectors.toList());
	}

	/**
	 * Creates a filter for a single field of a schema. Currently not all field types are supported.
	 * 
	 * @param fieldSchema
	 *            The field schema to create the filter for
	 */
	private FilterField<GraphFieldContainer, ?> createFieldFilter(FieldSchema fieldSchema) {
		String schemaName = schema.getName();
		String name = fieldSchema.getName();
		String description = "Filters by the field " + name;
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		switch (type) {
		case STRING:
			return new FieldMappedFilter<>(name, description, StringFilter.filter(),
				node -> getOrNull(node.getString(name), StringGraphField::getString), schemaName);
		case HTML:
			return new FieldMappedFilter<>(name, description, StringFilter.filter(),
				node -> getOrNull(node.getHtml(name), HtmlGraphField::getHTML), schemaName);
		case DATE:
			return new FieldMappedFilter<>(name, description, DateFilter.filter(),
				node -> getOrNull(node.getDate(name), DateGraphField::getDate), schemaName);
		case BOOLEAN:
			return new FieldMappedFilter<>(name, description, BooleanFilter.filter(),
				node -> getOrNull(node.getBoolean(name), BooleanGraphField::getBoolean), schemaName);
		case NUMBER:
			return new FieldMappedFilter<>(name, description, NumberFilter.filter(),
				node -> getOrNull(node.getNumber(name), val -> new BigDecimal(val.getNumber().toString())), schemaName);
		// TODO correctly implement other types
		case BINARY:
		case S3BINARY:
		case LIST:
		case NODE:
		case MICRONODE:
		default:
			throw new RuntimeException("Unexpected type " + type);
		}
	}

	private static <T, R> R getOrNull(T nullableValue, Function<T, R> mapper) {
		if (nullableValue == null) {
			return null;
		} else {
			return mapper.apply(nullableValue);
		}
	}
}
