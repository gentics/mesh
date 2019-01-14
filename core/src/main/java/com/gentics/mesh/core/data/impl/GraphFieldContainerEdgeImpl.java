package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.graphdb.spi.FieldType.LINK;
import static com.gentics.mesh.graphdb.spi.FieldType.STRING;
import static com.gentics.mesh.graphdb.spi.FieldType.STRING_SET;

import java.util.List;

import com.gentics.mesh.core.data.BasicFieldContainer;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.AbstractBasicGraphFieldContainerImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.FieldMap;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.annotations.GraphElement;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.Traversal;
import com.syncleus.ferma.traversals.TraversalFunction;
import com.syncleus.ferma.traversals.VertexTraversal;

/**
 * @see GraphFieldContainerEdge
 */
@GraphElement
public class GraphFieldContainerEdgeImpl extends MeshEdgeImpl implements GraphFieldContainerEdge {

	public static void init(Database db) {
		db.addEdgeType(GraphFieldContainerEdgeImpl.class.getSimpleName());
		db.addEdgeType(HAS_FIELD_CONTAINER, GraphFieldContainerEdgeImpl.class);

		FieldMap fields = new FieldMap();
		fields.put("out", LINK);
		fields.put(BRANCH_UUID_KEY, STRING);
		fields.put(EDGE_TYPE_KEY, STRING);
		fields.put(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY, STRING);
		db.addCustomEdgeIndex(HAS_FIELD_CONTAINER, "branch_type_lang", fields, false);

		// Webroot index:
		fields = new FieldMap();
		fields.put(BRANCH_UUID_KEY, STRING);
		fields.put(EDGE_TYPE_KEY, STRING);
		fields.put(WEBROOT_PROPERTY_KEY, STRING);
		db.addCustomEdgeIndex(HAS_FIELD_CONTAINER, WEBROOT_INDEX_POSTFIX_NAME, fields, true);

		// Webroot url field index:
		fields = new FieldMap();
		fields.put(BRANCH_UUID_KEY, STRING);
		fields.put(EDGE_TYPE_KEY, STRING);
		fields.put(WEBROOT_URLFIELD_PROPERTY_KEY, STRING_SET);
		db.addCustomEdgeIndex(HAS_FIELD_CONTAINER, WEBROOT_URLFIELD_INDEX_POSTFIX_NAME, fields, true);

	}

	public void setSegmentInfo(Node parentNode, String segment) {
		setSegmentInfo(composeSegmentInfo(parentNode, segment));
	}

	/**
	 * Creates the key for the webroot index.
	 *
	 * @param segmentInfo
	 *            Value of the segment field
	 * @param branchUuid
	 *            Uuid of the branch
	 * @param type
	 *            Type of the container
	 * @return The composed key
	 */
	public static Object composeWebrootIndexKey(String segmentInfo, String branchUuid, ContainerType type) {
		Database db = MeshInternal.get().database();
		return db.createComposedIndexKey(branchUuid, type.getCode(), segmentInfo);
	}

	public static String composeSegmentInfo(Node parentNode, String segment) {
		return parentNode == null ? "" : parentNode.getUuid() + segment;
	}

	public static Object composeWebrootUrlFieldIndexKey(String path, String branchUuid, ContainerType type) {
		Database db = MeshInternal.get().database();
		return db.createComposedIndexKey(branchUuid, type.getCode(), path);
	}

	/**
	 * Extend the given traversal to filter edges that have one of the given language tags set (if languageTags is not null and not empty)
	 * 
	 * @param traversal
	 * @param languageTags
	 * @return
	 */
	public static EdgeTraversal<?, ?, ? extends VertexTraversal<?, ?, ?>> filterLanguages(
		EdgeTraversal<?, ?, ? extends VertexTraversal<?, ?, ?>> traversal, List<String> languageTags) {
		if (languageTags != null && languageTags.size() > 0) {
			LanguageRestrictionFunction[] pipes = new LanguageRestrictionFunction[languageTags.size()];
			for (int i = 0; i < languageTags.size(); i++) {
				pipes[i] = new LanguageRestrictionFunction(languageTags.get(0));
			}
			return traversal.or(pipes);
		} else {
			return traversal;
		}
	}

	@Override
	public String getLanguageTag() {
		return property(LANGUAGE_TAG_KEY);
	}

	@Override
	public void setLanguageTag(String languageTag) {
		property(LANGUAGE_TAG_KEY, languageTag);
	}

	@Override
	public BasicFieldContainer getContainer() {
		return outV().nextOrDefaultExplicit(AbstractBasicGraphFieldContainerImpl.class, null);
	}

	@Override
	public NodeGraphFieldContainer getNodeContainer() {
		return inV().nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class, null);
	}

	@Override
	public Node getNode() {
		return outV().nextOrDefaultExplicit(NodeImpl.class, null);
	}

	@Override
	public ContainerType getType() {
		return ContainerType.get(property(EDGE_TYPE_KEY));
	}

	@Override
	public void setType(ContainerType type) {
		if (type == null) {
			property(EDGE_TYPE_KEY, null);
		} else {
			property(EDGE_TYPE_KEY, type.getCode());
		}
	}

	@Override
	public String getBranchUuid() {
		return property(BRANCH_UUID_KEY);
	}

	@Override
	public void setBranchUuid(String uuid) {
		property(BRANCH_UUID_KEY, uuid);
	}

	/**
	 * Traversal function that restricts by given language tag
	 */
	protected static class LanguageRestrictionFunction implements TraversalFunction<EdgeFrame, Traversal<?, ?, ?, ?>> {
		protected String languageTag;

		public LanguageRestrictionFunction(String languageTag) {
			this.languageTag = languageTag;
		}

		@Override
		public Traversal<?, ?, ?, ?> compute(EdgeFrame argument) {
			return argument.traversal().has(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY, languageTag);
		}
	}

}
