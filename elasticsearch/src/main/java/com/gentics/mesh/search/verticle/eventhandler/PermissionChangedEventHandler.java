package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.request.UpdateDocumentRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModel;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.search.index.node.NodeContainerTransformer;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import io.reactivex.Flowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toFlowable;
import static com.gentics.mesh.util.StreamUtil.ofNullable;

@Singleton
public class PermissionChangedEventHandler implements EventHandler {
	private static final Logger log = LoggerFactory.getLogger(PermissionChangedEventHandler.class);

	private final MeshEntities meshEntities;
	private final MeshHelper meshHelper;

	@Inject
	public PermissionChangedEventHandler(MeshEntities meshEntities, MeshHelper meshHelper) {
		this.meshEntities = meshEntities;
		this.meshHelper = meshHelper;
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Collections.singletonList(MeshEvent.ROLE_PERMISSIONS_CHANGED);
	}

	@Override
	public Flowable<UpdateDocumentRequest> handle(MessageEvent messageEvent) {
		PermissionChangedEventModel model = requireType(PermissionChangedEventModel.class, messageEvent.message);

		if (model.getType() == ElementType.NODE) {
			return handleNodePermissionsChange(model);
		} else {
			return simplePermissionChange(model);
		}
	}

	private Flowable<UpdateDocumentRequest> handleNodePermissionsChange(PermissionChangedEventModel model) {
		NodeContainerTransformer tf = (NodeContainerTransformer) meshEntities.nodeContent.getTransformer();
		return meshHelper.getDb().tx(() ->
			ofNullable(meshHelper.getBoot().projectRoot().findByUuid(model.getProject().getUuid()))
				.flatMap(project -> ofNullable(project.getNodeRoot().findByUuid(model.getUuid()))
				.flatMap(node -> project.getBranchRoot().findAll().stream().map(MeshElement::getUuid)
				.flatMap(branchUuid -> Stream.of(ContainerType.DRAFT, ContainerType.PUBLISHED)
				.flatMap(type -> node.getGraphFieldContainers(branchUuid, type).stream()
				.map(container -> meshHelper.updateDocumentRequest(
					NodeGraphFieldContainer.composeIndexName(
						model.getProject().getUuid(),
						branchUuid,
						container.getSchemaContainerVersion().getUuid(),
						type
					),
					NodeGraphFieldContainer.composeDocumentId(model.getUuid(), container.getLanguageTag()),
					tf.toPermissionPartial(node, type)
				))))))
			.collect(toFlowable())
		);
	}

	private Flowable<UpdateDocumentRequest> simplePermissionChange(PermissionChangedEventModel model) {
		return meshEntities.of(model.getType())
			.flatMap(meshEntity -> meshHelper.getDb().tx(() -> meshEntity.getPermissionPartial(model)))
			.map(doc -> Flowable.just(meshHelper.updateDocumentRequest(
				getIndex(model),
				model.getUuid(),
				doc
			))).orElse(Flowable.empty());
	}

	private String getIndex(PermissionChangedEventModel model) {
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
			case SCHEMACONTAINER:
				return SchemaContainer.composeIndexName();
			case MICROSCHEMAVERSION:
				return MicroschemaContainer.composeIndexName();
			case TAG:
				return Tag.composeIndexName(model.getProject().getUuid());
			case TAGFAMILY:
				return TagFamily.composeIndexName(model.getProject().getUuid());
			default:
				throw new InvalidParameterException("Unexpected event: " + model);
		}
	}


}
