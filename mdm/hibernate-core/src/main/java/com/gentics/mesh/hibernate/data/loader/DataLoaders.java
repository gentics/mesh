package com.gentics.mesh.hibernate.data.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingUserDao;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.dao.ContentDaoImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibMicronodeFieldImpl;
import com.gentics.mesh.hibernate.util.TriFunction;
import com.gentics.mesh.util.VersionNumber;

/**
 * Gives access to functions that implement batching and caching for content/node information.
 * Usage example:
 * <pre>{@code
 *
 * 	DataLoaders contentLoader = new DataLoaders(nodes, ac, Collections.singletonList(DataLoaders.Loader.PARENT_LOADER_KEY));
 * 	// Batching: the code below will trigger a single query, instead of nodes.size() queries
 * 	var loader = contentLoader.getBreadcrumbsLoader().get();
 * 	for (HibNode node: nodes) {
 * 	    loader.apply(node);
 * 	}
 *
 * 	// Caching: the code below will not trigger any query, since the cached values will be returned
 * 	for (HibNode node: nodes) {
 *   	 loader.apply(node);
 *  }
 * }
 * </pre>
 *
 * The loaders functions are memoized, so that the first call might take a long time, while subsequent calls will return
 * the stored value immediately.
 * It's responsibility of the consumers to make sure memoized data won't get out of date.
 * A good rule of thumb is to register loader functions for data that won't change during the loaders lifetime
 *
 */
public class DataLoaders {

	private final Set<HibNode> nodes;
	private final HibBranch branch;
	private final InternalActionContext ac;
	private final List<Loader> loaders;
	private final ContentDaoImpl contentDao;
	private final NodeDao nodeDao;

	private final Map<ContainerType, Map<HibNode, List<HibNodeFieldContainer>>> containersByType = new HashMap<>();
	private final Map<String, Map<HibNode, List<HibNodeFieldContainer>>> containersByVersion = new HashMap<>();

	private Map<HibNode, HibNode> parentNodes;
	private Map<HibNode, List<HibNode>> children;
	private Map<HibNode, List<HibNode>> breadcrumbs;

	/**
	 * Batch Loaded Micronodes (keys are pairs of contentUUID/fieldKey)
	 */
	private Map<Pair<String, String>, HibMicronode> micronodes;

	/**
	 * Create a DataLoaders, registering the provided loaders functions
	 * @param nodes
	 * @param ac
	 * @param loaders
	 */
	public DataLoaders(Collection<? extends HibNode> nodes, InternalActionContext ac, List<Loader> loaders) {
		this.nodes = new HashSet<>(nodes);
		this.branch = Tx.get().getBranch(ac);
		this.ac = ac;
		this.loaders = loaders;
		this.contentDao = HibernateTx.get().contentDao();
		this.nodeDao = Tx.get().nodeDao();
	}

	/**
	 * Get an optional function that given a {@link HibNode} and a {@link ContainerType} and returns a list of containers
	 * @return
	 */
	public Optional<BiFunction<HibNode, ContainerType, List<HibNodeFieldContainer>>> getTypeLoader() {
		if (!loaders.contains(Loader.FOR_TYPE_CONTENT_LOADER)) {
			return Optional.empty();
		}

		return Optional.of(this::loadContainersForType);
	}

	/**
	 * Get an optional function that take given a {@link HibNode}, a language list and a version returns the matching container
	 * @return
	 */
	public Optional<TriFunction<HibNode, List<String>, String, HibNodeFieldContainer>> getVersionLoader() {
		if (!loaders.contains(Loader.FOR_VERSION_CONTENT_LOADER)) {
			return Optional.empty();
		}

		return Optional.of(this::loadContainerForVersion);
	}

	/**
	 * Get an optional function that given a {@link HibNode} returns its parent
	 * @return
	 */
	public Optional<Function<HibNode, HibNode>> getParentLoader() {
		if (!loaders.contains(Loader.PARENT_LOADER)) {
			return Optional.empty();
		}

		return Optional.of(this::loadParentNode);
	}

	/**
	 * Get an optional function that given a {@link HibNode} returns its children
	 * @return
	 */
	public Optional<Function<HibNode, List<HibNode>>> getChildrenLoader() {
		if (!loaders.contains(Loader.CHILDREN_LOADER)) {
			return Optional.empty();
		}

		return Optional.of(this::loadChildren);
	}

	/**
	 * Get an optional function that given a {@link HibNode} returns its breadcrumbs
	 * @return
	 */
	public Optional<Function<HibNode, List<HibNode>>> getBreadcrumbsLoader() {
		if (!loaders.contains(Loader.BREADCRUMBS_LOADER)) {
			return Optional.empty();
		}

		return Optional.of(this::loadBreadcrumbs);
	}

