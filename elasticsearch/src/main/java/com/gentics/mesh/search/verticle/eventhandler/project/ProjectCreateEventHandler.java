package com.gentics.mesh.search.verticle.eventhandler.project;

import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.CreateIndexRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.search.index.tag.TagIndexHandlerImpl;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.eventhandler.EventHandler;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;
import io.reactivex.Flowable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;

import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_CREATED;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;

@Singleton
public class ProjectCreateEventHandler implements EventHandler {

	private final MeshHelper helper;
	private final TagIndexHandlerImpl tagIndexHandler;

	@Inject
	public ProjectCreateEventHandler(MeshHelper helper, TagIndexHandlerImpl tagIndexHandler) {
		this.helper = helper;
		this.tagIndexHandler = tagIndexHandler;
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		MeshElementEventModelImpl model = requireType(MeshElementEventModelImpl.class, messageEvent.message);
		IndexInfo indexInfo = tagIndexHandler.getIndex(model.getUuid());

		return Flowable.just(new CreateIndexRequest(indexInfo));
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(PROJECT_CREATED);
	}

}
