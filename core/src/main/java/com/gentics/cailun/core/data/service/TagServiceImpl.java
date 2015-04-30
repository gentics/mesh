package com.gentics.cailun.core.data.service;

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

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.generic.GenericPropertyContainer;
import com.gentics.cailun.core.data.service.content.TransformationInfo;
import com.gentics.cailun.core.data.service.generic.GenericPropertyContainerServiceImpl;
import com.gentics.cailun.core.data.service.tag.TagTransformationTask;
import com.gentics.cailun.core.repository.TagRepository;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.paging.CaiLunPageRequest;
import com.gentics.cailun.paging.PagingInfo;

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
	private CaiLunSpringConfiguration springConfiguration;

	@Autowired
	private GraphDatabaseService graphDb;

	@Autowired
	private UserService userService;

	private static ForkJoinPool pool = new ForkJoinPool(8);

	@Override
	public TagResponse transformToRest(RoutingContext rc, Tag tag, List<String> languageTags, int depth) {

		TransformationInfo info = new TransformationInfo(rc, depth, languageTags);
		info.setUserService(userService);
		info.setLanguageService(languageService);
		info.setGraphDb(graphDb);
		info.setContentService(contentService);
		info.setSpringConfiguration(springConfiguration);
		info.setTagService(this);
		info.setNeo4jTemplate(neo4jTemplate);

		TagResponse restTag = new TagResponse();
		TagTransformationTask task = new TagTransformationTask(tag, info, restTag);

		pool.invoke(task);
		return restTag;
	}

	@Override
	public Page<Tag> findAllVisible(RoutingContext rc, String projectName, List<String> languageTags, PagingInfo pagingInfo) {
		CaiLunPageRequest pr = new CaiLunPageRequest(pagingInfo);
		String userUuid = rc.session().getPrincipal().getString("uuid");
		if (languageTags == null || languageTags.size() == 0) {
			return tagRepository.findAll(userUuid, projectName, pr);
		} else {
			return tagRepository.findAll(userUuid, projectName, languageTags, pr);
		}
	}

	@Override
	public Page<Tag> findAllVisibleTags(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo) {
		CaiLunPageRequest pr = new CaiLunPageRequest(pagingInfo);
		String userUuid = rc.session().getPrincipal().getString("uuid");

		if (languageTags == null || languageTags.size() == 0) {
			return tagRepository.findAllTags(userUuid, projectName, rootTag, pr);
		} else {
			return tagRepository.findAllTags(userUuid, projectName, rootTag, languageTags, pr);
		}
	}

	@Override
	public Page<Content> findAllVisibleContents(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags,
			PagingInfo pagingInfo) {
		CaiLunPageRequest pr = new CaiLunPageRequest(pagingInfo);
		String userUuid = rc.session().getPrincipal().getString("uuid");

		if (languageTags == null || languageTags.size() == 0) {
			return tagRepository.findAllVisibleContents(userUuid, projectName, rootTag, pr);
		} else {
			return tagRepository.findAllVisibleContents(userUuid, projectName, rootTag, languageTags, pr);
		}
	}

	@Override
	public Page<? extends GenericPropertyContainer> findAllVisibleChildNodes(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo) {
		CaiLunPageRequest pr = new CaiLunPageRequest(pagingInfo);
		String userUuid = rc.session().getPrincipal().getString("uuid");

		if (languageTags == null || languageTags.size() == 0) {
			return tagRepository.findAllVisibleChildNodes(userUuid, projectName, rootTag, pr);
		} else {
			return tagRepository.findAllVisibleChildNodes(userUuid, projectName, rootTag, languageTags, pr);
		}	}

}
