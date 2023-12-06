package com.gentics.mesh.graphql.dataloader;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.dataloader.BatchLoaderWithContext;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingUserDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.PermissionException;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.model.NodeReferenceIn;
import com.gentics.mesh.graphql.type.NodeTypeProvider;
import com.gentics.mesh.parameter.PagingParameters;

import io.vertx.core.Promise;

/**
 * Implements batching and caching for nodes using the data loader pattern
 */
public class NodeDataLoader {

	public static final String CONTENT_LOADER_KEY = "contentLoader";
	public static final String CHILDREN_LOADER_KEY = "childrenLoader";
	public static final String PATH_LOADER_KEY = "pathLoader";
	public static final String REFERENCED_BY_LOADER_KEY = "referencedByLoader";
	public static final String PARENT_LOADER_KEY = "parentLoader";
	public static final String BREADCRUMB_LOADER_KEY = "breadcrumbLoader";

	/**
	 * Batch load all field containers of a node for transaction branch and the provided types, without doing permission
	 * checks.
	 */
	public static BatchLoaderWithContext<HibNode, List<HibNodeFieldContainer>> CONTENT_LOADER = (keys, environment) -> {
		Tx tx = Tx.get();
		GraphQLContext context = environment.getContext();
		String branchUuid = tx.getBranch(context).getUuid();
		Map<HibNode, List<HibNodeFieldContainer>> fieldsContainers = new HashMap<>();

		// query for all types provided
		partitioningByContainerType(environment.getKeyContexts(), (Pair<ContainerType, List<HibNode>> keysByContainerType) -> {
			Set<HibNode> keysForPartition = new HashSet<>(keysByContainerType.getValue());
			ContainerType type = keysByContainerType.getKey();
			Map<HibNode, List<HibNodeFieldContainer>> containerByType = tx.contentDao().getFieldsContainers(keysForPartition, branchUuid, type);
			fieldsContainers.putAll(containerByType);
		});

		List<List<HibNodeFieldContainer>> results = new ArrayList<>();
		for (HibNode key : keys) {
			results.add(fieldsContainers.getOrDefault(key, Collections.emptyList()));
		}
		Promise<List<List<HibNodeFieldContainer>>> promise = Promise.promise();
		promise.complete(results);

		return promise.future().toCompletionStage();
	};

	/**
	 * Batch load all children of a node for a transaction branch, the provided container type and language tags,
	 * also checking for permissions.
	 */
	public static BatchLoaderWithContext<HibNode, List<NodeContent>> CHILDREN_LOADER = (keys, environment) -> {
		Tx tx = Tx.get();
		GraphQLContext context = environment.getContext();
		String branchUuid = tx.getBranch(context).getUuid();

		Map<HibNode, List<NodeContent>> childrenByNode = new HashMap<>();
		partitioningByContext(environment.getKeyContexts(), (Pair<Context, List<HibNode>> keysByContext) -> {
			Set<HibNode> keysForPartition = new HashSet<>(keysByContext.getValue());
			List<String> languageTags = keysByContext.getKey().getLanguageTags();
			ContainerType type = keysByContext.getKey().getType();
			childrenByNode.putAll(tx.nodeDao().getChildren(keysForPartition, context, branchUuid, languageTags, type, keysByContext.getKey().getPaging(), keysByContext.getKey().getMaybeNativeFilter()));
		});

		List<List<NodeContent>> results = new ArrayList<>();
		for (HibNode key : keys) {
			results.add(childrenByNode.getOrDefault(key, Collections.emptyList()));
		}
		Promise<List<List<NodeContent>>> promise = Promise.promise();
		promise.complete(results);

		return promise.future().toCompletionStage();
	};

	/**
	 * Batch loading node parents
	 */
	public static BatchLoaderWithContext<NodeContent, Collection<NodeReferenceIn>> REFERENCED_BY_LOADER = (keys, environment) -> {
		GraphQLContext context = environment.getContext();

		// we will collect the results by keys here
		Map<NodeContent, List<NodeReferenceIn>> resultByKey = NodeReferenceIn.fromContent(context, keys).collect(Collectors.groupingBy(Pair::getValue, Collectors.mapping(Pair::getKey, Collectors.toList())));

		// collect the results as list in the correct order (corresponding to the order of the keys)
		List<Collection<NodeReferenceIn>> results = new ArrayList<>();
		for (NodeContent key : keys) {
			results.add(resultByKey.getOrDefault(key, Collections.emptyList()));
		}
		Promise<List<Collection<NodeReferenceIn>>> promise = Promise.promise();
		promise.complete(results);

		return promise.future().toCompletionStage();
	};

