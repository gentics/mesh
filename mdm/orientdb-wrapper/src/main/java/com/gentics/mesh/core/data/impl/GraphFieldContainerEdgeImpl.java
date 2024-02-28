package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.madl.field.FieldType.LINK;
import static com.gentics.mesh.madl.field.FieldType.STRING;
import static com.gentics.mesh.madl.field.FieldType.STRING_SET;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import java.util.Iterator;
import java.util.List;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.madl.annotations.GraphElement;
import com.gentics.madl.graph.DelegatingFramedMadlGraph;
import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.traversal.EdgeTraversal;
import com.gentics.madl.traversal.VertexTraversal;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.BasicFieldContainer;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.AbstractBasicGraphFieldContainerImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.graph.GraphAttribute;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.dagger.OrientDBMeshComponent;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.gentics.mesh.madl.field.FieldMap;


/**
 * @see GraphFieldContainerEdge
 */
@GraphElement
public class GraphFieldContainerEdgeImpl extends MeshEdgeImpl implements GraphFieldContainerEdge {

	/**
	 * Initialize the edge type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createType(edgeType(GraphFieldContainerEdgeImpl.class.getSimpleName()));
		type.createType(edgeType(HAS_FIELD_CONTAINER).withSuperClazz(GraphFieldContainerEdgeImpl.class));

		FieldMap fields = new FieldMap();
		fields.put("out", LINK);
		fields.put(BRANCH_UUID_KEY, STRING);
		fields.put(EDGE_TYPE_KEY, STRING);
		fields.put(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY, STRING);
		index.addCustomEdgeIndex(HAS_FIELD_CONTAINER, "branch_type_lang", fields, false);

		index.createIndex(edgeIndex(HAS_FIELD_CONTAINER)
			.withPostfix("field")
			.withField("out", LINK)
			.withField(BRANCH_UUID_KEY, STRING)
			.withField(EDGE_TYPE_KEY, STRING));

		// Webroot index:
		fields = new FieldMap();
		fields.put(BRANCH_UUID_KEY, STRING);
		fields.put(EDGE_TYPE_KEY, STRING);
		fields.put(WEBROOT_PROPERTY_KEY, STRING);
		index.addCustomEdgeIndex(HAS_FIELD_CONTAINER, WEBROOT_INDEX_POSTFIX_NAME, fields, true);

		// Webroot url field index:
		fields = new FieldMap();
		fields.put(BRANCH_UUID_KEY, STRING);
		fields.put(EDGE_TYPE_KEY, STRING);
		fields.put(WEBROOT_URLFIELD_PROPERTY_KEY, STRING_SET);
		index.addCustomEdgeIndex(HAS_FIELD_CONTAINER, WEBROOT_URLFIELD_INDEX_POSTFIX_NAME, fields, true);

	}

	/**
	 * Creates the key for the webroot index.
	 *
	 * @param db
	 * @param segmentInfo
	 *            Value of the segment field
	 * @param branchUuid
	 *            Uuid of the branch
	 * @param type
	 *            Type of the container
	 * @return The composed key
	 */
	public static Object composeWebrootIndexKey(GraphDatabase db, String segmentInfo, String branchUuid, ContainerType type) {
		return db.index().createComposedIndexKey(branchUuid, type.getCode(), segmentInfo);
	}

