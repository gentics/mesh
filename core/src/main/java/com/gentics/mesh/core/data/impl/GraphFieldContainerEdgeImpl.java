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
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.FieldMap;
import com.syncleus.ferma.AbstractEdgeFrame;
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
public class GraphFieldContainerEdgeImpl extends AbstractEdgeFrame implements GraphFieldContainerEdge {

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
		db.addCustomEdgeIndex(HAS_FIELD_CONTAINER, WEBROOT_INDEX_NAME, FieldMap.create(WEBROOT_PROPERTY_KEY, STRING), true);
		db.addCustomEdgeIndex(HAS_FIELD_CONTAINER, PUBLISHED_WEBROOT_INDEX_NAME, FieldMap.create(PUBLISHED_WEBROOT_PROPERTY_KEY, STRING), true);

		// Webroot url field index:
		db.addCustomEdgeIndex(HAS_FIELD_CONTAINER, WEBROOT_URLFIELD_INDEX_NAME, FieldMap.create(WEBROOT_URLFIELD_PROPERTY_KEY, STRING_SET), true);
		db.addCustomEdgeIndex(HAS_FIELD_CONTAINER, PUBLISHED_WEBROOT_URLFIELD_INDEX_NAME,FieldMap.create(PUBLISHED_WEBROOT_URLFIELD_PROPERTY_KEY, STRING_SET), true);

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
		return getProperty(LANGUAGE_TAG_KEY);
	}

	@Override
	public void setLanguageTag(String languageTag) {
		setProperty(LANGUAGE_TAG_KEY, languageTag);
	}

	public MeshVertexImpl getStartNode() {
		return inV().nextOrDefault(MeshVertexImpl.class, null);
	}

	@Override
	public BasicFieldContainer getContainer() {
		return outV().nextOrDefault(AbstractBasicGraphFieldContainerImpl.class, null);
	}

	@Override
	public NodeGraphFieldContainer getNodeContainer() {
		return inV().nextOrDefault(NodeGraphFieldContainerImpl.class, null);
	}

	@Override
	public Node getNode() {
		return outV().nextOrDefault(NodeImpl.class, null);
	}

	@Override
	public ContainerType getType() {
		return ContainerType.get(getProperty(EDGE_TYPE_KEY));
	}

	@Override
	public void setType(ContainerType type) {
		if (type == null) {
			setProperty(EDGE_TYPE_KEY, null);
		} else {
			setProperty(EDGE_TYPE_KEY, type.getCode());
		}
	}

	@Override
	public String getBranchUuid() {
		return getProperty(BRANCH_UUID_KEY);
	}

	@Override
	public void setBranchUuid(String uuid) {
		setProperty(BRANCH_UUID_KEY, uuid);
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