	/**
	 * Get the Micronode from the given micronode field.
	 * If the {@link Loader#MICRONODE_LOADER} has been initialized, it will be used for loading the micronode, otherwise (or if the loader did not load the micronode),
	 * the given default supplier will be used.
	 * @param field micronode field
	 * @param defaultSupplier default supplier
	 * @return micronode
	 */
	public HibMicronode getMicronode(HibMicronodeField field, Supplier<HibMicronode> defaultSupplier) {
		if (!loaders.contains(Loader.MICRONODE_LOADER)) {
			return defaultSupplier.get();
		}

		return loadMicronode(field, defaultSupplier);
	}

	/**
	 * Enumeration of all available loaders
	 */
	public enum Loader {
		/** Loads children of a node */
		CHILDREN_LOADER,

		/** Loads the breadcrumbs of a node */
		BREADCRUMBS_LOADER,

		/** Loads a list of containers for a node and a content type */
		FOR_TYPE_CONTENT_LOADER,

		/** Loads the container for a node, a language list and  a version */
		FOR_VERSION_CONTENT_LOADER,

		/** Loads the parent of a node */
		PARENT_LOADER,

		/** Loads micronodes **/
		MICRONODE_LOADER,

		/** Loads items of list fields */
		LIST_ITEM_LOADER;
	}

	/**
	 * Return a list of node field containers for the node and the container type. The result will be cached so that
	 * if the method is called again it will return the same value immediately
	 * @param node
	 * @param type
	 * @return
	 */
	private List<HibNodeFieldContainer> loadContainersForType(HibNode node, ContainerType type) {
		Supplier<List<HibNodeFieldContainer>> fallback = () -> contentDao.getFieldsContainers(Collections.singleton(node), branch.getUuid(), type)
				.getOrDefault(node, Collections.emptyList());
		return load(node, () -> loadContainersForType(type), fallback);
	}

	/**
	 * Return a list of node field containers for the node and the provided version.
	 * The result will be cached so that if the method is called again it will return the same value immediately
	 * @param node
	 * @param version
	 * @return
	 */
	private List<HibNodeFieldContainer> loadContainersForVersion(HibNode node, String version) {
		Supplier<List<HibNodeFieldContainer>> fallback = () -> {
			ContainerType containerType = ContainerType.forVersion(version);
			if (!containerType.equals(ContainerType.INITIAL)) {
				return contentDao.getFieldsContainers(Collections.singleton(node), branch.getUuid(), containerType).getOrDefault(node, Collections.emptyList());
			}

			return contentDao.getFieldsContainers(Collections.singleton(node), branch.getUuid(), new VersionNumber(version)).getOrDefault(node, Collections.emptyList());
		};

		return load(node, () -> loadContainersForVersion(version), fallback);
	}

	/**
	 * Return the first container matching the provided language tags and version, otherwise returns null.
	 * The result will be cached so that if the method is called again it will return the same value immediately
	 * @param node
	 * @param languageTags
	 * @param version
	 * @return
	 */
	private HibNodeFieldContainer loadContainerForVersion(HibNode node, List<String> languageTags, String version) {
		Map<String, HibNodeFieldContainer> containers = loadContainersForVersion(node, version).stream()
				.collect(Collectors.toMap(HibNodeFieldContainer::getLanguageTag, Function.identity(), (a, b) -> a));

		for (String languageTag : languageTags) {
			HibNodeFieldContainer container = containers.get(languageTag);
			if (container != null) {
				return container;
			}
		}

		return null;
	}

	/**
	 * Load the parent node
	 * The result will be cached so that if the method is called again it will return the same value immediately
	 * @param node
	 * @return
	 */
	private HibNode loadParentNode(HibNode node) {
		return load(node, this::getParentNodes, () -> nodeDao.getParentNode(node, branch.getUuid()));
	}

	/**
	 * Load the children of the node
	 * The result will be cached so that if the method is called again it will return the same value immediately
	 * @param node
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<HibNode> loadChildren(HibNode node) {
		return load(node, this::loadChildren, () -> (List<HibNode>) nodeDao.getChildren(node, branch.getUuid()).list());
	}

	/**
	 * Load the breadcrumb of the node
	 * The result will be cached so that if the method is called again it will return the same value immediately
	 * @param node
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<HibNode> loadBreadcrumbs(HibNode node) {
		return load(node, this::loadBreadcrumbsMap, () -> (List<HibNode>) nodeDao.getBreadcrumbNodes(node, ac).list());
	}

	/**
	 * Load the micronode of the micronode field, either by fetching it from the {@link #micronodes} map (which will be filled, if that was not done before),
	 * or by using the given default supplier.
	 * @param field micronode field
	 * @param defaultSupplier default supplier
	 * @return micronode
	 */
	private HibMicronode loadMicronode(HibMicronodeField field, Supplier<HibMicronode> defaultSupplier) {
		Optional<Pair<String, String>> optKey = getHashKey(field);
		if (optKey.isPresent()) {
			Pair<String, String> key = optKey.get();
			Map<Pair<String, String>, HibMicronode> micronodesMap = loadMicronodes();
			if (micronodesMap.containsKey(key)) {
				return micronodesMap.get(key);
			}
			HibMicronode micronode = defaultSupplier.get();
			micronodesMap.put(key, micronode);
			return micronode;
		} else {
			return defaultSupplier.get();
		}
	}

