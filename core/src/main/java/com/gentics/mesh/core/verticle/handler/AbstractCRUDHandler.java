package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.responde;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.NamedNode;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.util.BlueprintTransaction;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

public abstract class AbstractCRUDHandler {

	@Autowired
	protected I18NService i18n;

	@Autowired
	protected RouterStorage routerStorage;

	@Autowired
	protected BootstrapInitializer boot;

	@Autowired
	protected FramedThreadedTransactionalGraph fg;

	protected SearchQueue searchQueue;

	protected MeshRoot meshRoot;

	protected Vertx vertx = Mesh.vertx();

	@PostConstruct
	public void setup() {
		searchQueue = boot.meshRoot().getSearchQueue();
		meshRoot = boot.meshRoot();
	}

	protected Project getProject(RoutingContext rc) {
		return boot.projectRoot().findByName(getProjectName(rc));
	}

	protected String getProjectName(RoutingContext rc) {
		return rc.get(RouterStorage.PROJECT_CONTEXT_KEY);
	}

	abstract public void handleCreate(RoutingContext rc);

	abstract public void handleDelete(RoutingContext rc);

	abstract public void handleUpdate(RoutingContext rc);

	abstract public void handleRead(RoutingContext rc);

	abstract public void handleReadList(RoutingContext rc);

	public <T extends GenericVertex<? extends RestModel>> void delete(RoutingContext rc, String uuidParameterName, String i18nMessageKey,
			RootVertex<T> root) {
		I18NService i18n = I18NService.getI18n();

		loadObject(
				rc,
				uuidParameterName,
				DELETE_PERM,
				root,
				rh -> {
					if (hasSucceeded(rc, rh)) {
						GenericVertex<?> vertex = rh.result();
						String uuid = vertex.getUuid();
						String name = null;
						if (vertex instanceof NamedNode) {
							name = ((NamedNode) vertex).getName();
						}
						FramedThreadedTransactionalGraph fg = MeshSpringConfiguration.getMeshSpringConfiguration()
								.framedThreadedTransactionalGraph();
						try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
							vertex.delete();
							BootstrapInitializer.getBoot().meshRoot().getSearchQueue()
									.put(vertex.getUuid(), vertex.getType(), SearchQueueEntryAction.DELETE_ACTION);
							Mesh.vertx().eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
							tx.success();
						}
						String id = name != null ? uuid + "/" + name : uuid;
						responde(rc, toJson(new GenericMessageResponse(i18n.get(rc, i18nMessageKey, id))));
					}
				});
	}

}
