package com.gentics.mesh.plugin.common.util;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
		return webrootMaybe(client, project, path.split("/"));
	}

	/**
	 * Tries to load the given <code>pathSegments</code> via webroot and wraps it in a
	 * {@link Maybe}.
	 *
	 * <p>
	 *     When the webroot response has the status <em>404 Not Found</em>, the
	 *     returned <code>Maybe</code> will be {@link Maybe#empty() empty}.
	 * </p>
	 *
	 * @param client The client to use
	 * @param project The project to load the pathSegments from
	 * @param pathSegments The segments of the path to load
	 * @return A <code>Maybe</code> with the loaded resource if it exists, or
	 * 		an empty <code>Maybe</code> otherwise
	 */
	public static Maybe<NodeResponse> webrootMaybe(MeshRestClient client, String project, String[] pathSegments) {
		return client.webroot(project, pathSegments).toSingle()
			.toMaybe()
			.doAfterSuccess(MeshWebrootResponse::close)
			.onErrorComplete(err -> {
				if (err instanceof MeshRestClientMessageException) {
					MeshRestClientMessageException meshError = (MeshRestClientMessageException) err;

					return meshError.getStatusCode() == HttpResponseStatus.NOT_FOUND.code();
				}

				return false;
			})
			.filter(resp -> !resp.isBinary())
			.map(MeshWebrootResponse::getNodeResponse);
	}

	/**
	 * Encodes the byte chunks of the upstream {@link Flowable} with Base64.
	 *
	 * <p>
	 *     <strong>Important:</strong> This transformer has an internal state,
	 *     and <em>must not</em> be reused.
	 * </p>
	 *
	 * @return A {@code FlowableTransformer} with the upstream data encoded in Base64.
	 */
	public static FlowableTransformer<byte[], byte[]> base64() {
		Encoder encoder = Base64.getEncoder();
		AtomicReference<byte[]> buf = new AtomicReference<>(new byte[0]);

		return upstream -> upstream.flatMap(
			chunk -> {
				byte[] rest = buf.get();
				int restLen = rest.length;
				int total = restLen + chunk.length;
				int overflow = total % 3;
				int toEncodeLen = total - overflow;

				if (toEncodeLen == 0) {
					rest = Arrays.copyOf(rest, total);
					System.arraycopy(chunk, 0, rest, restLen, chunk.length);
					buf.set(rest);

					return Flowable.empty();
				}

				byte[] toEncode = Arrays.copyOf(rest, toEncodeLen);

				System.arraycopy(chunk, 0, toEncode, restLen, toEncodeLen - restLen);
				buf.set(Arrays.copyOfRange(chunk, toEncodeLen - restLen, chunk.length));

				return Flowable.just(encoder.encode(toEncode));
			})
			.concatWith(Single.fromCallable(() -> encoder.encode(buf.get())));
	}


	/**
	 * Makes sure that all but the last byte array the Flowable emits have a maximum size of {@code chunkSize}.
	 *
	 * <p>
	 *     The {@code chunkSize} must be greater than zero, or an {@code IllegalArgumentException} will be thrown.
	 * </p>
	 *
	 * @param chunkSize The desired chunk size (must be greater than zero)
	 * @return A {@link FlowableTransformer} that will resize the byte arrays accordingly
	 * @throws IllegalArgumentException When {@code chunkSize} is zero or less
	 */
	public static FlowableTransformer<byte[], byte[]> split(int chunkSize) {
		if (chunkSize <= 0) {
			throw new IllegalArgumentException("chunkSize must be greater than zero");
		}

		AtomicReference<byte[]> buf = new AtomicReference<>(new byte[0]);

		return upstream -> upstream
			.filter(chunk -> chunk.length > 0)
			.flatMap(chunk -> {
				byte[] availableBytes = ArrayUtils.addAll(buf.get(), chunk);
				List<byte[]> resizedChunks = MiscUtil.splitBytes(availableBytes, chunkSize);
				byte[] lastChunk = resizedChunks.remove(resizedChunks.size() - 1);
				Flowable<byte[]> ret = Flowable.fromIterable(resizedChunks);

				if (lastChunk.length < chunkSize) {
					buf.set(lastChunk);
				} else {
					ret = ret.concatWith(Single.just(lastChunk));
					buf.set(new byte[0]);
				}

				return ret;
			})
			.concatWith(Flowable.defer(() -> {
				byte[] rest = buf.get();

				return rest.length == 0 ? Flowable.empty() : Flowable.just(rest);
			}));
	}
}
