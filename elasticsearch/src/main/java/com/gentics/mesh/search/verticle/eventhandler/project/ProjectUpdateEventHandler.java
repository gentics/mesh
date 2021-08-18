package com.gentics.mesh.search.verticle.eventhandler.project;

import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toFlowable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.TagFamilyDao;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.search.index.node.NodeContainerTransformer;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import com.gentics.mesh.search.verticle.eventhandler.EventHandler;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import io.reactivex.Flowable;

/**
 * When a project is updated, all nodes, tags and tag family documents have to
 * be updated, because the documents for these objects contain the project name.
 */
@Singleton
public class ProjectUpdateEventHandler implements EventHandler {

	private final MeshHelper helper;
	private final MeshEntities entities;
	private final ComplianceMode complianceMode;

	@Inject
	public ProjectUpdateEventHandler(MeshHelper helper, MeshEntities entities, MeshOptions options) {
		this.helper = helper;
		this.entities = entities;
		this.complianceMode = options.getSearchOptions().getComplianceMode();
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		MeshElementEventModelImpl model = requireType(MeshElementEventModelImpl.class, messageEvent.message);
		return Flowable.mergeArray(updateNodes(model), updateTags(model));
	}

	/**
	 * Finds all latest nodes of all languages, all types and all branches in the
	 * project and transforms them to elastic search create requests.
	 * 
	 * @param model
	 * @return
	 */
	private Flowable<SearchRequest> updateNodes(MeshElementEventModelImpl model) {
		return Flowable.defer(() -> helper.getDb()
				.transactional(tx -> entities.project.getElement(model).stream().flatMap(project -> {
					BranchDao branchDao = tx.branchDao();
					NodeDao nodeDao = tx.nodeDao();
					List<Branch> branches = (List<Branch>) branchDao.findAll(project).list();
					return nodeDao.findAll(project).stream().flatMap(node -> Stream.of(DRAFT, PUBLISHED)
							.flatMap(type -> branches.stream().flatMap(branch -> ((ContentDaoWrapper) tx.contentDao())
									.getGraphFieldContainers(node, branch, type).stream()
									.map(container -> helper.createDocumentRequest(
											ContentDao.composeIndexName(project.getUuid(), branch.getUuid(),
													container.getSchemaContainerVersion().getUuid(), type),
											ContentDao.composeDocumentId(node.getUuid(),
													container.getLanguageTag()),
											((NodeContainerTransformer) entities.nodeContent.getTransformer())
													.toDocument(container, branch.getUuid(), type),
											complianceMode)))));
				}).collect(toFlowable())).runInNewTx());
	}

	/**
	 * Creates requests for all tag families and tags in the project
	 * 
	 * @param model
	 * @return
	 */
	private Flowable<SearchRequest> updateTags(MeshElementEventModelImpl model) {
		return Flowable.defer(() -> helper.getDb()
				.transactional(tx -> entities.project.getElement(model).stream().flatMap(project -> {
					TagFamilyDao tagFamilyDao = tx.tagFamilyDao();
					return tagFamilyDao.findAll(project).stream()
							.flatMap(family -> Stream.concat(Stream.of(createTagFamilyRequest(project, family)),
									createTagRequests(family, project)));
				}).collect(toFlowable())).runInNewTx());
	}

	private Stream<CreateDocumentRequest> createTagRequests(HibTagFamily family, HibProject project) {
		TagDao tagDao = Tx.get().tagDao();
		return tagDao.findAll(family).stream()
				.map(tag -> helper.createDocumentRequest(Tag.composeIndexName(project.getUuid()), tag.getUuid(),
						entities.tag.transform(tag), complianceMode));
	}

	private CreateDocumentRequest createTagFamilyRequest(HibProject project, HibTagFamily family) {
		return helper.createDocumentRequest(TagFamily.composeIndexName(project.getUuid()), family.getUuid(),
				entities.tagFamily.transform(family), complianceMode);
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(PROJECT_UPDATED);
	}

}