	/**
	 * Generate the composed value for the webroot url field value.
	 * 
	 * Format: branchUuid + contentType + url field path
	 * 
	 * @param db
	 * @param path
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	public static Object composeWebrootUrlFieldIndexKey(GraphDatabase db, String path, String branchUuid, ContainerType type) {
		return db.index().createComposedIndexKey(branchUuid, type.getCode(), path);
	}

	/**
	 * Extend the given traversal to filter edges that have one of the given language tags set (if languageTags is not null and not empty)
	 * 
	 * @param traversal
	 * @param languageTags
	 * @return
	 */
	public static EdgeTraversal<?, ? extends VertexTraversal<?, ? extends Vertex>> filterLanguages(
			EdgeTraversal<?, ? extends VertexTraversal<?, ? extends Vertex>> traversal, List<String> languageTags) {
		if (languageTags != null && languageTags.size() > 0) {
			return traversal.filter(edge -> edge.has(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY, P.within(languageTags)).hasNext());
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
		return outV(AbstractBasicGraphFieldContainerImpl.class).nextOrNull();
	}

	@Override
	public NodeGraphFieldContainer getNodeContainer() {
		return inV(NodeGraphFieldContainerImpl.class).nextOrNull();
	}

	@Override
	public Node getNode() {
		return outV(NodeImpl.class).nextOrNull();
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
	 * Check whether the node has a content (NGFC) for the branch and given type (Draft/Published/Initial).
	 * 
	 * @param nodeId
	 *            Object id of the node
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	public static boolean matchesBranchAndType(Object nodeId, String branchUuid, ContainerType type) {
		DelegatingFramedMadlGraph<? extends Graph> graph = GraphDBTx.getGraphTx().getGraph();
		OrientDBMeshComponent mesh = graph.getAttribute(GraphAttribute.MESH_COMPONENT);
		Iterator<? extends Edge> edges = graph.maybeGetIndexedFramedElements("e." + HAS_FIELD_CONTAINER.toLowerCase() + "_field",
					mesh.database().index().createComposedIndexKey(nodeId, branchUuid, type.getCode()), Edge.class)
				.orElseGet(() -> new DefaultGraphTraversal<>(graph.getRawTraversal())
						.E()
						.has(BRANCH_UUID_KEY, branchUuid)
						.has(EDGE_TYPE_KEY, type.getCode())
						.outV()
						.hasId(nodeId)
						.inE(HAS_FIELD_CONTAINER)
						.has(BRANCH_UUID_KEY, branchUuid)
						.has(EDGE_TYPE_KEY, type.getCode()));
		return edges.hasNext();
	}

	/**
	 * Utilize the graph index and lookup the edge between node and content for the given parameters.
	 * 
	 * @param nodeId
	 * @param branchUuid
	 * @param code
	 * @param lang
	 * @return
	 */
	public static GraphFieldContainerEdge findEdge(Object nodeId, String branchUuid, ContainerType type, String lang) {
		return GraphDBTx.getGraphTx().getGraph().frameElementExplicit(
				MeshEdgeImpl.findEdge(nodeId, lang, branchUuid, type), 
				GraphFieldContainerEdgeImpl.class
			);
	}

	/**
	 * Use the graph index to lookup the edges between the node and the contents for the given parameters. Since the language is not included in the index key
	 * multiple edges may be returned for the edges to the various content languages.
	 * 
	 * @param nodeId
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	public static Result<GraphFieldContainerEdgeImpl> findEdges(Object nodeId, String branchUuid, ContainerType type) {
		DelegatingFramedMadlGraph<? extends Graph> graph = GraphDBTx.getGraphTx().getGraph();
		OrientDBMeshComponent mesh = graph.getAttribute(GraphAttribute.MESH_COMPONENT);
		Iterator<? extends Edge> edges = graph.maybeGetIndexedFramedElements("e." + HAS_FIELD_CONTAINER.toLowerCase() + "_field",
			mesh.database().index().createComposedIndexKey(nodeId, branchUuid, type.getCode()), Edge.class)
				.orElseGet(() -> new DefaultGraphTraversal<>(graph.getRawTraversal())
						.E()
						.has(BRANCH_UUID_KEY, branchUuid)
						.has(EDGE_TYPE_KEY, type.getCode())
						.outV()
						.hasId(nodeId)
						.inE(HAS_FIELD_CONTAINER)
						.has(BRANCH_UUID_KEY, branchUuid)
						.has(EDGE_TYPE_KEY, type.getCode()));
		Iterator<? extends GraphFieldContainerEdgeImpl> frames = graph.frameExplicit(edges, GraphFieldContainerEdgeImpl.class);
		return new TraversalResult<>(frames);
	}

}
