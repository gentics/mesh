package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;

import java.util.List;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.AbstractBasicGraphFieldContainerImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.AbstractEdgeFrame;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.annotations.GraphElement;
import com.syncleus.ferma.ext.interopt.EdgeTraversal;

/**
 * @see GraphFieldContainerEdge
 */
@GraphElement
public class GraphFieldContainerEdgeImpl extends AbstractEdgeFrame implements GraphFieldContainerEdge {

	public static void init(Database db) {
		db.addEdgeType(GraphFieldContainerEdgeImpl.class.getSimpleName());
		db.addEdgeType(HAS_FIELD_CONTAINER, GraphFieldContainerEdgeImpl.class);
		db.addCustomEdgeIndex(HAS_FIELD_CONTAINER, "branch_type_lang", "out", GraphFieldContainerEdgeImpl.BRANCH_UUID_KEY,
			GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY);
	}

	/**
	 * Extend the given traversal to filter edges that have one of the given language tags set (if languageTags is not null and not empty)
	 * 
	 * @param traversal
	 * @param languageTags
	 * @return
	 */
	public static EdgeTraversal filterLanguages(
		EdgeTraversal traversal, List<String> languageTags) {
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

	public String getLanguageTag() {
		return getProperty(LANGUAGE_TAG_KEY);
	}

	public void setLanguageTag(String languageTag) {
		setProperty(LANGUAGE_TAG_KEY, languageTag);
	}

	public MeshVertexImpl getStartNode() {
		return traverse(g -> g.inV()).nextOrDefault(MeshVertexImpl.class, null);
	}

	public AbstractBasicGraphFieldContainerImpl getContainer() {
		return traverse(g -> g.outV()).nextOrDefault(AbstractBasicGraphFieldContainerImpl.class, null);
	}

	public NodeGraphFieldContainer getNodeContainer() {
		return traverse(g ->g.inV()).nextOrDefault(NodeGraphFieldContainerImpl.class, null);
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
