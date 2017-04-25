package com.gentics.mesh.core.data.page.impl;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.RestModel;

import rx.Observable;
import rx.Single;

/**
 * @see TransformablePage
 * @param <T>
 *            Type of the page element
 */
public class TransformablePageImpl<T extends TransformableElement<? extends RestModel>> extends PageImpl<T> implements TransformablePage<T> {

	public TransformablePageImpl(List<? extends T> wrappedList, long totalElements, long pageNumber, long totalPages, int numberOfElements,
			int perPage) {
		super(wrappedList, totalElements, pageNumber, totalPages, perPage);
	}

	/**
	 * Transform a page into a transformable page.
	 * 
	 * @param page
	 */
	public TransformablePageImpl(Page<T> page) {
		super(page.getWrappedList(), page.getTotalElements(), page.getNumber(), page.getPageCount(), page.getPerPage());
	}

	@Override
	public Single<? extends ListResponse<RestModel>> transformToRest(InternalActionContext ac, int level) {
		List<Single<? extends RestModel>> obs = new ArrayList<>();
		for (T element : wrappedList) {
			obs.add(element.transformToRest(ac, level));
		}
		ListResponse<RestModel> listResponse = new ListResponse<>();
		if (obs.size() == 0) {
			setPaging(listResponse);
			return Single.just(listResponse);
		}

		return Observable.from(obs).concatMapEager(s -> s.toObservable()).toList().toSingle().map(list -> {
			setPaging(listResponse);
			listResponse.getData().addAll(list);
			return listResponse;
		});
	}

	@Override
	public String getETag(InternalActionContext ac) {
		StringBuilder builder = new StringBuilder();
		builder.append(getTotalElements());
		builder.append(getNumber());
		builder.append(getPerPage());
		for (T element : this) {
			builder.append("-");
			builder.append(element.getETag(ac));
		}
		return builder.toString();
	}

}