	/**
	 * Batch load path of a nodefieldcontainer for a transaction branch, the provided container type and language tags,
	 * without doing permission checks.
	 */
	public static BatchLoaderWithContext<HibNodeFieldContainer, String> PATH_LOADER = (keys, environment) -> {
		Tx tx = Tx.get();
		GraphQLContext context = environment.getContext();
		Map<HibNodeFieldContainer, String> contentPaths = new HashMap<>();

		partitioningByContext(environment.getKeyContexts(), (Pair<Context, List<HibNodeFieldContainer>> keysByContext) -> {
			Map<HibNodeFieldContainer, HibNode> nodeByContent = keysByContext.getValue().stream().collect(Collectors.toMap(Function.identity(), HibNodeFieldContainer::getNode));
			String[] languageTags = keysByContext.getKey().getLanguageTagsArray();
			ContainerType type = keysByContext.getKey().getType();
			Map<HibNode, String> nodePaths = tx.nodeDao().getPaths(nodeByContent.values(), context, type, languageTags);
			Map<HibNodeFieldContainer, String> pathsByPartition = nodeByContent.entrySet()
					.stream()
					.map(entry -> Pair.of(entry.getKey(), nodePaths.get(entry.getValue())))
					.filter(p -> p.getValue() != null)
					.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
			contentPaths.putAll(pathsByPartition);
		});

		List<String> results = new ArrayList<>();
		for (HibNodeFieldContainer key : keys) {
			results.add(contentPaths.getOrDefault(key, null));
		}
		Promise<List<String>> promise = Promise.promise();
		promise.complete(results);

		return promise.future().toCompletionStage();
	};

