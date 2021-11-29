package com.gentics.mesh.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.event.MeshEventSender;
import com.gentics.mesh.metric.SearchRequestMetric;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.parameter.ParameterProviderContext;

import io.micrometer.core.instrument.Timer;
import io.reactivex.Completable;

public class SearchWaitUtilImpl implements SearchWaitUtil {

	private MeshEventSender meshEventSender;
	private MeshOptions options;

	private AtomicLong waitingGauge;

	private Timer waitingTimer;

	public SearchWaitUtilImpl(MeshEventSender meshEventSender, MeshOptions options, MetricsService metrics) {
		this.meshEventSender = meshEventSender;
		this.options = options;
		if (metrics != null && metrics.isEnabled()) {
			waitingGauge = metrics.longGauge(SearchRequestMetric.WAITING);
			waitingTimer = metrics.timer(SearchRequestMetric.WAITING_TIME);
		}
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

	@Override
	public long waitTimeoutMs() {
		ElasticSearchOptions searchOptions = options.getSearchOptions();
		if (searchOptions == null) {
			return ElasticSearchOptions.DEFAULT_WAIT_FOR_IDLE_TIMEOUT;
		}
		return searchOptions.getWaitForIdleTimeout();
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
		AtomicReference<Timer.Sample> sample = new AtomicReference<>();
		return meshEventSender.isSearchIdle().flatMapCompletable(isIdle -> {
			if (isIdle) {
				return Completable.complete();
			}
			meshEventSender.flushSearch();
			return meshEventSender.waitForEvent(MeshEvent.SEARCH_IDLE);
		}).andThen(meshEventSender.refreshSearch())
		.doOnSubscribe(ignore -> {
			// another request is waiting for the search becoming idle
			if (waitingTimer != null) {
				sample.set(Timer.start());
			}
			if (waitingGauge != null) {
				waitingGauge.incrementAndGet();
			}
		})
		.doOnDispose(() -> {
			// waiting timed out before search became idle, so the request is not waiting any more
			if (waitingGauge != null) {
				waitingGauge.decrementAndGet();
			}
			if (waitingTimer != null) {
				sample.get().stop(waitingTimer);
			}
		})
		.doOnTerminate(() -> {
			// search is idle now, so the request is not waiting any more
			if (waitingGauge != null) {
				waitingGauge.decrementAndGet();
			}
			if (waitingTimer != null) {
				sample.get().stop(waitingTimer);
			}
		});
	}
}
