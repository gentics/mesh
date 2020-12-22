package com.gentics.mesh.util;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.event.MeshEventSender;
import com.gentics.mesh.parameter.ParameterProviderContext;
import io.reactivex.Completable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Utility to handle the elasticsearch wait
 */
@Singleton
public class SearchWaitUtil {

	@Inject
	public MeshEventSender meshEventSender;

	@Inject
	public AbstractMeshOptions options;

	@Inject
	public SearchWaitUtil() {
	}

	/**
	 * Return the effective wait for idle flag either by the query parameter or by fallback of to mesh search settings.
	 * 
	 * @param ppc
	 * @return
	 */
	public boolean delayRequested(ParameterProviderContext ppc) {
		return ppc.getSearchParameters().isWait()
			.orElseGet(options.getSearchOptions()::isWaitForIdle);
	}

	/**
	 * Wait for the sync idle event of the elasticsearch integration if either the wait flag has been set in the query param or if it is enabled in the config.
	 * 
	 * Please note that the wait happens asynchronous and the callback will occure from within the eventloop thread.
	 * 
	 * @param ppc
	 * @return
	 */
	public Completable awaitSync(ParameterProviderContext ppc) {
		if (!delayRequested(ppc)) {
			return Completable.complete();
		}

		// We don't have to wait if no search is configured
		ElasticSearchOptions searchOptions = options.getSearchOptions();
		if (searchOptions == null || searchOptions.getUrl() == null) {
			return Completable.complete();
		}

		return meshEventSender.isSearchIdle().flatMapCompletable(isIdle -> {
			if (isIdle) {
				return Completable.complete();
			}
			meshEventSender.flushSearch();
			return meshEventSender.waitForEvent(MeshEvent.SEARCH_IDLE);
		}).andThen(meshEventSender.refreshSearch());
	}
}
