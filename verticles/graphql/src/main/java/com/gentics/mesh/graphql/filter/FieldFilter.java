package com.gentics.mesh.graphql.filter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.graphql.context.GraphQLContext;

/**
 * Filters by the fields of a (micro)node with a certain schema.
 */
public class FieldFilter extends MainFilter<HibFieldContainer> {
	private static final String NAME_PREFIX = "FieldFilter.";

	// TODO Remove this after all types are supported
	private static final Set<String> availableTypes = Stream.of(
		FieldTypes.STRING, FieldTypes.HTML, FieldTypes.DATE, FieldTypes.BOOLEAN, FieldTypes.NUMBER, FieldTypes.MICRONODE).map(FieldTypes::toString)
		.collect(Collectors.toSet());

	/**
	 * Creates a new filter for the provided schema
	 * 
	 * @param context
	 *            The context of the current query
	 * @param container
	 *            The schema model to create the filter for
	 */
	public static FieldFilter filter(GraphQLContext context, FieldSchemaContainerVersion container) {
		return context.getOrStore(NAME_PREFIX + container.getName(), () -> new FieldFilter(container, context));
	}

	private final FieldSchemaContainerVersion schema;
	private final GraphQLContext context;

	private FieldFilter(FieldSchemaContainerVersion container, GraphQLContext context) {
		super(container.getName() + "FieldFilter", "Filters by fields", Optional.empty());
		this.schema = container;
		this.context = context;
	}

	@Override
	protected List<FilterField<HibFieldContainer, ?>> getFilters() {
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
	private FilterField<HibFieldContainer, ?> createFieldFilter(FieldSchema fieldSchema) {
		String schemaName = schema.getName();
		String name = fieldSchema.getName();
		String description = "Filters by the field " + name;
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		switch (type) {
		case STRING:
			return new FieldMappedFilter<>(type, name, description, StringFilter.filter(),
				node -> node == null ? null : getOrNull(node.getString(name), HibStringField::getString), schemaName);
		case HTML:
			return new FieldMappedFilter<>(type, name, description, StringFilter.filter(),
				node -> node == null ? null : getOrNull(node.getHtml(name), HibHtmlField::getHTML), schemaName);
		case DATE:
			return new FieldMappedFilter<>(type, name, description, DateFilter.filter(),
				node -> node == null ? null : getOrNull(node.getDate(name), HibDateField::getDate), schemaName);
		case BOOLEAN:
			return new FieldMappedFilter<>(type, name, description, BooleanFilter.filter(),
				node -> node == null ? null : getOrNull(node.getBoolean(name), HibBooleanField::getBoolean), schemaName);
		case NUMBER:
			return new FieldMappedFilter<>(type, name, description, NumberFilter.filter(),
				node -> node == null ? null : getOrNull(node.getNumber(name), val -> new BigDecimal(val.getNumber().toString())), schemaName);
		case MICRONODE:
			return new FieldMappedFilter<>(type, name, description, MicronodeFilter.filter(context), 
				node -> node == null ? null : getOrNull(node.getMicronode(name), HibMicronodeField::getMicronode), schemaName);
		// TODO correctly implement other types
		case BINARY:
		case S3BINARY:
		case LIST:
		case NODE:
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
