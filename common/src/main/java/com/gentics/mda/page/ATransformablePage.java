package com.gentics.mda.page;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mda.entity.ATransformableElement;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.util.ETag;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface ATransformablePage<T extends ATransformableElement<? extends RestModel>> extends Page<T> {

	/**
	 * Transform the page into a list response.
	 *
	 * @param ac
	 * @param level
	 *            Level of transformation
	 */
	default Single<? extends ListResponse<RestModel>> transformToRest(InternalActionContext ac, int level) {
		List<Single<? extends RestModel>> obs = new ArrayList<>();
		for (T element : getWrappedList()) {
			obs.add(element.transformToRest(ac, level));
		}
		ListResponse<RestModel> listResponse = new ListResponse<>();
		if (obs.size() == 0) {
			setPaging(listResponse);
			return Single.just(listResponse);
		}

		return Observable.fromIterable(obs).concatMapEager(s -> s.toObservable()).toList().map(list -> {
			setPaging(listResponse);
			listResponse.getData().addAll(list);
			return listResponse;
		});
	}

	default ListResponse<RestModel> transformToRestSync(InternalActionContext ac, int level) {
		List<RestModel> responses = new ArrayList<>();
		for (T element : getWrappedList()) {
			responses.add(element.transformToRestSync(ac, level));
		}
		ListResponse<RestModel> listResponse = new ListResponse<>();
		setPaging(listResponse);
		listResponse.getData().addAll(responses);
		return listResponse;
	}

	/**
	 * Return the eTag of the page. The etag is calculated using the following information:
	 * <ul>
	 * <li>Number of total elements (all pages)</li>
	 * <li>All etags for all found elements</li>
	 * <li>Number of the current page</li>
	 * </ul>
	 *
	 * @param ac
	 * @return
	 */
	default String getETag(InternalActionContext ac) {
		StringBuilder builder = new StringBuilder();
		builder.append(getTotalElements());
		builder.append(getNumber());
		builder.append(getPerPage());
		for (T element : this) {
			builder.append("-");
			builder.append(element.getETag(ac));
		}
		return ETag.hash(builder.toString());
	}
}
