package com.gentics.mesh.core.data.service;

import io.vertx.core.Future;
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

import com.gentics.mesh.core.data.model.Content;
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
	private ContentService contentService;

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
		info.setContentService(contentService);
		info.setSpringConfiguration(springConfiguration);
		info.setTagService(this);
		info.setNeo4jTemplate(neo4jTemplate);

		// Configuration
		List<String> languageTags = rcs.getSelectedLanguageTags(rc);
		info.setLanguageTags(languageTags);
		// Future<Integer> depthFuture = rcs.getDepthParameter(rc);
		Future<Boolean> tagsIncludeFuture = rcs.getTagsIncludeParameter(rc);
		info.setIncludeTags(tagsIncludeFuture.result());
		Future<Boolean> contentIncludeFuture = rcs.getContentsIncludeParameter(rc);
		info.setIncludeContents(contentIncludeFuture.result());
		Future<Boolean> childTagIncludeFuture = rcs.getChildTagIncludeParameter(rc);
		info.setIncludeChildTags(childTagIncludeFuture.result());

		TagResponse restTag = new TagResponse();
		TagTransformationTask task = new TagTransformationTask(tag, info, restTag);

		pool.invoke(task);
		return restTag;
	}

	@Override
	public Page<Tag> findChildTags(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo) {
		String userUuid = rc.session().getPrincipal().getString("uuid");
		return tagRepository.findChildTags(userUuid, projectName, rootTag, languageTags, pagingInfo);
	}

	@Override
	public Page<Content> findChildContents(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo) {
		String userUuid = rc.session().getPrincipal().getString("uuid");
		return tagRepository.findChildContents(userUuid, projectName, rootTag, languageTags, pagingInfo);
	}

	@Override
	public Page<Content> findTaggedContents(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo) {
		String userUuid = rc.session().getPrincipal().getString("uuid");
		return tagRepository.findTaggedContents(userUuid, projectName, rootTag, languageTags, pagingInfo);
	}

	@Override
	public Page<Tag> findProjectTags(RoutingContext rc, String projectName, List<String> languageTags, PagingInfo pagingInfo) {
		String userUuid = rc.session().getPrincipal().getString("uuid");
		return tagRepository.findProjectTags(userUuid, projectName, languageTags, pagingInfo);
	}

	@Override
	public Page<Tag> findTaggedTags(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo) {
		String userUuid = rc.session().getPrincipal().getString("uuid");
		return tagRepository.findTaggedTags(userUuid, projectName, rootTag, languageTags, pagingInfo);
	}

	@Override
	public Page<Tag> findTaggingTags(RoutingContext rc, String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo) {
		String userUuid = rc.session().getPrincipal().getString("uuid");
		return tagRepository.findTaggingTags(userUuid, projectName, tag, languageTags, pagingInfo);
	}

}