	/**
	 * Batch loading node parents
	 */
	public static BatchLoaderWithContext<ParentNodeLoaderKey, NodeContentWithOptionalRuntimeException> PARENT_LOADER = (keys, environment) -> {
		Tx tx = Tx.get();
		GraphQLContext context = environment.getContext();
		HibUser user = context.getUser();
		String branchUuid = tx.getBranch(context).getUuid();
		PersistingUserDao userDao = CommonTx.get().userDao();

		// we will collect the results by keys here
		Map<ParentNodeLoaderKey, NodeContentWithOptionalRuntimeException> resultByKey = new HashMap<>();

		// first load the parent nodes
		Map<HibNode, HibNode> parentByNode = tx.nodeDao().getParentNodes(
				keys.stream().map(ParentNodeLoaderKey::getNode).collect(Collectors.toSet()), branchUuid);

		if (!user.isAdmin()) {
			// prepare the permissions for all parent nodes
			Set<Object> parentNodeIds = parentByNode.values().stream().filter(parent -> parent != null)
					.map(HibNode::getId).collect(Collectors.toSet());
			userDao.preparePermissionsForElementIds(user, parentNodeIds);
		}

		// now partition the keys by type and language tags for loading the containers
		ParentNodeLoaderKey.partitioningByTypeAndLanguageTags(keys, (partContext, partNodes) -> {
			ContainerType type = partContext.getType();
			InternalPermission perm = type == PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM;
			List<String> languageTags = partContext.getLanguageTags();

			Set<HibNode> partParents = partNodes.stream()
					.map(node -> parentByNode.get(node))
					.filter(parent -> parent != null)
					.collect(Collectors.toSet());

			// load all containers (languages) for the partition
			Map<HibNode, List<HibNodeFieldContainer>> containersForNodePartitions = tx.contentDao()
					.getFieldsContainers(partParents, branchUuid, type);

			// make language fallback for each parent from the partition
			Map<HibNode, NodeContentWithOptionalRuntimeException> contentsByParentNode = partParents.stream().map(parentNode -> {
				NodeContentWithOptionalRuntimeException result = null;

				// first we check, whether the user has ANY read permission on the parentNode
				if (user.isAdmin() || userDao.hasPermission(user, parentNode, READ_PUBLISHED_PERM) || userDao.hasPermission(user, parentNode, READ_PERM)) {
					// now check whether the user has the correct permission
					if (user.isAdmin() || userDao.hasPermission(user, parentNode, perm)) {
						List<HibNodeFieldContainer> containers = containersForNodePartitions.getOrDefault(parentNode, Collections.emptyList());
						HibNodeFieldContainer container = NodeTypeProvider.getContainerWithFallback(languageTags, containers);
						result = new NodeContentWithOptionalRuntimeException(new NodeContent(parentNode, container, languageTags, type));
					} else {
						PermissionException error = new PermissionException("node", parentNode.getUuid());
						result = new NodeContentWithOptionalRuntimeException(new NodeContent(parentNode, null, languageTags, type), error);
					}
				} else {
					// no permission at all, so we will just return the error
					PermissionException error = new PermissionException("node", parentNode.getUuid());
					result = new NodeContentWithOptionalRuntimeException(error);
				}

				return Map.entry(parentNode, result);
			}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			// for all nodes in the partition, get the content of the parent node and put into the resultsByKey map
			for (HibNode node : partNodes) {
				HibNode parentNode = parentByNode.get(node);
				if (parentNode != null) {
					NodeContentWithOptionalRuntimeException content = contentsByParentNode.get(parentNode);
					if (content != null) {
						resultByKey.put(new ParentNodeLoaderKey(node, partContext), content);
					}
				}
			}
		});

		// collect the results as list in the correct order (corresponding to the order of the keys)
		List<NodeContentWithOptionalRuntimeException> results = new ArrayList<>();
		for (ParentNodeLoaderKey key : keys) {
			results.add(resultByKey.getOrDefault(key, NodeContentWithOptionalRuntimeException.EMPTY));
		}
		Promise<List<NodeContentWithOptionalRuntimeException>> promise = Promise.promise();
		promise.complete(results);

		return promise.future().toCompletionStage();
	};

	public static BatchLoaderWithContext<HibNode, List<NodeContent>> BREADCRUMB_LOADER = (keys, environment) -> {
		NodeDao nodeDao = Tx.get().nodeDao();
		ContentDao contentDao = Tx.get().contentDao();
		GraphQLContext context = environment.getContext();
		HibBranch branch = Tx.get().getBranch(context);

		Map<HibNode, List<NodeContent>> nodeContentMap = new HashMap<>();
		partitioningByContext(environment.getKeyContexts(), (Pair<Context, List<HibNode>> keysByContext) -> {
			List<HibNode> keysForPartition = keysByContext.getValue();
			List<String> languageTags = keysByContext.getKey().getLanguageTags();
			ContainerType type = keysByContext.getKey().getType();
			Map<HibNode, List<HibNode>> breadcrumbNodesMap = nodeDao.getBreadcrumbNodesMap(keysForPartition, context);
			Set<HibNode> nodes = breadcrumbNodesMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
			Map<HibNode, List<HibNodeFieldContainer>> fieldsContainers = contentDao.getFieldsContainers(nodes, branch.getUuid(), type);

			Map<HibNode, NodeContent> contentByNode = fieldsContainers.entrySet().stream()
					.map(kv -> {
						List<HibNodeFieldContainer> containers = kv.getValue();
						HibNodeFieldContainer container = NodeTypeProvider.getContainerWithFallback(languageTags, containers);
						return new NodeContent(kv.getKey(), container, languageTags, type);
					}).collect(Collectors.toMap(NodeContent::getNode, Function.identity()));

			Map<HibNode, List<NodeContent>> resultForPartition = breadcrumbNodesMap.entrySet().stream()
					.map(kv -> {
						HibNode sourceNode = kv.getKey();
						List<NodeContent> contents = kv.getValue().stream().map(contentByNode::get).collect(Collectors.toList());

						return Pair.of(sourceNode, contents);
					}).collect(Collectors.toMap(Pair::getKey, Pair::getValue));

			nodeContentMap.putAll(resultForPartition);
		});

		Promise<List<List<NodeContent>>> promise = Promise.promise();
		List<List<NodeContent>> results = new ArrayList<>();
		for (HibNode key : keys) {
			results.add(nodeContentMap.getOrDefault(key, Collections.emptyList()));
		}
		promise.complete(results);

		return promise.future().toCompletionStage();
	};

	@SuppressWarnings("unchecked")
	private static <T> void partitioningByContext(Map<Object, Object> keyContexts, Consumer<Pair<Context, List<T>>> consumer) {
		Map<Context, List<T>> partitionedContext = keyContexts.entrySet().stream()
				.map(kv -> Pair.of((T) kv.getKey(), (Context) kv.getValue()))
				.collect(Collectors.groupingBy(Pair::getValue, Collectors.mapping(Pair::getKey, Collectors.toList())));

		partitionedContext.forEach((key, value) -> consumer.accept(Pair.of(key, value)));
	}

	@SuppressWarnings("unchecked")
	private static <T> void partitioningByContainerType(Map<Object, Object> keyContexts, Consumer<Pair<ContainerType, List<T>>> consumer) {
		Map<ContainerType, List<T>> partitionedContext = keyContexts.entrySet().stream()
				.map(kv -> Pair.of((T) kv.getKey(), ((Context) kv.getValue()).type))
				.collect(Collectors.groupingBy(Pair::getValue, Collectors.mapping(Pair::getKey, Collectors.toList())));

		partitionedContext.forEach((key, value) -> consumer.accept(Pair.of(key, value)));
	}

	/**
	 * The context of the loader for a specific key
	 */
	public static class Context {
		private ContainerType type;
		
		private String languageTags = "";

		private Optional<FilterOperation<?>> maybeNativeFilter = Optional.empty();

		private PagingParameters paging;

		public Context(ContainerType type) {
			this.type = type;
		}

		public Context(ContainerType type, List<String> languageTags, Optional<FilterOperation<?>> maybeNativeFilter, PagingParameters pagingInfo) {
			this.type = type;
			// we are using linked hashset to deduplicate languageTags while still preserving order
			this.languageTags = String.join(",", new LinkedHashSet<>(languageTags));
			this.maybeNativeFilter = maybeNativeFilter;
			this.paging = pagingInfo;
		}

		public ContainerType getType() {
			return type;
		}

		public List<String> getLanguageTags() {
			return Arrays.asList(getLanguageTagsArray());
		}

		public String[] getLanguageTagsArray() {
			return languageTags.split(",");
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Context)) return false;
			Context context = (Context) o;
			return type == context.type && Objects.equals(languageTags, context.languageTags);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, languageTags);
		}

		public Optional<FilterOperation<?>> getMaybeNativeFilter() {
			return maybeNativeFilter;
		}

		public PagingParameters getPaging() {
			return paging;
		}
	}

	/**
	 * Keys for batchloading of parents. An instance of a key contains of the node and the {@link Context} (which contains of the {@link ContainerType} and the language tags).
	 */
	public static class ParentNodeLoaderKey {
		/**
		 * Node for which the parent node shall be loaded
		 */
		private HibNode node;

		/**
		 * Context for loading the parent node
		 */
		private Context context;

		/**
		 * Partition the given list of {@link ParentNodeLoaderKey} instances by {@link Context} and call the consumer for each partition
		 * @param keyList list of keys which shall be partitioned
		 * @param consumer consumer for handling of each partition
		 */
		public static void partitioningByTypeAndLanguageTags(List<ParentNodeLoaderKey> keyList, BiConsumer<Context, Set<HibNode>> consumer) {
			Map<Context, Set<HibNode>> partitions = keyList.stream()
					.collect(Collectors.groupingBy(ParentNodeLoaderKey::getContext,
							Collectors.mapping(ParentNodeLoaderKey::getNode, Collectors.toSet())));

			partitions.forEach((key, value) -> consumer.accept(key, value));
		}

		/**
		 * Create an instance
		 * @param node node for which the parent node shall be loaded
		 * @param context context for loading the parent node
		 */
		public ParentNodeLoaderKey(HibNode node, Context context) {
			this.node = node;
			this.context = context;
		}

		/**
		 * Create an instance
		 * @param node node for which the parent node shall be loaded
		 * @param type type of content to be loaded
		 * @param languageTags language tags for loading the content
		 * @param pagingInfo 
		 */
		public ParentNodeLoaderKey(HibNode node, ContainerType type, List<String> languageTags, PagingParameters pagingInfo) {
			this(node, new Context(type, languageTags, Optional.empty(), pagingInfo));
		}

		/**
		 * Get the node for which the parent node shall be loaded
		 * @return child node
		 */
		public HibNode getNode() {
			return node;
		}

		/**
		 * Get the context for loading the content of the parent node
		 * @return context
		 */
		public Context getContext() {
			return context;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof ParentNodeLoaderKey)) {
				return false;
			}
			ParentNodeLoaderKey other = (ParentNodeLoaderKey) o;
			return Objects.equals(context, other.context) && Objects.equals(node, other.node);
		}

		@Override
		public int hashCode() {
			return Objects.hash(context, node);
		}
	}
}
