package com.gentics.mesh.plugin;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.impl.HttpUtils;
import io.vertx.core.net.impl.URIDecoder;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.impl.Utils;

/**
 * A REST Plugin is an plugin which will extend the REST API of Gentics Mesh.
 */
public interface RestPlugin extends MeshPlugin {

	/**
	 * Note that this method will be invoked multiple times in order to register the endpoints to all REST verticles.
	 */
	default Router createGlobalRouter() {
		return null;
	}

	/**
	 * Note that this method will be invoked multiple times in order to register the endpoints to all REST verticles.
	 * 
	 * @return
	 */
	default Router createProjectRouter() {
		return null;
	}

	/**
	 * Return a wrapped routing context.
	 * 
	 * @param rc
	 *            Vert.x routing context
	 * @return Wrapped context
	 */
	default PluginContext wrap(RoutingContext rc) {
		return new PluginContext(rc, environment());
	}

	/**
	 * Return a wrapped routing context handler
	 * 
	 * @param handler
	 *            Handler to be wrapped
	 * @return Wrapped handler
	 */
	default Handler<RoutingContext> wrapHandler(Handler<PluginContext> handler) {
		return rc -> handler.handle(wrap(rc));
	}

	/**
	 * Return the API name for the REST plugin. By default the plugin id will be used for the API name.
	 * 
	 * @return name for the api
	 */
	default String restApiName() {
		return id();
	}

	/**
	 * Variant of {@link #addStaticHandlerFromClasspath(Route, String, Handler)} without extra handler
	 * to configure the static handler.
	 * @param route route
	 * @param base base path of the handled resources in the classpath
	 * @return route
	 */
	default Route addStaticHandlerFromClasspath(Route route, String base) {
		return addStaticHandlerFromClasspath(route, base, null);
	}

	/**
	 * Add a static handler for resources loaded from the plugin's classpath.
	 * This will add two handlers, the first will copy the resource from the classpath
	 * into a subdirectory of the plugin's storage directory (if not done before), the second
	 * is a {@link StaticHandler}.
	 * @param route route
	 * @param base base path of the handled resources in the classpath
	 * @param prepareStaticHandler optional handler to prepare (configure) the instance of StaticHandler
	 * @return route
	 */
	default Route addStaticHandlerFromClasspath(Route route, String base, Handler<StaticHandler> prepareStaticHandler) {
		// determine the time, when the handler is created.
		// files, that were last modified before this time, will be overwritten from the classpath
		long handlerCreationTime = System.currentTimeMillis();

		// create and prepare (configure) the static handler to handle files from storage/[base]
		StaticHandler staticHandler = StaticHandler.create(new File(getStorageDir(), base).getPath());
		if (prepareStaticHandler != null) {
			prepareStaticHandler.handle(staticHandler);
		}

		// add a handler, which will copy the files from the classpath into storage/[base] before letting the static
		// handler serve the file
		route.handler(rc ->  {
			FileSystem fs = vertx().fileSystem();
			String uriDecodedPath = URIDecoder.decodeURIComponent(rc.normalisedPath(), false);
			String path = HttpUtils.removeDots(uriDecodedPath.replace('\\', '/'));

			// filePath is something like "webroot/get/the/file.txt" (if get/the/file.txt is requested from the route)
			String filePath = base + Utils.pathOffset(path, rc);
			// storageFile is the file in the storage directory, where the file will be copied to
			File storageFile = new File(getStorageDir(), filePath);
			String storagePath = storageFile.getAbsolutePath();

			// get the url of the requested file from the classpath (will be null, if the resource does not exist)
			URL url = getClass().getClassLoader().getResource(filePath);
			if (url != null) {
				// this is the handler, which will copy the file
				Handler<Promise<Object>> copy = prom -> {
					storageFile.getParentFile().mkdirs();
					try (InputStream in = getClass().getClassLoader().getResourceAsStream(filePath)) {
						Files.copy(in, storageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						prom.complete();
					} catch (Exception e) {
						prom.fail(e);
					}
				};

				// result handler after copying
				Handler<AsyncResult<Object>> result = res -> {
					if (res.failed()) {
						rc.fail(res.cause());
					} else {
						rc.next();
					}
				};

				// check whether the file already exists in the storage dir
				fs.exists(storagePath, handler -> {
					if (handler.succeeded()) {
						if (handler.result()) {
							// file exists, so check, whether it is too old
							fs.props(storagePath, props -> {
								if (props.succeeded()) {
									// if the file is too old, copy it again from the classpath
									if (props.result().lastModifiedTime() < handlerCreationTime) {
										vertx().executeBlocking(copy, result);
									} else {
										rc.next();
									}
								} else {
									rc.fail(props.cause());
								}
							});
						} else {
							// file does not exist, so copy it from the classpath
							vertx().executeBlocking(copy, result);
						}
					} else {
						rc.fail(handler.cause());
					}
				});
			} else {
				// resource does not exist in the classpath, so check whether the file
				// exists in the storage dir
				fs.exists(storagePath, handler -> {
					if (handler.succeeded()) {
						if (handler.result()) {
							// if the file (still) exists in the storage dir, remove it
							fs.delete(storagePath, delHandler -> {
								rc.next();
							});
						} else {
							rc.next();
						}
					} else {
						rc.fail(handler.cause());
					}
				});
			}
		}).handler(staticHandler);

		return route;
	}
}
