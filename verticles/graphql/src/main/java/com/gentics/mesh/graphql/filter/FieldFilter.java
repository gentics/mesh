package com.gentics.mesh.graphql.filter;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.graphqlfilter.filter.BooleanFilter;
import com.gentics.graphqlfilter.filter.DateFilter;
import com.gentics.graphqlfilter.filter.Filter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.NumberFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.list.HibListField;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.graphql.context.GraphQLContext;

/**
 * Filters by the fields of a (micro)node with a certain schema.
 */
public class FieldFilter extends MainFilter<HibFieldContainer> {
	private static final String NAME_PREFIX = "FieldFilter.";

	/**
	 * Creates a new filter for the provided schema
	 * 
	 * @param context
	 *            The context of the current query
	 * @param container
	 *            The schema model to create the filter for
	 */
	public static FieldFilter filter(GraphQLContext context, HibFieldSchemaVersionElement<?,?,?,?,?> container) {
		return context.getOrStore(NAME_PREFIX + container.getClass().getSimpleName() + "." + container.getName(), () -> new FieldFilter(container, context));
	}

	private final FieldSchemaContainerVersion schema;
	private final String schemaUuid;
	private final GraphQLContext context;

	private FieldFilter(HibFieldSchemaVersionElement<?,?,?,?,?> schemaVersion, GraphQLContext context) {
		super(schemaVersion.getName() + "FieldFilter", "Filters by fields", Optional.empty());
		this.schema = schemaVersion.getSchema();
		this.schemaUuid = schemaVersion.getSchemaContainer().getUuid();
		this.context = context;
	}

	@Override
	protected List<FilterField<HibFieldContainer, ?>> getFilters() {
		return schema.getFields().stream()
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
		String name = fieldSchema.getName();
		String description = "Filters by the field " + name;
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		switch (type) {
		case STRING:
			return new FieldMappedFilter<>(type, name, description, StringFilter.filter(),
				node -> node == null ? null : getOrNull(node.getString(name), HibStringField::getString), schema);
		case HTML:
			return new FieldMappedFilter<>(type, name, description, StringFilter.filter(),
				node -> node == null ? null : getOrNull(node.getHtml(name), HibHtmlField::getHTML), schema);
		case DATE:
			return new FieldMappedFilter<>(type, name, description, DateFilter.filter(),
				node -> node == null ? null : getOrNull(node.getDate(name), HibDateField::getDate), schema);
		case BOOLEAN:
			return new FieldMappedFilter<>(type, name, description, BooleanFilter.filter(),
				node -> node == null ? null : getOrNull(node.getBoolean(name), HibBooleanField::getBoolean), schema);
		case NUMBER:
			return new FieldMappedFilter<>(type, name, description, NumberFilter.filter(),
				node -> node == null ? null : getOrNull(node.getNumber(name), val -> new BigDecimal(val.getNumber().toString())), schema);
		case MICRONODE:
			return new FieldMappedFilter<>(type, name, description, EntityReferenceFilter.micronodeFieldFilter(context, "MICRONODEFIELD"),
				node -> node == null ? null : getOrNull(node.getMicronode(name), Function.identity()), schema);
		case NODE:
			return new FieldMappedFilter<>(type, name, description, EntityReferenceFilter.nodeFieldFilter(context, "NODEFIELD"),
				node -> node == null ? null : getOrNull(node.getNode(name), Function.identity()), schema);
		case LIST:
			Pair<Filter<Collection<?>, ?>, Function<HibFieldContainer, Collection<?>>> listReferenceMap = listReferenceMapper(name, fieldSchema);
			return new FieldMappedFilter<>(type, name, description, listReferenceMap.getLeft(), 
					listReferenceMap.getRight(), schema, fieldSchema.maybeGetListField().map(ListFieldSchema::getListType).map(FieldTypes::valueByName));
		case BINARY:
			return new FieldMappedFilter<>(type, name, description, BinaryFieldFilter.filter("BINARYFIELD", context), 
					node -> node == null ? null : getOrNull(node.getBinary(name), Function.identity()), schema);
		case S3BINARY:
			return new FieldMappedFilter<>(type, name, description, S3BinaryFieldFilter.filter("S3BINARYFIELD"), 
					node -> node == null ? null : getOrNull(node.getS3Binary(name), Function.identity()), schema);
		default:
			throw new RuntimeException("Unexpected type " + type);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Pair<Filter<Collection<?>, ?>, Function<HibFieldContainer, Collection<?>>> listReferenceMapper(String name, FieldSchema fieldSchema) {
		ListFieldSchema listFieldSchema = fieldSchema.maybeGetListField().get();
		FieldTypes listType = FieldTypes.valueByName(listFieldSchema.getListType());
		
		ListFilter listFilter;
		Function<HibFieldContainer, HibListField<?,?,?>> listFieldGetter;
		switch (listType) {
		case BOOLEAN:
			listFilter = ListFilter.booleanListFilter();
			listFieldGetter = node -> node.getBooleanList(name);
			break;
		case DATE:
			listFilter = ListFilter.dateListFilter();
			listFieldGetter = node -> node.getDateList(name);
			break;
		case STRING:
			listFilter = ListFilter.stringListFilter();
			listFieldGetter = node -> node.getStringList(name);
			break;
		case HTML:
			listFilter = ListFilter.htmlListFilter();
			listFieldGetter = node -> node.getHTMLList(name);
			break;
		case MICRONODE:
			listFilter = ListFilter.micronodeListFilter(context);
			listFieldGetter = node -> node.getMicronodeList(name);
			break;
		case NUMBER:
			listFilter = ListFilter.numberListFilter();
			listFieldGetter = node -> node.getNumberList(name);
			break;
		case NODE:
			listFilter = ListFilter.nodeListFilter(context);
			listFieldGetter = node -> node.getNodeList(name);
			break;
		case BINARY:
		case S3BINARY:
		case LIST:
		default:
			throw new RuntimeException("Unexpected list type " + listType);
		}
		return Pair.of(listFilter, node -> node == null ? null : getOrNull(listFieldGetter.apply(node), HibListField::getValues));
	}

	@Override
	public Optional<String> maybeGetFilterId() {
		return Optional.of(schemaUuid);
	}

	private static <T, R> R getOrNull(T nullableValue, Function<T, R> mapper) {
		if (nullableValue == null) {
			return null;
		} else {
			return mapper.apply(nullableValue);
		}
	}
}
