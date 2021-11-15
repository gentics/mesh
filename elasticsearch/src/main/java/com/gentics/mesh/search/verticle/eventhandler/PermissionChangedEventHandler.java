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
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.search.request.UpdateDocumentRequest;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.core.rest.event.role.PermissionChangedProjectElementEventModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.search.index.node.NodeContainerTransformer;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;

import io.reactivex.Flowable;

/**
 * Event handler for permission change events. A permission change event may require documents to be updated since the document also contain references to the
 * roleUuids which grant read to those elements.
 */
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
			ProjectDao projectDao = tx.projectDao();
			BranchDao branchDao = tx.branchDao();
			NodeDao nodeDao = tx.nodeDao();

			return ofNullable(projectDao.findByUuid(model.getProject().getUuid()))
				.flatMap(project -> ofNullable(nodeDao.findByUuid(project, model.getUuid()))
					.flatMap(node -> branchDao.findAll(project).stream().map(HibBaseElement::getUuid)
						.flatMap(branchUuid -> Util.latestVersionTypes()
							.flatMap(type -> tx.contentDao().getFieldContainers(node, branchUuid, type).stream()
								.map(container -> meshHelper.updateDocumentRequest(
									ContentDao.composeIndexName(
										model.getProject().getUuid(),
										branchUuid,
										container.getSchemaContainerVersion().getUuid(),
										type),
									ContentDao.composeDocumentId(model.getUuid(), container.getLanguageTag()),
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
			return HibUser.composeIndexName();
		case GROUP:
			return HibGroup.composeIndexName();
		case ROLE:
			return HibRole.composeIndexName();
		case PROJECT:
			return HibProject.composeIndexName();
		case SCHEMA:
			return HibSchema.composeIndexName();
		case MICROSCHEMA:
			return HibMicroschema.composeIndexName();
		case TAG:
			if (model instanceof PermissionChangedProjectElementEventModel) {
				PermissionChangedProjectElementEventModel projectModel = (PermissionChangedProjectElementEventModel) model;
				return HibTag.composeIndexName(projectModel.getProject().getUuid());
			}
		case TAGFAMILY:
			if (model instanceof PermissionChangedProjectElementEventModel) {
				PermissionChangedProjectElementEventModel projectModel = (PermissionChangedProjectElementEventModel) model;
				return HibTagFamily.composeIndexName(projectModel.getProject().getUuid());
			}
		default:
			throw new InvalidParameterException("Unexpected event: " + model);
		}
	}

}
