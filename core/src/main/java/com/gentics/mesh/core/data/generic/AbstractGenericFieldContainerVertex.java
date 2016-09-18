package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;

import java.util.Objects;

import com.gentics.mesh.core.data.BasicFieldContainer;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.syncleus.ferma.traversals.EdgeTraversal;

public abstract class AbstractGenericFieldContainerVertex<T extends AbstractResponse, R extends MeshCoreVertex<T, R>>
		extends AbstractMeshCoreVertex<T, R> {

	protected <U extends BasicFieldContainer> U getGraphFieldContainer(Language language, Release release, ContainerType type, Class<U> classOfU) {
		return getGraphFieldContainer(language.getLanguageTag(), release != null ? release.getUuid() : null, type, classOfU);
	}

//	 static Map<String, BasicFieldContainer> map = new HashMap<>();

	/**
	 * Locate the field container using the provided information.
	 * 
	 * @param languageTag
	 *            Language tag of the field container
	 * @param releaseUuid
	 *            Optional release to search within
	 * @param type
	 *            Optional type of the field container (published, draft)
	 * @param classOfU
	 * @return
	 */
	protected <U extends BasicFieldContainer> U getGraphFieldContainer(String languageTag, String releaseUuid, ContainerType type,
			Class<U> classOfU) {
//		Objects.requireNonNull(languageTag);
//		Objects.requireNonNull(classOfU);

		// String key = "l:" + languageTag + "r:" + releaseUuid + "t:" + type + "i:" + getId();
		// System.out.println(key);
		// if(map.containsKey(key)) {
		// return (U) map.get(key);
		// }

		// System.out.println("not found");
		// TODO Add index usage!
		// (nodeId, languageTag)
		EdgeTraversal<?, ?, ?> traversal = outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY, languageTag);

		// + releaseUuid)
		if (releaseUuid != null) {
			traversal = traversal.has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid);
		}
		// + edgetype)
		if (type != null) {
			traversal = traversal.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode());
		}
		U container = traversal.inV().nextOrDefault(classOfU, null);

		// map.put(key, container);

		return container;
	}

	/**
	 * Optionally creates a new field container for the given container type and language.
	 * 
	 * @param language
	 *            Language of the field container
	 * @param classOfU
	 *            Container implementation class to be used for element creation
	 * @return Located field container or created field container
	 */
	protected <U extends BasicFieldContainer> U getOrCreateGraphFieldContainer(Language language, Class<U> classOfU) {

		// Check all existing containers in order to find existing ones
		U container = null;
		EdgeTraversal<?, ?, ?> edgeTraversal = outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY, language.getLanguageTag());
		if (edgeTraversal.hasNext()) {
			container = edgeTraversal.next().inV().has(classOfU).nextOrDefault(classOfU, null);
		}

		// Create a new container if no existing one was found
		if (container == null) {
			container = getGraph().addFramedVertex(classOfU);
			container.setLanguage(language);
			GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, container.getImpl(), GraphFieldContainerEdgeImpl.class);
			edge.setLanguageTag(language.getLanguageTag());
		}

		return container;
	}

}