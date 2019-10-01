package com.gentics.mesh.plugin.search.util;

import io.reactivex.Single;

import java.util.concurrent.CompletableFuture;

public class RxUtils {

	public static <T> CompletableFuture<T> completableFuture(Single<T> single) {
		CompletableFuture<T> ret = new CompletableFuture<>();

		single.subscribe(ret::complete, ret::completeExceptionally);

		return ret;
	}
}
