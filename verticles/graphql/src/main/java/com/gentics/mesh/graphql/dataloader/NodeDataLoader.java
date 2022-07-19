package com.gentics.mesh.graphql.dataloader;

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
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.type.NodeTypeProvider;
import io.vertx.core.Promise;
import org.apache.commons.lang3.tuple.Pair;
import org.dataloader.BatchLoaderWithContext;

/**
 * Implements batching and caching for nodes using the data loader pattern
 */
public class NodeDataLoader {

	public static final String CONTENT_LOADER_KEY = "contentLoader";
	public static final String CHILDREN_LOADER_KEY = "childrenLoader";
	public static final String PATH_LOADER_KEY = "pathLoader";
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
			childrenByNode.putAll(tx.nodeDao().getChildren(keysForPartition, context, branchUuid, languageTags, type));
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

	private static <T> void partitioningByContext(Map<Object, Object> keyContexts, Consumer<Pair<Context, List<T>>> consumer) {
		Map<Context, List<T>> partitionedContext = keyContexts.entrySet().stream()
				.map(kv -> Pair.of((T) kv.getKey(), (Context) kv.getValue()))
				.collect(Collectors.groupingBy(Pair::getValue, Collectors.mapping(Pair::getKey, Collectors.toList())));

		partitionedContext.forEach((key, value) -> consumer.accept(Pair.of(key, value)));
	}

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

		public Context(ContainerType type) {
			this.type = type;
		}

		public Context(ContainerType type, List<String> languageTags) {
			this.type = type;
			// we are using linked hashset to deduplicate languageTags while still preserving order
			this.languageTags = String.join(",", new LinkedHashSet<>(languageTags));
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
	}
}
