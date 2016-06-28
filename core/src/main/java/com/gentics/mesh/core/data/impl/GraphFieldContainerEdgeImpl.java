package com.gentics.mesh.core.data.impl;

import java.util.List;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.container.impl.AbstractBasicGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.syncleus.ferma.AbstractEdgeFrame;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.Traversal;
import com.syncleus.ferma.traversals.TraversalFunction;
import com.syncleus.ferma.traversals.VertexTraversal;

/**
 * @see GraphFieldContainerEdge
 */
public class GraphFieldContainerEdgeImpl extends AbstractEdgeFrame implements GraphFieldContainerEdge {

	public static final String LANGUAGE_TAG_KEY = "languageTag";

	public static final String RELEASE_UUID_KEY = "releaseUuid";

	public static final String EDGE_TYPE_KEY = "edgeType";

	/**
	 * Extend the given traversal to filter edges that have one of the given
	 * language tags set (if languageTags is not null and not empty)
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

	public String getLanguageTag() {
		return getProperty(LANGUAGE_TAG_KEY);
	}

	public void setLanguageTag(String languageTag) {
		setProperty(LANGUAGE_TAG_KEY, languageTag);
	}

	public MeshVertexImpl getStartNode() {
		return inV().nextOrDefault(MeshVertexImpl.class, null);
	}

	public AbstractBasicGraphFieldContainerImpl getI18NProperties() {
		return outV().nextOrDefault(AbstractBasicGraphFieldContainerImpl.class, null);
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
	public String getReleaseUuid() {
		return getProperty(RELEASE_UUID_KEY);
	}

	@Override
	public void setReleaseUuid(String uuid) {
		setProperty(RELEASE_UUID_KEY, uuid);
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
