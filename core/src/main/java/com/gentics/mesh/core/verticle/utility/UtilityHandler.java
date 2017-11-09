package com.gentics.mesh.core.verticle.utility;

import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.search.index.microschema.MicroschemaContainerIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandler;

import rx.Single;

/**
 * Handler for utility request methods.
 */
public class UtilityHandler extends AbstractHandler {

	private Database db;

	private WebRootLinkReplacer linkReplacer;

	private NodeIndexHandler nodeIndexHandler;

	private MicroschemaContainerIndexHandler microschemaIndexHandler;

	@Inject
	public UtilityHandler(Database db, WebRootLinkReplacer linkReplacer, NodeIndexHandler nodeIndexHandler,
			MicroschemaContainerIndexHandler microschemaIndexHandler) {
		this.db = db;
		this.linkReplacer = linkReplacer;
		this.nodeIndexHandler = nodeIndexHandler;
		this.microschemaIndexHandler = microschemaIndexHandler;
	}

	/**
	 * Handle a link resolve request.
	 * 
	 * @param rc
	 */
	public void handleResolveLinks(InternalActionContext ac) {
		db.operateTx(() -> {
			String projectName = ac.getParameter("project");
			if (projectName == null) {
				projectName = "project";
			}

			return Single.just(linkReplacer.replace(null, null, ac.getBodyAsString(), ac.getNodeParameters().getResolveLinks(), projectName, ac
					.getNodeParameters().getLanguageList()));
		}).subscribe(body -> ac.send(body, OK, "text/plain"), ac::fail);
	}

	/**
	 * Handle the schema validation request.
	 * 
	 * @param ac
	 */
	public void validateSchema(InternalActionContext ac) {
		db.operateTx(() -> {
			Schema model = JsonUtil.readValue(ac.getBodyAsString(), SchemaModelImpl.class);
			model.validate();
			nodeIndexHandler.validate(model);
			return Single.just(message(ac, "schema_validation_successful"));
		}).subscribe(msg -> ac.send(msg, OK), ac::fail);
	}

	/**
	 * Handle the schema validation request.
	 * 
	 * @param ac
	 */
	public void validateMicroschema(InternalActionContext ac) {
		db.operateTx(() -> {
			Microschema model = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModelImpl.class);
			model.validate();
			return Single.just(message(ac, "microschema_validation_successful"));
		}).subscribe(msg -> ac.send(msg, OK), ac::fail);
	}

}
