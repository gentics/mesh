package com.gentics.mesh.util;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.RestModel;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

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
	public static SingleObserver<RestModel> restModelSender(InternalActionContext rc) {
		return new SingleObserver<RestModel>() {
			@Override
			public void onSubscribe(Disposable d) {
			}

			@Override
			public void onSuccess(RestModel restModel) {
				rc.send(restModel, OK);
			}

			@Override
			public void onError(Throwable e) {
				rc.fail(e);
			}
		};
	}
}
