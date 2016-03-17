package com.gentics.mesh.core.data.impl;

import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.container.impl.AbstractBasicGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.syncleus.ferma.AbstractEdgeFrame;

/**
 * @see GraphFieldContainerEdge
 */
public class GraphFieldContainerEdgeImpl extends AbstractEdgeFrame implements GraphFieldContainerEdge {

	public static final String LANGUAGE_TAG_KEY = "languageTag";

	public static final String RELEASE_UUID_KEY = "releaseUuid";

	public static final String EDGE_TYPE_KEY = "edgeType";

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
	public Type getType() {
		return Type.get(getProperty(EDGE_TYPE_KEY));
	}

	@Override
	public void setType(Type type) {
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
}
