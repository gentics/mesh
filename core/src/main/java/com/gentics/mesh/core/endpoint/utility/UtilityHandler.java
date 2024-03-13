package com.gentics.mesh.core.endpoint.utility;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.i18n.I18NUtil;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.link.WebRootLinkReplacerImpl;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.AbstractRestException;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.validation.SchemaValidationResponse;
import com.gentics.mesh.core.rest.validation.ValidationStatus;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.search.index.node.NodeIndexHandlerImpl;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler for utility request methods.
 */
public class UtilityHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(UtilityHandler.class);

	private final MeshOptions options;

	private final Database db;

	private final WebRootLinkReplacerImpl linkReplacer;

	private final NodeIndexHandlerImpl nodeIndexHandler;

	private final HandlerUtilities utils;

	@Inject
	public UtilityHandler(MeshOptions options, Database db, WebRootLinkReplacerImpl linkReplacer, NodeIndexHandlerImpl nodeIndexHandler,
		HandlerUtilities utils) {
		this.options = options;
		this.db = db;
		this.linkReplacer = linkReplacer;
		this.nodeIndexHandler = nodeIndexHandler;
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
			SchemaModel schema = JsonUtil.readValue(ac.getBodyAsString(), SchemaModelImpl.class);
			SchemaValidationResponse response = new SchemaValidationResponse();
			response.setStatus(ValidationStatus.VALID);
			nodeIndexHandler.createIndexSettings(schema).ifPresentOrElse(fullSettings -> {
				response.setElasticsearch(fullSettings);
				Map<String, JsonObject> languageElasticsearch = schema.findOverriddenSearchLanguages().collect(Collectors.toMap(
					Function.identity(),
					lang -> nodeIndexHandler.createIndexSettings(schema, lang).orElseGet(() -> new JsonObject())
				));
				if (!languageElasticsearch.isEmpty()) {
					response.setLanguageElasticsearch(languageElasticsearch);
				}
				nodeIndexHandler.validate(schema).onErrorComplete(error -> {
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
				});
			}, () -> {
				log.info("Validation of schema {" + schema.getName() + "} skipped, as being excluded from indexing");
				response.setMessage(new GenericMessageResponse(I18NUtil.get(ac, "schema_error_index_disabled", schema.getName()), "schema_error_index_disabled"));
			});
			return Single.just(response);
		}).subscribe(msg -> ac.send(msg, OK), ac::fail);
	}

	/**
	 * Handle the schema validation request.
	 * 
	 * @param ac
	 */
	public void validateMicroschema(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			MicroschemaModel model = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModelImpl.class);
			model.validate();
			return new SchemaValidationResponse();
		}, msg -> ac.send(msg, OK));
	}

}