	private Map<HibNode, List<HibNodeFieldContainer>> loadContainersForType(ContainerType type) {
		if (!containersByType.containsKey(type)) {
			Map<HibNode, List<HibNodeFieldContainer>> fieldsContainers = contentDao.getFieldsContainers(nodes, branch.getUuid(), type);
			// the map will not contain entries for nodes, that do not have any containers of the given type, so we add those (mapping to empty lists)
			for (HibNode node : nodes) {
				fieldsContainers.computeIfAbsent(node, n -> Collections.emptyList());
			}
			containersByType.put(type, fieldsContainers);

			if (loaders.contains(Loader.LIST_ITEM_LOADER)) {
				List<HibNodeFieldContainer> containers = new ArrayList<>();
				fieldsContainers.values().forEach(list -> containers.addAll(list));
				contentDao.loadListFields(containers);
			}
		}

		return containersByType.get(type);
	}

	private Map<HibNode, List<HibNode>> loadBreadcrumbsMap() {
		if (breadcrumbs == null) {
			breadcrumbs = nodeDao.getBreadcrumbNodesMap(nodes, ac);
		}

		return breadcrumbs;
	}

	private Map<HibNode, List<HibNodeFieldContainer>> loadContainersForVersion(String version) {
		ContainerType containerType = ContainerType.forVersion(version);
		if (containerType.equals(ContainerType.PUBLISHED) || containerType.equals(ContainerType.DRAFT)) {
			return loadContainersForType(containerType);
		}

		if (!containersByVersion.containsKey(version)) {
			Map<HibNode, List<HibNodeFieldContainer>> fieldContainers = contentDao.getFieldsContainers(nodes, branch.getUuid(), new VersionNumber(version));
			containersByVersion.put(version, fieldContainers);
		}

		return containersByVersion.get(version);
	}

	private Map<HibNode, HibNode> getParentNodes() {
		if (parentNodes == null) {
			parentNodes = nodeDao.getParentNodes(nodes, branch.getUuid());
		}

		return parentNodes;
	}

	private Map<HibNode, List<HibNode>> loadChildren() {
		if (children == null) {
			children = nodeDao.getChildren(nodes, branch.getUuid());

			// also prepare the permissions for the children
			PersistingUserDao userDao = CommonTx.get().userDao();
			List<Object> nodeIds = children.values().stream().flatMap(list -> list.stream()).map(HibNode::getId).collect(Collectors.toList());
			userDao.preparePermissionsForElementIds(ac.getUser(), nodeIds);
		}

		return children;
	}

	/**
	 * If {@link #micronodes} has not been filled, do this now for all contents from {@link #containersByType}
	 * @return filled micronodes map
	 */
	private Map<Pair<String, String>, HibMicronode> loadMicronodes() {
		if (micronodes == null) {
			List<HibMicronodeField> micronodeFields = new ArrayList<>();
			for (Map<HibNode, List<HibNodeFieldContainer>> map : containersByType.values()) {
				for (List<HibNodeFieldContainer> lists : map.values()) {
					for (HibNodeFieldContainer content : lists) {
						for (HibField field : content.getFields()) {
							if (field instanceof HibMicronodeField) {
								micronodeFields.add(HibMicronodeField.class.cast(field));
							}
						}
					}
				}
			}

			Map<HibMicronodeField, HibMicronode> tempMap = contentDao.getMicronodes(micronodeFields);
			micronodes = new HashMap<>();
			for (Entry<HibMicronodeField, HibMicronode> entry : tempMap.entrySet()) {
				getHashKey(entry.getKey()).ifPresent(key -> {
					micronodes.put(key, entry.getValue());
				});
			}
		}

		return micronodes;
	}

	/**
	 * Get the optional hash key to be used as key in the {@link #micronodes} map for the given micronode field.
	 * If the micronode field is not an instance of {@link HibMicronodeFieldImpl}, an empty optional is returned
	 * @param micronodeField micronode field
	 * @return optional hash key
	 */
	private Optional<Pair<String, String>> getHashKey(HibMicronodeField micronodeField) {
		if (micronodeField instanceof HibMicronodeFieldImpl) {
			HibMicronodeFieldImpl impl = HibMicronodeFieldImpl.class.cast(micronodeField);
			return Optional.of(Pair.of(impl.getContainer().getUuid(), impl.getFieldKey()));
		} else {
			return Optional.empty();
		}
	}

	private <T> T load(HibNode node, Supplier<Map<HibNode, T>> mapSupplier, Supplier<T> fallbackValue) {
		nodes.add(node);
		Map<HibNode, T> map = mapSupplier.get();

		if (!map.containsKey(node)) {
			// this could happen when the data loader is loading a node that is not contained in the initial nodes
			// provided in the constructor.
			T value = fallbackValue.get();
			map.put(node, value);
		}

		return map.get(node);
	}
}
