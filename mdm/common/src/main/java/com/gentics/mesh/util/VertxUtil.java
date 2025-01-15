package com.gentics.mesh.util;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.RestModel;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;

/**
 * Various utility functions regarding Vert.x
 */
public final class VertxUtil {
	private VertxUtil() {
	}

	/**
	 * Sends a {@link RestModel} to the client. Propagates any error to the failure handler.
	 *
	 * Usage: <code>.subscribe(restModelSender(ac))</code>
	 *
	 * @param rc
	 * @return
	 */
	public static final SingleObserver<RestModel> restModelSender(InternalActionContext rc) {
		return restModelSender(rc, OK);
	}

	/**
	 * Convert a {@link Runnable} into {@link Action};
	 * 
	 * @param r
	 * @return
	 */
	public static final Action intoAction(Runnable r) {
		return () -> r.run();
	}

	/**
	 * Convert a {@link Action} into {@link Runnable}, wrapping its exception with {@link IllegalStateException}
	 * 
	 * @param r
	 * @return
	 */
	public static final Runnable intoRunnable(Action r) {
		return () -> {
			 try {
				r.run();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		};
	}

	/**
	 * Sends a {@link RestModel} to the client. Propagates any error to the failure handler.
	 *
	 * Usage: <code>.subscribe(restModelSender(ac, OK))</code>
	 *
	 * @param rc
	 * @param statusCode
	 * @return
	 */
	public static final SingleObserver<RestModel> restModelSender(InternalActionContext rc, HttpResponseStatus statusCode) {
		return new SingleObserver<RestModel>() {
			@Override
			public void onSubscribe(Disposable d) {
			}

			@Override
			public void onSuccess(RestModel restModel) {
				rc.send(restModel, statusCode);
			}

			@Override
			public void onError(Throwable e) {
				rc.fail(e);
			}
		};
	}
}
