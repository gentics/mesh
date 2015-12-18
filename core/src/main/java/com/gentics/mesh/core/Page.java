package com.gentics.mesh.core;

import static com.gentics.mesh.json.JsonUtil.toJson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.handler.InternalActionContext;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

public class Page<T extends TransformableElement<? extends RestModel>> implements Iterable<T> {

	private List<? extends T> wrappedList;
	private int totalElements;
	private int numberOfElements;
	private int pageNumber;
	private int totalPages;
	private int perPage;

	public Page(List<? extends T> wrappedList, int totalElements, int pageNumber, int totalPages, int numberOfElements, int perPage) {
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

	public int getSize() {
		return wrappedList.size();
	}

	public int getTotalElements() {
		return totalElements;
	}

	public int getNumber() {
		return pageNumber;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public int getNumberOfElements() {
		return numberOfElements;
	}

	public long getPerPage() {
		return perPage;
	}

	/**
	 * Transform the page into a list response.
	 * 
	 * @param ac
	 */
	public Observable<? extends ListResponse<RestModel>> transformToRest(InternalActionContext ac) {
		List<Observable<? extends RestModel>> obs = new ArrayList<>();

		for (T element : wrappedList) {
			ObservableFuture<RestModel> obsF = RxHelper.observableFuture();
			obs.add(obsF);
			element.transformToRest(ac, rh -> {
				if (rh.failed()) {
					obsF.toHandler().handle(Future.failedFuture(rh.cause()));
				} else {
					obsF.toHandler().handle(Future.succeededFuture(rh.result()));
				}
			});
		}

		ListResponse<RestModel> listResponse = new ListResponse<>();
		// Wait for all async processes to complete
		return Observable.merge(obs).collect(() -> {
			setPaging(listResponse);
			return listResponse.getData();
		} , (x, y) -> {
			x.add(y);
		}).map(item -> {
			return listResponse;
		});
	}

	// /**
	// * Transform the page into a list response.
	// *
	// * @param ac
	// * @param page
	// * @param handler
	// * @param listResponse
	// */
	// public <TR extends RestModel, RL extends ListResponse<TR>> void transformToRest(InternalActionContext ac, Handler<AsyncResult<ListResponse<TR>>> handler)
	// {
	// List<ObservableFuture<TR>> futures = new ArrayList<>();
	//
	// ListResponse<RestModel> listResponse = new ListResponse<>();
	//
	// for (T node : wrappedList) {
	// ObservableFuture<TR> obs = RxHelper.observableFuture();
	// futures.add(obs);
	// node.transformToRest(ac, obs.toHandler());
	// }
	//
	// // Wait for all async processes to complete
	// Observable.merge(futures).collect(() -> {
	// return listResponse.getData();
	// } , (x, y) -> {
	// x.add(y);
	// }).subscribe(list -> {
	// setPaging(listResponse);
	// handler.handle(Future.succeededFuture(listResponse));
	// } , error -> {
	// handler.handle(Future.failedFuture(error));
	// });
	// }

	/**
	 * Set the paging parameters into the given list response by examining the given page.
	 * 
	 * @param response
	 *            List response that will be updated
	 * @param page
	 *            Page that will be used to extract the paging parameters
	 */
	public void setPaging(ListResponse<?> response) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(getNumber());
		info.setPageCount(getTotalPages());
		info.setPerPage(getPerPage());
		info.setTotalCount(getTotalElements());
	}
	
	
	/**
	 * Transform the given page to a rest page and send it to the client.
	 * 
	 * @param ac
	 * @param page
	 * @param listResponse
	 * @param status
	 */
	public <T extends MeshCoreVertex<TR, T>, TR extends RestModel, RL extends ListResponse<TR>> void transformAndRespond(
			InternalActionContext ac, HttpResponseStatus status) {
		transformToRest(ac).subscribe(list -> {
			ac.send(toJson(list), status);
		} , error -> {
			ac.fail(error);
		});
	}
}
