package com.gentics.mesh.core.endpoint.utility;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.i18n.I18NUtil;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.AbstractRestException;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.validation.SchemaValidationResponse;
import com.gentics.mesh.core.rest.validation.ValidationStatus;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
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

	private final MeshOptions options;

	private final Database db;

	private final WebRootLinkReplacer linkReplacer;

	private final NodeIndexHandler nodeIndexHandler;

	private final MicroschemaContainerIndexHandler microschemaIndexHandler;

	private final HandlerUtilities utils;

	@Inject
	public UtilityHandler(MeshOptions options, Database db, WebRootLinkReplacer linkReplacer, NodeIndexHandler nodeIndexHandler,
		MicroschemaContainerIndexHandler microschemaIndexHandler, HandlerUtilities utils) {
		this.options = options;
		this.db = db;
		this.linkReplacer = linkReplacer;
		this.nodeIndexHandler = nodeIndexHandler;
		this.microschemaIndexHandler = microschemaIndexHandler;
		this.utils = utils;
	}

	/**
	 * Handle a link resolve request.
	 * 
	 * @param ac
	 */
	public void handleResolveLinks(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {

			String projectName = ac.getParameter("project");
			if (projectName == null) {
				projectName = "project";
			}

			return linkReplacer.replace(ac, null, null, ac.getBodyAsString(), ac.getNodeParameters().getResolveLinks(), projectName,
				ac.getNodeParameters().getLanguageList(options));
		}, body -> ac.send(body, OK, "text/plain"));
	}

	/**
	 * Handle the schema validation request.
	 * 
	 * @param ac
	 */
	public void validateSchema(InternalActionContext ac) {
		db.asyncTx(() -> {
			Schema schema;
			try {
				schema = JsonUtil.readValue(ac.getBodyAsString(), SchemaCreateRequest.class);
				schema.validate();
			} catch (GenericRestException error) {
				return Single.just(toValidationResponse(ac, error));
			}
			JsonObject fullSettings = nodeIndexHandler.createIndexSettings(schema);
			SchemaValidationResponse response = new SchemaValidationResponse();
			response.setElasticsearch(fullSettings);
			response.setStatus(ValidationStatus.VALID);
			return nodeIndexHandler.validate(schema)
				.andThen(Single.just(response))
				.onErrorReturn(error -> toValidationResponse(ac, error, fullSettings));
		}).subscribe(msg -> ac.send(msg, OK), ac::fail);
	}

	/**
	 * Handle the schema validation request.
	 * 
	 * @param ac
	 */
	public void validateMicroschema(InternalActionContext ac) {
		db.asyncTx(() -> {
			try {
				Microschema model = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaCreateRequest.class);
				model.validate();
				return Single.just(new SchemaValidationResponse()
					.setStatus(ValidationStatus.VALID));
			} catch (Throwable error) {

				return Single.just(toValidationResponse(ac, error));
			}
		}).subscribe(msg -> ac.send(msg, OK), ac::fail);
	}

	public SchemaValidationResponse toValidationResponse(InternalActionContext ac, Throwable error) {
		return toValidationResponse(ac, error, null);
	}

	public SchemaValidationResponse toValidationResponse(InternalActionContext ac, Throwable error, JsonObject elasticSearchSettings) {
		SchemaValidationResponse response = new SchemaValidationResponse();
		response.setStatus(ValidationStatus.INVALID);
		response.setElasticsearch(elasticSearchSettings);

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
		return response;
	}
}
