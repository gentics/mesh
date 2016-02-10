package com.gentics.mesh.core.verticle.microschema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.rest.schema.MicroschemaResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

@Component
public class MicroschemaCrudHandler extends AbstractCrudHandler<MicroschemaContainer, MicroschemaResponse> {

	@Autowired
	private MicroschemaComparator comparator;
	
	@Override
	public RootVertex<MicroschemaContainer> getRootVertex(InternalActionContext ac) {
		return boot.microschemaContainerRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> getRootVertex(ac), "uuid", "microschema_deleted");
	}
	
	public void handleDiff(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			Observable<MicroschemaContainer> obsSchema = getRootVertex(ac).loadObject(ac, "uuid", READ_PERM);
			return obsSchema.flatMap(microschema -> microschema.diff(ac, comparator));
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}
	
	public void handleGetSchemaChanges(InternalActionContext ac) {
		// TODO Auto-generated method stub

	}
	
	public void handleExecuteSchemaChanges(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			Observable<SchemaContainer> obsSchema = boot.schemaContainerRoot().loadObject(ac, "schemaUuid", UPDATE_PERM);
			return obsSchema.flatMap(schema -> {
				return schema.getLatestVersion().applyChanges(ac);
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

}
