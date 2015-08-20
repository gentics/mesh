package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.schema.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaUpdateRequest;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.ext.web.RoutingContext;

@Component
public class SchemaContainerCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {

			MeshAuthUser requestUser = getUser(rc);

			SchemaCreateRequest schema;
			try {
				schema = JsonUtil.readSchema(rc.getBodyAsString(), SchemaCreateRequest.class);
				if (StringUtils.isEmpty(schema.getName())) {
					rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "schema_missing_name")));
					return;
				}
				SchemaContainerRoot root = boot.schemaContainerRoot();
				if (requestUser.hasPermission(root, CREATE_PERM)) {
					SchemaContainer container;
					try (Trx txCreate = new Trx(db)) {
						container = root.create(schema, requestUser);
						requestUser.addCRUDPermissionOnRole(root, CREATE_PERM, container);
						txCreate.success();
					}
					try (Trx txSearch = new Trx(db)) {
						searchQueue().put(container.getUuid(), SchemaContainer.TYPE, SearchQueueEntryAction.CREATE_ACTION);
						vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
						txSearch.success();
					}
					transformAndResponde(rc, container);
				}

			} catch (Exception e1) {
				rc.fail(e1);
			}
		}

	}

	@Override
	public void handleDelete(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			delete(rc, "uuid", "schema_deleted", boot.schemaContainerRoot());
		}
	}

	@Override
	public void handleUpdate(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadObject(rc, "uuid", UPDATE_PERM, boot.schemaContainerRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					SchemaContainer schemaContainer = rh.result();
					SchemaUpdateRequest requestModel = fromJson(rc, SchemaUpdateRequest.class);

					if (StringUtils.isEmpty(requestModel.getName())) {
						rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error_name_must_be_set")));
						return;
					}

					schemaContainer.setSchema(requestModel);
					/*
					 * // if (!schema.getName().equals(requestModel.getName())) { // schema.setName(requestModel.getName()); // } //TODO handle request
					 */
					searchQueue().put(schemaContainer.getUuid(), SchemaContainer.TYPE, SearchQueueEntryAction.UPDATE_ACTION);
					vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
					transformAndResponde(rc, schemaContainer);
				}
			});
		}
	}

	@Override
	public void handleRead(RoutingContext rc) {
		String uuid = rc.request().params().get("uuid");
		if (StringUtils.isEmpty(uuid)) {
			rc.next();
		} else {
			try (Trx tx = new Trx(db)) {
				loadTransformAndResponde(rc, "uuid", READ_PERM, boot.schemaContainerRoot());
			}
		}
	}

	@Override
	public void handleReadList(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadTransformAndResponde(rc, boot.schemaContainerRoot(), new SchemaListResponse());
		}
	}

	public void handleAddProjectToSchema(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadObject(rc, "projectUuid", UPDATE_PERM, boot.projectRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					loadObject(rc, "schemaUuid", READ_PERM, boot.schemaContainerRoot(), srh -> {
						if (hasSucceeded(rc, srh)) {
							Project project = rh.result();
							SchemaContainer schema = srh.result();
							try (Trx txAdd = new Trx(db)) {
								project.getSchemaContainerRoot().addSchemaContainer(schema);
								txAdd.success();
							}
							// TODO add simple message or return schema?
							transformAndResponde(rc, schema);
						}
					});
				}
			});
		}

	}

	public void handleRemoveProjectFromSchema(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadObject(rc, "projectUuid", UPDATE_PERM, boot.projectRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					// TODO check whether schema is assigned to project
					loadObject(rc, "schemaUuid", READ_PERM, boot.schemaContainerRoot(), srh -> {
						if (hasSucceeded(rc, srh)) {
							SchemaContainer schema = srh.result();
							Project project = rh.result();
							try (Trx txRemove = new Trx(db)) {
								project.getSchemaContainerRoot().removeSchemaContainer(schema);
								txRemove.success();
							}
							transformAndResponde(rc, schema);
						}
					});
				}
			});
		}
	}

}
