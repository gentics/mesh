package com.gentics.mesh.util;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.MeshEventSender;
import com.gentics.mesh.parameter.ParameterProviderContext;
import io.reactivex.Completable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SearchWaitUtil {

	@Inject
	public MeshEventSender meshEventSender;

	@Inject
	public MeshOptions options;

	@Inject
	public SearchWaitUtil() {
	}


	public boolean delayRequested(ParameterProviderContext ppc) {
		return ppc.getSearchParameters().isWait()
				.orElseGet(options.getSearchOptions()::isWaitForIdle);
	}

	public Completable awaitSync(ParameterProviderContext ppc) {
		if (!delayRequested(ppc)) {
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
