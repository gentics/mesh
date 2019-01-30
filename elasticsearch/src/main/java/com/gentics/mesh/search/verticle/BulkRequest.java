package com.gentics.mesh.search.verticle;

import io.reactivex.Completable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BulkRequest implements Bulkable {

	private final String actions;

	public BulkRequest(List<Bulkable> requests) {
		actions = requests.stream()
			.flatMap(bulkable -> bulkable.toBulkActions().stream())
			.collect(Collectors.joining("\n"));
	}

	@Override
	public Completable execute() {
		return null;
	}

	@Override
	public List<String> toBulkActions() {
		return Collections.singletonList(actions);
	}
}
