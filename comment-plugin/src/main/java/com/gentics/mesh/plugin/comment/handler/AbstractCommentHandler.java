package com.gentics.mesh.plugin.comment.handler;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.comment.error.CommentPluginError;
import com.gentics.mesh.plugin.comment.model.JsonSerializable;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public abstract class AbstractCommentHandler {

	private static final Logger log = LoggerFactory.getLogger(AbstractCommentHandler.class);

	protected BiConsumer<? super JsonSerializable, ? super Throwable> jsonHandler(RoutingContext event) {
		return (delegate, error) -> {
			if (delegate != null) {
				event.response().end(delegate.toJson());
			} else {
				handleError(event, error);
			}
		};
	}

	protected BiConsumer<? super JsonObject, ? super Throwable> jsonObjectHandler(RoutingContext event) {
		return (json, error) -> {
			if (json != null) {
				event.response().end(json.encodePrettily());
			} else {
				handleError(event, error);
			}
		};
	}

	// TODO refactor so that other handlers use this
	protected BiConsumer<? super String, ? super Throwable> stringHandler(RoutingContext event) {
		return (output, error) -> {
			if (output != null) {
				event.response().end(output);
			} else {
				handleError(event, error);
			}
		};
	}

	protected Consumer<? super Throwable> emptyResponseHandler(RoutingContext event) {
		return error -> {
			if (error == null) {
				event.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code());
				event.response().end();
			} else {
				handleError(event, error);
			}
		};
	}


	private void handleError(RoutingContext context, Throwable e) {
		if (e instanceof CommentPluginError) {
			CommentPluginError error = (CommentPluginError) e;
			if (error.isServerLog()) {
				log.error("Error in handler", error);
			}
			context.response().setStatusCode(error.getHttpStatusCode());
			context.response().end(error.getMessage()); // TODO i18n?
		} else {
			log.error("Unexpected error in handler", e);
			context.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
			context.response().end(e.getMessage());
		}
	}

	public abstract Completable handle(PluginContext context);
}
