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

	public boolean delayRequested(ParameterProviderContext ppc) {
		return ppc.getSearchParameters().isWait()
				.orElseGet(options.getSearchOptions()::isWaitForIdle);
	}

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
