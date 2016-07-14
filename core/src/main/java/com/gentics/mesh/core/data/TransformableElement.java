package com.gentics.mesh.core.data;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.Database;

import rx.Single;

/**
 * A {@link TransformableElement} is a node that can be transformed into a rest model response.
 *
 * @param <T>
 *            RestModel response class
 */
public interface TransformableElement<T extends RestModel> extends MeshElement {

	/**
	 * Transform the node into the matching rest model response asynchronously
	 * 
	 * @param ac
	 *            Context of the calling action
	 * @param level
	 *            Level of transformation
	 * @param languageTags
	 *            optional list of language tags to be used for language fallback
	 */
	default Single<T> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		Database db = MeshSpringConfiguration.getInstance().database();
		return db.asyncNoTrxExperimental(() -> {
			return transformToRestSync(ac, level, languageTags);
		});
	}

	/**
	 * Transform the node into the matching rest model response synchronously
	 *
	 * @param ac
	 *            Context of the calling action
	 * @param level
	 *            Level of transformation
	 * @param languageTags
	 *            optional list of language tags to be used for language fallback
	 * @return
	 */
	Single<T> transformToRestSync(InternalActionContext ac, int level, String... languageTags);
}
