package com.gentics.mesh.core.endpoint.utility;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.i18n.I18NUtil;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.AbstractRestException;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.validation.SchemaValidationResponse;
import com.gentics.mesh.core.rest.validation.ValidationStatus;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.search.index.microschema.MicroschemaContainerIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandler;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.reactivex.Single;

/**
 * Handler for utility request methods.
 */
public class UtilityHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(UtilityHandler.class);

	private LegacyDatabase db;

	private WebRootLinkReplacer linkReplacer;

	private NodeIndexHandler nodeIndexHandler;

	private MicroschemaContainerIndexHandler microschemaIndexHandler;

	@Inject
	public UtilityHandler(LegacyDatabase db, WebRootLinkReplacer linkReplacer, NodeIndexHandler nodeIndexHandler,
			MicroschemaContainerIndexHandler microschemaIndexHandler) {
		this.db = db;
		this.linkReplacer = linkReplacer;
		this.nodeIndexHandler = nodeIndexHandler;
		this.microschemaIndexHandler = microschemaIndexHandler;
	}

	/**
	 * Handle a link resolve request.
	 * 
	 * @param ac
	 */
	public void handleResolveLinks(InternalActionContext ac) {
		db.asyncTx(() -> {
			String projectName = ac.getParameter("project");
			if (projectName == null) {
				projectName = "project";
			}

			return Single.just(linkReplacer.replace(ac, null, null, ac.getBodyAsString(), ac.getNodeParameters().getResolveLinks(), projectName,
					ac.getNodeParameters().getLanguageList()));
		}).subscribe(body -> ac.send(body, OK, "text/plain"), ac::fail);
	}

	/**
	 * Handle the schema validation request.
	 * 
	 * @param ac
	 */
	public void validateSchema(InternalActionContext ac) {
		db.asyncTx(() -> {
			Schema schema = JsonUtil.readValue(ac.getBodyAsString(), SchemaModelImpl.class);
			JsonObject fullSettings = nodeIndexHandler.createIndexSettings(schema);
			SchemaValidationResponse response = new SchemaValidationResponse();
			response.setElasticsearch(fullSettings);
			response.setStatus(ValidationStatus.VALID);
			return nodeIndexHandler.validate(schema).onErrorComplete(error -> {
				log.error("Validation of schema {" + schema.getName() + "} failed with error", error);
				response.setStatus(ValidationStatus.INVALID);

				GenericMessageResponse msg = new GenericMessageResponse();
				if (error instanceof AbstractRestException) {
					AbstractRestException gre = ((AbstractRestException) error);
					String i18nMsg = I18NUtil.get(ac, gre.getI18nKey(), gre.getI18nParameters());
					msg.setInternalMessage(gre.getI18nKey());
					msg.setMessage(i18nMsg);
				} else {
					msg.setInternalMessage(error.getMessage());
					String i18nMsg = I18NUtil.get(ac, "schema_error_index_validation", error.getMessage());
					msg.setMessage(i18nMsg);
				}
				response.setMessage(msg);
				return true;
			}).andThen(Single.just(response));
		}).subscribe(msg -> ac.send(msg, OK), ac::fail);
	}

	/**
	 * Handle the schema validation request.
	 * 
	 * @param ac
	 */
	public void validateMicroschema(InternalActionContext ac) {
		db.asyncTx(() -> {
			Microschema model = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModelImpl.class);
			model.validate();
			SchemaValidationResponse report = new SchemaValidationResponse();
			return Single.just(report);
		}).subscribe(msg -> ac.send(msg, OK), ac::fail);
	}

}
