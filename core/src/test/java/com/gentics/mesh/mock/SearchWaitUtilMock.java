package com.gentics.mesh.mock;

import com.gentics.mesh.parameter.ParameterProviderContext;
import com.gentics.mesh.util.SearchWaitUtil;
import io.reactivex.Completable;

import java.util.concurrent.TimeUnit;

public class SearchWaitUtilMock implements SearchWaitUtil {

	private boolean shouldWait = false;
	private long timeout = 10_000;

	public SearchWaitUtilMock() {}

	@Override
	public boolean delayRequested(ParameterProviderContext ppc) {
		return shouldWait;
	}

	@Override
	public Completable awaitSync(ParameterProviderContext ppc) {
		return shouldWait ? Completable.timer(this.timeout, TimeUnit.MILLISECONDS) : Completable.complete();
	}

	public boolean isShouldWait() {
		return shouldWait;
	}

	public SearchWaitUtilMock setShouldWait(boolean shouldWait) {
		this.shouldWait = shouldWait;
		return this;
	}

	public long getTimeout() {
		return timeout;
	}

	public SearchWaitUtilMock setTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}
}
