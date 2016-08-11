package com.gentics.mesh.core.data.page.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.common.RestModel;

import rx.Observable;
import rx.Single;

/**
 * @see Page
 * @param <T>
 */
public class PageImpl<T extends TransformableElement<? extends RestModel>> implements Iterable<T>, Page {

	private List<? extends T> wrappedList;
	private int totalElements;
	private int numberOfElements;
	private int pageNumber;
	private int totalPages;
	private int perPage;

	public PageImpl(List<? extends T> wrappedList, int totalElements, int pageNumber, int totalPages, int numberOfElements, int perPage) {
		this.wrappedList = wrappedList;
		this.totalElements = totalElements;
		this.pageNumber = pageNumber;
		this.totalPages = totalPages;
		this.numberOfElements = numberOfElements;
		this.perPage = perPage;
	}

	@Override
	public Iterator<T> iterator() {
		return (Iterator<T>) wrappedList.iterator();
	}

	@Override
	public int getSize() {
		return wrappedList.size();
	}

	@Override
	public int getTotalElements() {
		return totalElements;
	}

	@Override
	public int getNumber() {
		return pageNumber;
	}

	@Override
	public int getTotalPages() {
		return totalPages;
	}

	@Override
	public int getNumberOfElements() {
		return numberOfElements;
	}

	@Override
	public long getPerPage() {
		return perPage;
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

		Observable<RestModel> merged = Observable.empty();
		for (Single<? extends RestModel> element : obs) {
			merged = merged.concatWith(element.toObservable());
		}

		return merged.concatMap(item -> {
			listResponse.getData().add(item);
			return Observable.just(listResponse);
		}).last().map(item -> {
			setPaging(listResponse);
			return listResponse;
		}).toSingle();

	}

	@Override
	public void setPaging(ListResponse<?> response) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(getNumber());
		info.setPageCount(getTotalPages());
		info.setPerPage(getPerPage());
		info.setTotalCount(getTotalElements());
	}

	@Override
	public String getETag(InternalActionContext ac) {
		StringBuilder builder = new StringBuilder();
		builder.append(getTotalElements());
		builder.append(getNumber());
		for (T element : this) {
			builder.append("-");
			builder.append(element.getETag(ac));
		}
		return builder.toString();
	}

}
