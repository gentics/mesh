package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.core.data.service.generic.GenericPropertyContainerServiceImpl;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.data.service.transformation.tag.TagTransformationTask;
import com.gentics.mesh.core.repository.TagRepository;
import com.gentics.mesh.core.rest.tag.response.TagResponse;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.paging.PagingInfo;

@Component
@Transactional(readOnly = true)
public class TagServiceImpl extends GenericPropertyContainerServiceImpl<Tag> implements TagService {

	private static final Logger log = LoggerFactory.getLogger(TagServiceImpl.class);

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private LanguageService languageService;

	@Autowired
	private MeshNodeService nodeService;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private MeshSpringConfiguration springConfiguration;

	@Autowired
	private GraphDatabaseService graphDb;

	@Autowired
	private UserService userService;

	@Autowired
	private RoutingContextService rcs;

	private static ForkJoinPool pool = new ForkJoinPool(8);

	@Override
	public TagResponse transformToRest(RoutingContext rc, Tag tag) {

		TransformationInfo info = new TransformationInfo(rc);
		info.setUserService(userService);
		info.setLanguageService(languageService);
		info.setGraphDb(graphDb);
		info.setContentService(nodeService);
		info.setSpringConfiguration(springConfiguration);
		info.setTagService(this);
		info.setNeo4jTemplate(neo4jTemplate);

		// Configuration
		List<String> languageTags = rcs.getSelectedLanguageTags(rc);
		info.setLanguageTags(languageTags);

		TagResponse restTag = new TagResponse();
		TagTransformationTask task = new TagTransformationTask(tag, info, restTag);

		pool.invoke(task);
		return restTag;
	}

	@Override
	public Page<Tag> findProjectTags(RoutingContext rc, String projectName, List<String> languageTags, PagingInfo pagingInfo) {
		String userUuid = rc.session().getPrincipal().getString("uuid");
		return tagRepository.findProjectTags(userUuid, projectName, languageTags, pagingInfo);
	}

	@Override
	public Page<Tag> findTags(RoutingContext rc, String projectName, MeshNode node, List<String> languageTags, PagingInfo pagingInfo) {
		String userUuid = rc.session().getPrincipal().getString("uuid");
		return tagRepository.findTags(userUuid, projectName, node, languageTags, pagingInfo);
	}

	@Override
	public Page<MeshNode> findTaggedNodes(RoutingContext rc, String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo) {
		String userUuid = rc.session().getPrincipal().getString("uuid");
		return tagRepository.findTaggedNodes(userUuid, projectName, tag, languageTags, pagingInfo);
	}

}
