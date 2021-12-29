package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.INITIAL;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.gentics.mesh.core.data.HibNodeFieldContainerEdge;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.data.node.field.HibNumberField;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.BooleanGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.DateGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.HtmlGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.NumberGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.graph.GraphAttribute;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.util.VersionNumber;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.FramedGraph;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ContentDaoWrapperImpl implements ContentDaoWrapper {

	private static final Logger log = LoggerFactory.getLogger(ContentDaoWrapperImpl.class);

	private final Database db;

	@Inject
	public ContentDaoWrapperImpl(Database db) {
		this.db = db;
	}

	@Override
	public HibNodeFieldContainer getLatestDraftFieldContainer(HibNode node, String languageTag) {
		return toGraph(node).getLatestDraftFieldContainer(languageTag);
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(HibNode node, String languageTag) {
		return toGraph(node).getFieldContainer(languageTag);
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(HibNode node, String languageTag, String branchUuid, ContainerType type) {
		return toGraph(node).getFieldContainer(languageTag, branchUuid, type);
	}

	@Override
	public Result<? extends HibNodeFieldContainerEdge> getFieldEdges(HibNode node, String branchUuid, ContainerType type) {
		return toGraph(node).getFieldContainerEdges(branchUuid, type);
	}

	@Override
	public long getFieldContainerCount(HibNode node) {
		return toGraph(node).getFieldContainerCount();
	}

	@Override
	public Stream<HibNodeField> getInboundReferences(HibNode node) {
		return toGraph(node).getInboundReferences();
	}

	@Override
	public void delete(HibNodeFieldContainer content, BulkActionContext bac) {
		content.delete(bac);
	}

	@Override
	public void delete(HibNodeFieldContainer content, BulkActionContext bac, boolean deleteNext) {
		content.delete(bac, deleteNext);
	}

	@Override
	public void deleteFromBranch(HibNodeFieldContainer content, HibBranch branch, BulkActionContext bac) {
		content.deleteFromBranch(branch, bac);
	}

	@Override
	public String getDisplayFieldValue(HibNodeFieldContainer content) {
		return content.getDisplayFieldValue();
	}

	@Override
	public HibNode getNode(HibNodeFieldContainer content) {
		return content.getNode();
	}

	@Override
	public VersionNumber getVersion(HibNodeFieldContainer content) {
		return content.getVersion();
	}

	@Override
	public void setVersion(HibNodeFieldContainer content, VersionNumber version) {
		content.setVersion(version);
	}

	@Override
	public boolean hasNextVersion(HibNodeFieldContainer content) {
		return content.hasNextVersion();
	}

	@Override
	public Iterable<HibNodeFieldContainer> getNextVersions(HibNodeFieldContainer content) {
		return content.getNextVersions();
	}

	@Override
	public void setNextVersion(HibNodeFieldContainer current, HibNodeFieldContainer next) {
		current.setNextVersion(next);
	}

	@Override
	public boolean hasPreviousVersion(HibNodeFieldContainer content) {
		return content.hasPreviousVersion();
	}

	@Override
	public HibNodeFieldContainer getPreviousVersion(HibNodeFieldContainer content) {
		return content.getPreviousVersion();
	}

	@Override
	public void clone(HibNodeFieldContainer dest, HibNodeFieldContainer src) {
		dest.clone(src);
	}

	@Override
	public boolean isType(HibNodeFieldContainer content, ContainerType type) {
		return content.isType(type);
	}

	@Override
	public boolean isType(HibNodeFieldContainer content, ContainerType type, String branchUuid) {
		return content.isType(type, branchUuid);
	}

	@Override
	public Set<String> getBranches(HibNodeFieldContainer content, ContainerType type) {
		return content.getBranches(type);
	}

	@Override
	public List<FieldContainerChange> compareTo(HibNodeFieldContainer content, HibNodeFieldContainer container) {
		return content.compareTo(container);
	}

	@Override
	public List<FieldContainerChange> compareTo(HibNodeFieldContainer content, FieldMap fieldMap) {
		return content.compareTo(fieldMap);
	}

	@Override
	public HibSchemaVersion getSchemaContainerVersion(HibNodeFieldContainer content) {
		return content.getSchemaContainerVersion();
	}

	@Override
	public List<HibMicronodeField> getMicronodeFields(HibNodeFieldContainer content, HibMicroschemaVersion version) {
		return content.getMicronodeFields(version);
	}

	@Override
	public Result<HibMicronodeFieldList> getMicronodeListFields(HibNodeFieldContainer content, HibMicroschemaVersion version) {
		return content.getMicronodeListFields(version);
	}

	@Override
	public String getETag(HibNodeFieldContainer content, InternalActionContext ac) {
		return content.getETag(ac);
	}

	@Override
	public void updateDisplayFieldValue(HibNodeFieldContainer content) {
		content.updateDisplayFieldValue();
	}

	@Override
	public void postfixSegmentFieldValue(HibNodeFieldContainer content) {
		content.postfixSegmentFieldValue();
	}

	@Override
	public Path getPath(HibNodeFieldContainer content, InternalActionContext ac) {
		return content.getPath(ac);
	}

	@Override
	public Iterator<GraphFieldContainerEdge> getContainerEdge(HibNodeFieldContainer content, ContainerType type, String branchUuid) {
		return toGraph(content).getContainerEdge(type, branchUuid);
	}

	@Override
	public HibNodeFieldContainerEdge getConflictingEdgeOfWebrootPath(HibNodeFieldContainer content, String segmentInfo, String branchUuid, ContainerType type, HibNodeFieldContainerEdge edge) {
		return toGraph(content).getConflictingEdgeOfWebrootPath(segmentInfo, branchUuid, type, toGraph(edge));
	}

	@Override
	public HibNodeFieldContainerEdge getConflictingEdgeOfWebrootField(HibNodeFieldContainer content, HibNodeFieldContainerEdge edge, String urlFieldValue, String branchUuid, ContainerType type) {
		return toGraph(content).getConflictingEdgeOfWebrootField(toGraph(edge), urlFieldValue, branchUuid, type);
	}

	@Override
	public boolean isPurgeable(HibNodeFieldContainer content) {
		return content.isPurgeable();
	}

	@Override
	public boolean isAutoPurgeEnabled(HibNodeFieldContainer content) {
		return content.isAutoPurgeEnabled();
	}

	@Override
	public void purge(HibNodeFieldContainer content, BulkActionContext bac) {
		content.purge(bac);
	}

	@Override
	public Result<HibNodeFieldContainer> versions(HibNodeFieldContainer content) {
		return content.versions();
	}

	@Override
	public String getLanguageTag(HibNodeFieldContainer content) {
		return content.getLanguageTag();
	}

	@Override
	public void setLanguageTag(HibNodeFieldContainer content, String languageTag) {
		content.setLanguageTag(languageTag);
	}

	@Override
	public long globalCount() {
		return HibClassConverter.toGraph(db).count(NodeGraphFieldContainer.class);
	}

	/**
	 * This fix will create the missing {@link Node} for {@link NodeGraphFieldContainer}'s which were affected by the deletion bug which was fixed in version
	 * 0.18.3. Due to this bug the {@link Node} was deleted leaving the {@link NodeGraphFieldContainer} dangling in the graph.
	 */
	@Override
	public boolean repair(HibNodeFieldContainer hibContainer) {
		NodeGraphFieldContainer container = toGraph(hibContainer);
		MeshComponent mesh = container.getGraphAttribute(GraphAttribute.MESH_COMPONENT);
		BootstrapInitializer boot = mesh.boot();
		// Pick the first project we find to fetch the initial branchUuid
		HibProject project = boot.projectDao().findAll().iterator().next();
		String branchUuid = project.getInitialBranch().getUuid();

		HibSchemaVersion version = container.getSchemaContainerVersion();
		if (version == null) {
			log.error("Container {" + container.getUuid() + "} has no schema version linked to it.");
			return false;
		}
		HibSchema schemaContainer = version.getSchemaContainer();
		// 1. Find the initial version to check whether the whole version history is still intact
		HibNodeFieldContainer initial = findInitial(container);

		if (initial == null) {
			// The container has no previous version or is not the initial version so we can just delete it.
			container.remove();
			return true;
		}
		HibNodeFieldContainer latest = findLatest(container);
		HibNodeFieldContainer published = null;
		HibNodeFieldContainer draft = null;
		if (latest.getVersion().getFullVersion().endsWith(".0")) {
			published = latest;
		} else {
			draft = latest;
		}

		if (published == null) {
			published = findPublished(latest);
		}
		if (draft == null) {
			draft = findDraft(latest);
		}

		log.info("Initial:" + initial.getUuid() + " version: " + initial.getVersion());
		if (draft != null) {
			log.info("Draft:" + draft.getUuid() + " version: " + draft.getVersion());
		} else {
			throw new RuntimeException("The draft version could not be found");
		}
		if (published != null) {
			log.info("Publish:" + published.getUuid() + " version: " + published.getVersion());
		} else {
			log.info("Published not found");
		}

		log.info("Schema container " + schemaContainer.getName());

		FramedGraph graph = container.getGraph();
		Node node = graph.addFramedVertex(NodeImpl.class);
		node.setProject(project);
		node.setCreated(project.getCreator());

		if (published != null) {
			GraphFieldContainerEdge edge = node.addFramedEdge(HAS_FIELD_CONTAINER, toGraph(published), GraphFieldContainerEdgeImpl.class);
			edge.setLanguageTag(published.getLanguageTag());
			edge.setBranchUuid(branchUuid);
			edge.setType(PUBLISHED);
		}

		GraphFieldContainerEdge edge = node.addFramedEdge(HAS_FIELD_CONTAINER, toGraph(draft), GraphFieldContainerEdgeImpl.class);
		edge.setLanguageTag(draft.getLanguageTag());
		edge.setBranchUuid(branchUuid);
		edge.setType(DRAFT);

		GraphFieldContainerEdge initialEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
		initialEdge.setLanguageTag(initial.getLanguageTag());
		initialEdge.setBranchUuid(branchUuid);
		initialEdge.setType(INITIAL);

		BulkActionContext bac = mesh.bulkProvider().get();
		node.delete(bac);
		return true;
	}

	@Override
	public void migrateContainerOntoBranch(HibNodeFieldContainer hibContainer, HibBranch newBranch, HibNode node, EventQueueBatch batch, ContainerType containerType, boolean setInitial) {
		NodeGraphFieldContainer container = toGraph(hibContainer);
		if (setInitial) {
			setInitial(node, container, newBranch);
		}
		MeshComponent mesh = container.getGraphAttribute(GraphAttribute.MESH_COMPONENT);
		BootstrapInitializer boot = mesh.boot();
		GraphFieldContainerEdgeImpl edge = toGraph(node).addFramedEdge(HAS_FIELD_CONTAINER, toGraph(container), GraphFieldContainerEdgeImpl.class);
		edge.setLanguageTag(container.getLanguageTag());
		edge.setType(containerType);
		edge.setBranchUuid(newBranch.getUuid());
		String value = getSegmentFieldValue(container);
		HibNode parent = boot.nodeDao().getParentNode(node, newBranch.getUuid());
		if (value != null) {
			edge.setSegmentInfo(parent, value);
		} else {
			edge.setSegmentInfo(null);
		}
		edge.setUrlFieldInfo(getUrlFieldValues(container).collect(Collectors.toSet()));
		batch.add(onUpdated(container, newBranch.getUuid(), containerType));
	}

	private HibNodeFieldContainer findDraft(HibNodeFieldContainer latest) {
		HibNodeFieldContainer previous = latest.getPreviousVersion();
		while (previous != null) {
			if (!previous.getVersion().getFullVersion().equalsIgnoreCase(".0")) {
				return previous;
			}
			previous = previous.getPreviousVersion();
		}
		return null;
	}

	/**
	 * Create a new initial edge between node and container for the given branch.
	 */
	private void setInitial(HibNode node, NodeGraphFieldContainer container, HibBranch branch) {
		GraphFieldContainerEdgeImpl initialEdge = toGraph(node).addFramedEdge(HAS_FIELD_CONTAINER, container,
			GraphFieldContainerEdgeImpl.class);
		initialEdge.setLanguageTag(container.getLanguageTag());
		initialEdge.setBranchUuid(branch.getUuid());
		initialEdge.setType(INITIAL);
	}

	/**
	 * Iterate over all versions and try to find the latest published version.
	 * 
	 * @param latest
	 * @return
	 */
	private HibNodeFieldContainer findPublished(HibNodeFieldContainer latest) {
		HibNodeFieldContainer previous = latest.getPreviousVersion();
		while (previous != null) {
			if (previous.getVersion().getFullVersion().equalsIgnoreCase(".0")) {
				return previous;
			}
			previous = previous.getPreviousVersion();
		}
		return null;

	}

	private HibNodeFieldContainer findLatest(HibNodeFieldContainer container) {
		Iterator<HibNodeFieldContainer> it = container.getNextVersions().iterator();
		if (it.hasNext()) {
			HibNodeFieldContainer next = it.next();
			if (it.hasNext()) {
				throw new RuntimeException("The version history has branches. The fix is currently unable to deal with version branches.");
			}
			return findLatest(next);
		} else {
			return container;
		}
	}

	private HibNodeFieldContainer findInitial(HibNodeFieldContainer container) {
		if (container.getVersion().getFullVersion().equalsIgnoreCase("0.1")) {
			return container;
		}
		HibNodeFieldContainer initial = null;
		HibNodeFieldContainer previous = container.getPreviousVersion();
		while (previous != null) {
			initial = previous;
			previous = previous.getPreviousVersion();
		}

		return initial;
	}

	@Override
	public HibNodeFieldContainer createPersisted(String nodeuuid, HibSchemaVersion version, String uuid) {
		NodeGraphFieldContainerImpl container = GraphDBTx.getGraphTx().getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		if (StringUtils.isNotBlank(uuid)) {
			container.setUuid(uuid);
		}
		container.setSchemaContainerVersion(version);
		return container;
	}

	@Override
	@Deprecated
	public HibBooleanField createBoolean(HibNodeFieldContainer container, String name) {
		return new BooleanGraphFieldImpl(name, (NodeGraphFieldContainerImpl) container);
	}

	@Override
	@Deprecated
	public HibStringField createString(HibNodeFieldContainer container, String name) {
		return new StringGraphFieldImpl(name, (NodeGraphFieldContainerImpl) container);
	}

	@Override
	@Deprecated
	public HibNumberField createNumber(HibNodeFieldContainer container, String name) {
		return new NumberGraphFieldImpl(name, (NodeGraphFieldContainerImpl) container);
	}

	@Override
	@Deprecated
	public HibDateField createDate(HibNodeFieldContainer container, String name) {
		return new DateGraphFieldImpl(name, (NodeGraphFieldContainerImpl) container);
	}

	@Override
	@Deprecated
	public HibHtmlField createHtml(HibNodeFieldContainer container, String name) {
		return new HtmlGraphFieldImpl(name, (NodeGraphFieldContainerImpl) container);
	}

	@Override
	@Deprecated
	public HibBinaryField createBinary() {
		return new BinaryGraphFieldImpl();
	}

	@Override
	public Class<? extends HibMicronode> getMicronodePersistenceClass() {
		return MicronodeImpl.class;
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(HibNode node, String languageTag, HibBranch branch,
			ContainerType type) {
		return toGraph(node).getFieldContainer(languageTag, branch, type);
	}

	@Override
	public Result<HibNodeFieldContainer> getFieldContainers(HibNode node, String branchUuid, ContainerType type) {
		return toGraph(node).getFieldContainers(branchUuid, type);
	}

	@Override
	public Result<HibNodeFieldContainer> getFieldContainers(HibNode node, ContainerType type) {
		return toGraph(node).getFieldContainers(type);
	}

	@Override
	public void connectFieldContainer(HibNode node, HibNodeFieldContainer newContainer, HibBranch branch, String languageTag, boolean handleDraftEdge) {
		Node graphNode = toGraph(node);
		NodeGraphFieldContainer graphContainer = toGraph(newContainer);
		String branchUuid = branch.getUuid();

		if (handleDraftEdge) {
			EdgeFrame draftEdge = graphNode.getGraphFieldContainerEdgeFrame(languageTag, branchUuid, DRAFT);
			
			// remove existing draft edge
			if (draftEdge != null) {
				draftEdge.remove();
				updateWebrootPathInfo(newContainer, branchUuid, "node_conflicting_segmentfield_update");
			}

			// create a new draft edge
			GraphFieldContainerEdge edge = graphNode.addFramedEdge(HAS_FIELD_CONTAINER, graphContainer, GraphFieldContainerEdgeImpl.class);
			edge.setLanguageTag(languageTag);
			edge.setBranchUuid(branchUuid);
			edge.setType(DRAFT);
		}

		// if there is no initial edge, create one
		if (graphNode.getGraphFieldContainerEdgeFrame(languageTag, branchUuid, INITIAL) == null) {
			GraphFieldContainerEdge initialEdge = graphNode.addFramedEdge(HAS_FIELD_CONTAINER, graphContainer, GraphFieldContainerEdgeImpl.class);
			initialEdge.setLanguageTag(languageTag);
			initialEdge.setBranchUuid(branchUuid);
			initialEdge.setType(INITIAL);
		}
	}

	@Override
	public Node getParentNode(HibNodeFieldContainer container, String branchUuid) {
		NodeGraphFieldContainer graphContainer = toGraph(container);
		return graphContainer.inE(HAS_FIELD_CONTAINER).has(
			GraphFieldContainerEdgeImpl.BRANCH_UUID_KEY, branchUuid).outV().nextOrDefaultExplicit(NodeImpl.class, null);
	}
}
