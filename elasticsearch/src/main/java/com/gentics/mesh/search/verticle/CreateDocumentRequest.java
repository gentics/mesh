package com.gentics.mesh.search.verticle;

import io.reactivex.Completable;
import io.vertx.core.eventbus.Message;

import java.util.Collections;
import java.util.List;

public class CreateDocumentRequest implements Bulkable {

	public static CreateDocumentRequest fromMessage(Message<?> message) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Completable execute() {
		return null;
	}

	@Override
	public List<String> toBulkActions() {
		return Collections.emptyList();
	}
}
