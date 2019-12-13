package com.gentics.mesh.plugin.common.util;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import java.util.function.Supplier;

/**
 * Miscellaneous RxJava utility methods.
 */
public class RxUtil {

	/**
	 * Transforms a {@link Maybe} to a {@link Single}.
	 *
	 * <p>
	 *     If there is no element in the Maybe, the provided single is used
	 *     instead. To be used with {@link Single#to(Function)}.
	 * </p>
	 *
	 * @param defaultSingle This will be used if the upstream Maybe is empty
	 * @return A mapper function transforming a
	 */
	public static <T> Function<Maybe<T>, Single<T>> defaultSingle(Supplier<Single<T>> defaultSingle) {
		return source -> source.flatMap(Maybe::just, Maybe::error, () -> defaultSingle.get().toMaybe()).toSingle();
	}

	/**
	 * Convenience wrapper for
	 * {@link #webrootMaybe(MeshRestClient, String, String)} using the client
	 * and project name from the given {@link PluginContext}.
	 *
	 * @param pc The current plugin context
	 * @param path The path to load via webroot
	 * @return A <code>Maybe</code> with the response for the webroot request
	 */
	public static Maybe<NodeResponse> webrootMaybe(PluginContext pc, String path) {
		return webrootMaybe(pc.client(), pc.projectName(), path);
	}

	/**
	 * Tries to load the given <code>path</code> via webroot and wraps it in a
	 * {@link Maybe}.
	 *
	 * <p>
	 *     When the webroot response has the status <em>404 Not Found</em>, the
	 *     returned <code>Maybe</code> will be {@link Maybe#empty() empty}.
	 * </p>
	 *
	 * @param client The client to use
	 * @param project The project to load the path from
	 * @param path The path to load
	 * @return A <code>Maybe</code> with the loaded resource if it exists, or
	 * 		an empty <code>Maybe</code> otherwise
	 */
	public static Maybe<NodeResponse> webrootMaybe(MeshRestClient client, String project, String path) {
		return client.webroot(project, path).toSingle()
			.toMaybe()
			.onErrorComplete(err -> {
				if (err instanceof MeshRestClientMessageException) {
					MeshRestClientMessageException meshError = (MeshRestClientMessageException) err;

					return meshError.getStatusCode() == HttpResponseStatus.NOT_FOUND.code();
				}

				return false;
			})
			.map(MeshWebrootResponse::getNodeResponse);
	}
}
