package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

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
	 * @param languageTags
	 *            optional list of language tags to be used for language fallback
	 */
	default Observable<T> transformToRest(InternalActionContext ac, String... languageTags) {
		Database db = MeshSpringConfiguration.getInstance().database();
		return db.asyncNoTrxExperimental(() -> {
			return transformToRestSync(ac, languageTags);
		});
	}

	/**
	 * Transform the node into the matching rest model response synchronously
	 *
	 * @param ac
	 *            Context of the calling action
	 * @param languageTags
	 *            optional list of language tags to be used for language fallback
	 * @return
	 */
	Observable<T> transformToRestSync(InternalActionContext ac, String... languageTags);
}
