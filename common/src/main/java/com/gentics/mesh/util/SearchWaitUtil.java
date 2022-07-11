package com.gentics.mesh.util;

import com.gentics.mesh.parameter.ParameterProviderContext;
import io.reactivex.Completable;

/**
 * Interface to abstract the initial SearchWaitUtil implementation.
 * This Utility interface is used to be able to test the wait option
 * when executing an elasticsearch query.
 */
public interface SearchWaitUtil {

	/**
	 * If the request or a global setting are requesting to wait
	 * for elasticsearch to be done synchronizing/to be stable before accessing it.
	 *
	 * @param ppc The request context
	 *
	 * @return If it should wait for elasticsearch to be stable before accessing it.
	 */
	boolean delayRequested(ParameterProviderContext ppc);

	/**
	 * The actual implementation which waits until elasticsearch is stable/done synchronizing.
	 *
	 * @return A Completable which resolves once elasticsearch is ready.
	 */
	Completable waitForIdle();

	/**
	 * Util function which waits for elasticsearch (using the {@link #waitForIdle()} method), if the {@link #delayRequested(ParameterProviderContext)}
	 * returns `true`. Otherwise returns a Completable which resolves instantly.
	 *
	 * @param ppc The request context
	 *
	 * @return A completable which resolves once you're ready to use elasticsearch calls.
	 */
	default Completable awaitSync(ParameterProviderContext ppc) {
		if (!delayRequested(ppc)) {
			return Completable.complete();
		}

		return waitForIdle();
	}
}
