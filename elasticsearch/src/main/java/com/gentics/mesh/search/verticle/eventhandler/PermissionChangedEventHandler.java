package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toFlowable;
import static com.gentics.mesh.util.StreamUtil.ofNullable;

import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.search.request.UpdateDocumentRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.core.rest.event.role.PermissionChangedProjectElementEventModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.search.index.node.NodeContainerTransformer;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;

import io.reactivex.Flowable;

@Singleton
public class PermissionChangedEventHandler implements EventHandler {

	private final MeshEntities meshEntities;
	private final MeshHelper meshHelper;
	private final ComplianceMode complianceMode;

	@Inject
	public PermissionChangedEventHandler(MeshEntities meshEntities, MeshHelper meshHelper, MeshOptions options) {
		this.meshEntities = meshEntities;
		this.meshHelper = meshHelper;
		this.complianceMode = options.getSearchOptions().getComplianceMode();
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Collections.singletonList(MeshEvent.ROLE_PERMISSIONS_CHANGED);
	}

	@Override
	public Flowable<UpdateDocumentRequest> handle(MessageEvent messageEvent) {
		PermissionChangedEventModelImpl model = requireType(PermissionChangedEventModelImpl.class, messageEvent.message);

		// Check whether the action affects read permissions. We only need to update the document in the index if the action affects those perms
		// boolean grantReads = permissionsToGrant.contains(READ_PERM) || permissionsToGrant.contains(READ_PUBLISHED_PERM);
		// boolean revokesRead = permissionsToRevoke.contains(READ_PERM) || permissionsToRevoke.contains(READ_PUBLISHED_PERM);
		// if (grantReads || revokesRead) {
		//
		// }

		if (model.getType() == ElementType.NODE) {
			PermissionChangedProjectElementEventModel nodeModel = requireType(PermissionChangedProjectElementEventModel.class, messageEvent.message);
			return handleNodePermissionsChange(nodeModel);
		} else {
			return simplePermissionChange(model);
		}
	}

	private Flowable<UpdateDocumentRequest> handleNodePermissionsChange(PermissionChangedProjectElementEventModel model) {
		NodeContainerTransformer tf = (NodeContainerTransformer) meshEntities.nodeContent.getTransformer();
		return meshHelper.getDb().tx(tx -> {
			ProjectDaoWrapper projectDao = tx.data().projectDao();
			BranchDaoWrapper branchDao =  tx.data().branchDao();
			NodeDaoWrapper nodeDao = tx.data().nodeDao();

			return ofNullable(projectDao.findByUuid(model.getProject().getUuid()))
				.flatMap(project -> ofNullable(nodeDao.findByUuid(project, model.getUuid()))
					.flatMap(node -> branchDao.findAll(project).stream().map(HibElement::getUuid)
						.flatMap(branchUuid -> Util.latestVersionTypes()
							.flatMap(type -> tx.data().contentDao().getGraphFieldContainers(node, branchUuid, type).stream()
								.map(container -> meshHelper.updateDocumentRequest(
									ContentDaoWrapper.composeIndexName(
										model.getProject().getUuid(),
										branchUuid,
										container.getSchemaContainerVersion().getUuid(),
										type),
									ContentDaoWrapper.composeDocumentId(model.getUuid(), container.getLanguageTag()),
									tf.toPermissionPartial(node, type), complianceMode))))))
				.collect(toFlowable());
		});
	}

	private Flowable<UpdateDocumentRequest> simplePermissionChange(PermissionChangedEventModelImpl model) {
		return meshEntities.of(model.getType())
			.flatMap(meshEntity -> meshHelper.getDb().tx(() -> meshEntity.getPermissionPartial(model)))
			.map(doc -> Flowable.just(meshHelper.updateDocumentRequest(
				getIndex(model),
				model.getUuid(),
				doc, complianceMode)))
			.orElse(Flowable.empty());
	}

	private String getIndex(PermissionChangedEventModelImpl model) {
		// TODO Consider moving to entities
		switch (model.getType()) {
		case USER:
			return User.composeIndexName();
		case GROUP:
			return Group.composeIndexName();
		case ROLE:
			return Role.composeIndexName();
		case PROJECT:
			return Project.composeIndexName();
		case SCHEMA:
			return Schema.composeIndexName();
		case MICROSCHEMA:
			return Microschema.composeIndexName();
		case TAG:
			if (model instanceof PermissionChangedProjectElementEventModel) {
				PermissionChangedProjectElementEventModel projectModel = (PermissionChangedProjectElementEventModel) model;
				return Tag.composeIndexName(projectModel.getProject().getUuid());
			}
		case TAGFAMILY:
			if (model instanceof PermissionChangedProjectElementEventModel) {
				PermissionChangedProjectElementEventModel projectModel = (PermissionChangedProjectElementEventModel) model;
				return TagFamily.composeIndexName(projectModel.getProject().getUuid());
			}
		default:
			throw new InvalidParameterException("Unexpected event: " + model);
		}
	}

}
