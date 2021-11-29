package com.gentics.mesh.util;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.event.MeshEventSender;
import com.gentics.mesh.parameter.ParameterProviderContext;
import io.reactivex.Completable;

public class SearchWaitUtilImpl implements SearchWaitUtil {

	private MeshEventSender meshEventSender;
	private MeshOptions options;

	public SearchWaitUtilImpl(MeshEventSender meshEventSender, MeshOptions options) {
		this.meshEventSender = meshEventSender;
		this.options = options;
	}

	/**
	 * Implements the {@link SearchWaitUtil#delayRequested(ParameterProviderContext)} method.
	 *
	 * If no Elasticsearch URL is configured, it'll return false, as there's no elasticsearch to wait for.
	 * Unless the request provides a setting in the query parameter ("wait"), it'll return the global
	 * setting "elasticsearch.waitForIdle".
	 *
 	 * @param ppc The request context
	 *
	 * @return If any upcoming elasticsearch request should be stalled until it's ready.
	 */
	public boolean delayRequested(ParameterProviderContext ppc) {
		// We don't have to wait if no search is configured
		ElasticSearchOptions searchOptions = options.getSearchOptions();
		if (searchOptions == null || searchOptions.getUrl() == null) {
			return false;
		}

		// Try to get the value from the request ("wait"), and otherwise fallback to the global setting.
		return ppc.getSearchParameters().isWait()
				.orElseGet(options.getSearchOptions()::isWaitForIdle);
	}

	/**
	 * Implements the {@link SearchWaitUtil#waitForIdle()} method.
	 *
	 * Connects to the {@link MeshEventSender} and checks for internal mesh events to resolve the completable
	 * once the appropriate event ({@link MeshEvent#SEARCH_IDLE}) has been sent.
	 * Resolves the completable instantly if it isn't waiting for elasticsearch.
	 *
	 * @return A completable which resolves when elasticsearch is idle.
	 */
	public Completable waitForIdle() {
		return meshEventSender.isSearchIdle().flatMapCompletable(isIdle -> {
			if (isIdle) {
				return Completable.complete();
			}
			meshEventSender.flushSearch();
			return meshEventSender.waitForEvent(MeshEvent.SEARCH_IDLE);
		}).andThen(meshEventSender.refreshSearch());
	}
}
