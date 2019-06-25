package com.gentics.mesh.search.verticle.eventhandler.project;

import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toFlowable;
import static com.gentics.mesh.util.StreamUtil.toStream;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.search.index.node.NodeContainerTransformer;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import com.gentics.mesh.search.verticle.eventhandler.EventHandler;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import io.reactivex.Flowable;

/**
 * When a project is updated, all nodes, tags and tag family documents have to be updated,
 * because the documents for these objects contain the project name.
 */
@Singleton
public class ProjectUpdateEventHandler implements EventHandler {

	private final MeshHelper helper;
	private final MeshEntities entities;

	@Inject
	public ProjectUpdateEventHandler(MeshHelper helper, MeshEntities entities) {
		this.helper = helper;
		this.entities = entities;
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		MeshElementEventModelImpl model = requireType(MeshElementEventModelImpl.class, messageEvent.message);
		return Flowable.mergeArray(
			updateNodes(model),
			updateTags(model)
		);
	}

	/**
	 * Finds all latest nodes of all languages, all types and all branches in the project and transforms them to
	 * elastic search create requests.
	 * @param model
	 * @return
	 */
	private Flowable<SearchRequest> updateNodes(MeshElementEventModelImpl model) {
		return Flowable.defer(() -> helper.getDb().transactional(tx -> toStream(entities.project.getElement(model))
				.flatMap(project -> {
					List<Branch> branches = (List<Branch>) project.getBranchRoot().findAll().list();
					return project.getNodeRoot().findAll().stream()
				.flatMap(node -> Stream.of(DRAFT, PUBLISHED)
				.flatMap(type -> branches.stream()
				.flatMap(branch -> node.getGraphFieldContainers(branch, type).stream()
				.map(container -> helper.createDocumentRequest(
					NodeGraphFieldContainer.composeIndexName(
						project.getUuid(),
						branch.getUuid(),
						container.getSchemaContainerVersion().getUuid(),
						type
					),
					NodeGraphFieldContainer.composeDocumentId(node.getUuid(), container.getLanguageTag()),
					((NodeContainerTransformer)entities.nodeContent.getTransformer()).toDocument(
						container,
						branch.getUuid(),
						type
					)
				))
			)));})
			.collect(toFlowable()))
			.runInNewTx()
		);
	}

	/**
	 * Creates requests for all tag families and tags in the project
	 * @param model
	 * @return
	 */
	private Flowable<SearchRequest> updateTags(MeshElementEventModelImpl model) {
		return Flowable.defer(() -> helper.getDb().transactional(tx -> toStream(entities.project.getElement(model))
				.flatMap(project -> project.getTagFamilyRoot().findAll().stream()
				.flatMap(family -> Stream.concat(
					Stream.of(createTagFamilyRequest(project, family)),
					createTagRequests(family, project)
				)
			))
			.collect(toFlowable()))
			.runInNewTx()
		);
	}

	private Stream<CreateDocumentRequest> createTagRequests(TagFamily family, Project project) {
		return family.findAll().stream()
		.map(tag -> helper.createDocumentRequest(
			Tag.composeIndexName(project.getUuid()),
			tag.getUuid(),
			entities.tag.transform(tag)
		));
	}

	private CreateDocumentRequest createTagFamilyRequest(Project project, TagFamily family) {
		return helper.createDocumentRequest(
			TagFamily.composeIndexName(project.getUuid()),
			family.getUuid(),
			entities.tagFamily.transform(family)
		);
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(PROJECT_UPDATED);
	}

}
