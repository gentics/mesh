package com.gentics.mesh.core.verticle.microschema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.json.JsonUtil;

import rx.Observable;

@Component
public class MicroschemaCrudHandler extends AbstractCrudHandler<MicroschemaContainer, Microschema> {

	@Autowired
	private MicroschemaComparator comparator;

	@Override
	public RootVertex<MicroschemaContainer> getRootVertex(InternalActionContext ac) {
		return boot.microschemaContainerRoot();
	}

	@Override
	public void handleUpdate(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		db.asyncNoTrxExperimental(() -> {
			RootVertex<MicroschemaContainer> root = getRootVertex(ac);
			return root.loadObjectByUuid(ac, uuid, UPDATE_PERM).flatMap(element -> {
				return db.trx(() -> {
					try {
						Microschema requestModel = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModel.class);
						SchemaChangesListModel model = new SchemaChangesListModel();
						model.getChanges().addAll(MicroschemaComparator.getIntance().diff(element.getLatestVersion().getSchema(), requestModel));
						String name = element.getName();
						if (model.getChanges().isEmpty()) {
							return Observable.just(message(ac, "schema_update_no_difference_detected", name));
						} else {
							return element.getLatestVersion().applyChanges(ac, model).flatMap(e -> {
								return Observable.just(message(ac, "migration_invoked", name));
							});
						}
					} catch (Exception e) {
						return Observable.error(e);
					}
				});
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		HandlerUtilities.deleteElement(ac, () -> getRootVertex(ac), uuid, "microschema_deleted");
	}

	/**
	 * Compare the latest schema version with the given schema model.
	 * 
	 * @param uuid
	 *            Schema uuid
	 * @param ac
	 */
	public void handleDiff(InternalActionContext ac, String uuid) {
		db.asyncNoTrxExperimental(() -> {
			Observable<MicroschemaContainer> obsSchema = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			Microschema requestModel = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModel.class);
			return obsSchema.flatMap(microschema -> microschema.getLatestVersion().diff(ac, comparator, requestModel));
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleGetSchemaChanges(InternalActionContext ac, String schemaUuid) {
		// TODO Auto-generated method stub

	}

	public void handleApplySchemaChanges(InternalActionContext ac, String schemaUuid) {
		db.asyncNoTrxExperimental(() -> {
			Observable<MicroschemaContainer> obsSchema = boot.microschemaContainerRoot().loadObjectByUuid(ac, schemaUuid, UPDATE_PERM);
			return obsSchema.flatMap(schema -> {
				return schema.getLatestVersion().applyChanges(ac);
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

}
