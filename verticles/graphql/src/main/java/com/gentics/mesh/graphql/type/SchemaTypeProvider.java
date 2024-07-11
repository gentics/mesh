package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.graphql.type.NodeTypeProvider.NODE_PAGE_TYPE_NAME;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.core.data.NamedElement;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingRootDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.filter.NodeFilter;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.PagingParameters;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLOutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SchemaTypeProvider extends AbstractTypeProvider {

	private static final Logger log = LoggerFactory.getLogger(SchemaTypeProvider.class);

	public static final String SCHEMA_TYPE_NAME = "Schema";

	public static final String SCHEMA_PAGE_TYPE_NAME = "SchemasPage";

	public static final String SCHEMA_FIELD_TYPE = "SchemaFieldType";

	protected final InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public SchemaTypeProvider(MeshOptions options, InterfaceTypeProvider interfaceTypeProvider) {
		super(options);
		this.interfaceTypeProvider = interfaceTypeProvider;
	}

	public GraphQLObjectType createType(GraphQLContext context) {
		Builder schemaType = newObject().name(SCHEMA_TYPE_NAME).description("Node schema");
		interfaceTypeProvider.addCommonFields(schemaType);

		schemaType.field(newFieldDefinition().name("name").type(GraphQLString).dataFetcher((env) -> {
			Object source = env.getSource();
			if (source instanceof NamedElement) {
				return ((NamedElement) source).getName();
			}
			return null;
		}));

		// schemaType.field(newPagingFieldWithFetcher("projects", "Projects that this schema is assigned to", (env) -> {
		// GraphQLContext gc = env.getContext();
		// SchemaContainer schema = env.getSource();
		// return schema.findReferencedBranches().keySet().stream().map(Branch::getProject).distinct()
		// .filter(it -> gc.getUser().hasPermission(it, GraphPermission.READ_PERM)).collect(Collectors.toList());
		// }, PROJECT_REFERENCE_PAGE_TYPE_NAME));

		// .isEmpty
		schemaType.field(newFieldDefinition().name("isEmpty").type(GraphQLBoolean).dataFetcher((env) -> {
			SchemaVersionModel model = loadModelWithFallback(env);
			return model == null || model.getFields() == null || model.getFields().isEmpty();
		}));

		// .isContainer
		schemaType.field(newFieldDefinition().name("isContainer").type(GraphQLBoolean).dataFetcher((env) -> {
			SchemaVersionModel model = loadModelWithFallback(env);
			return model != null ? model.getContainer() : null;
		}));

		// .isAutoPurge
		schemaType.field(newFieldDefinition().name("isAutoPurge").type(GraphQLBoolean).dataFetcher((env) -> {
			SchemaVersionModel model = loadModelWithFallback(env);
			return model != null ? model.getAutoPurge() : null;
		}));

		// .displayField
		schemaType.field(newFieldDefinition().name("displayField").type(GraphQLString).dataFetcher((env) -> {
			SchemaVersionModel model = loadModelWithFallback(env);
			return model != null ? model.getDisplayField() : null;
		}));

		// .segmentField
		schemaType.field(newFieldDefinition().name("segmentField").type(GraphQLString).dataFetcher((env) -> {
			SchemaVersionModel model = loadModelWithFallback(env);
			return model != null ? model.getSegmentField() : null;
		}));

		// .nodes
		NodeFilter nodeFilter = NodeFilter.filter(context);
		schemaType
			.field(newPagingFieldWithFetcherBuilder("nodes", "Load nodes with this schema", env -> {
				Tx tx = Tx.get();
				NodeDao nodeDao = tx.nodeDao();
				GraphQLContext gc = env.getContext();
				List<String> languageTags = getLanguageArgument(env);
				ContainerType type = getNodeVersion(env);
				PagingParameters pagingInfo = getPagingInfo(env);
				SchemaDao schemaDao = tx.schemaDao();
				Pair<Predicate<NodeContent>, Optional<FilterOperation<?>>> filters = parseFilters(env, nodeFilter);
				Stream<? extends NodeContent> nodes = nodeDao.findAllContent(getSchemaContainerVersion(env), gc, languageTags, type, pagingInfo, filters.getRight());

				return applyNodeFilter(env, nodes, filters.getRight().isPresent()  && PersistingRootDao.shouldPage(pagingInfo), filters.getRight().isPresent());
			}, NODE_PAGE_TYPE_NAME, true)
				.argument(nodeFilter.createFilterArgument())
				.argument(nodeFilter.createSortArgument())
				.argument(createNativeFilterArg())
				.argument(createLanguageTagArg(true)));

		Builder fieldListBuilder = newObject().name(SCHEMA_FIELD_TYPE).description("List of schema fields");

		// .name
		fieldListBuilder.field(newFieldDefinition().name("name").type(GraphQLString).description("Name of the field"));

		// .label
		fieldListBuilder.field(newFieldDefinition().name("label").type(GraphQLString).description("Label of the field"));

		// .required
		fieldListBuilder.field(newFieldDefinition().name("required").type(GraphQLBoolean).description("Whether this field is required"));

		// .type
		fieldListBuilder.field(newFieldDefinition().name("type").type(GraphQLString).description("The type of the field"));
		// TODO add "allow" and "indexSettings"

		GraphQLOutputType type = GraphQLList.list(fieldListBuilder.build());

		// .fields
		schemaType.field(newFieldDefinition().name("fields").type(type).dataFetcher(env -> loadModelWithFallback(env).getFields()));

		return schemaType.build();
	}

	private SchemaVersion getSchemaContainerVersion(DataFetchingEnvironment env) {
		Object source = env.getSource();
		if (source instanceof SchemaVersion) {
			return (SchemaVersion) source;
		} else if (source instanceof Schema) {
			return ((Schema) source).getLatestVersion();
		} else {
			throw new RuntimeException("Invalid type {" + source + "}.");
		}
	}

	private SchemaVersionModel loadModelWithFallback(DataFetchingEnvironment env) {
		Object source = env.getSource();
		if (source instanceof Schema) {
			Schema schema = env.getSource();
			SchemaVersionModel model = JsonUtil.readValue(schema.getLatestVersion().getJson(), SchemaModelImpl.class);
			return model;
		}
		if (source instanceof SchemaVersion) {
			SchemaVersion schema = env.getSource();
			SchemaVersionModel model = JsonUtil.readValue(schema.getJson(), SchemaModelImpl.class);
			return model;
		}
		log.error("Invalid type {" + source + "} for schema model loading.");
		return null;
	}
}
