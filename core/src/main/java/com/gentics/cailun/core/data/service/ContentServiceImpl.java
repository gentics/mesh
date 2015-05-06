package com.gentics.cailun.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.generic.GenericPropertyContainerServiceImpl;
import com.gentics.cailun.core.data.service.transformation.TransformationInfo;
import com.gentics.cailun.core.data.service.transformation.content.ContentTransformationTask;
import com.gentics.cailun.core.link.CaiLunLinkResolver;
import com.gentics.cailun.core.link.CaiLunLinkResolverFactoryImpl;
import com.gentics.cailun.core.repository.ContentRepository;
import com.gentics.cailun.core.repository.GroupRepository;
import com.gentics.cailun.core.rest.content.response.ContentResponse;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.paging.CaiLunPageRequest;
import com.gentics.cailun.paging.PagingInfo;

@Component
@Transactional(readOnly = true)
public class ContentServiceImpl extends GenericPropertyContainerServiceImpl<Content> implements ContentService {

	@Autowired
	private LanguageService languageService;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private UserService userService;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private ObjectSchemaService objectSchemaService;

	@Autowired
	private TagService tagService;

	@Autowired
	private GraphDatabaseService graphDb;

	@Autowired
	private CaiLunSpringConfiguration springConfiguration;

	@Autowired
	private ContentRepository contentRepository;

	@Autowired
	private I18NService i18n;

	private static ForkJoinPool pool = new ForkJoinPool(8);

	public void setTeaser(Content content, Language language, String text) {
		setProperty(content, language, ObjectSchema.TEASER_KEY, text);
	}

	public void setTitle(Content content, Language language, String text) {
		setProperty(content, language, ObjectSchema.TITLE_KEY, text);
	}

	@Override
	public Iterable<Content> findAll(String project) {
		return contentRepository.findAll(project);
	}

	@Override
	public ContentResponse transformToRest(RoutingContext rc, Content content) {

		TransformationInfo info = new TransformationInfo(rc);
		info.setUserService(userService);
		info.setLanguageService(languageService);
		info.setGraphDb(graphDb);
		info.setTagService(tagService);
		info.setSpringConfiguration(springConfiguration);
		info.setContentService(this);
		info.setNeo4jTemplate(neo4jTemplate);
		info.setI18nService(i18n);
		ContentResponse restContent = new ContentResponse();
		ContentTransformationTask task = new ContentTransformationTask(content, info, restContent);
		pool.invoke(task);
		return restContent;

	}

	@Override
	public Page<Content> findAllVisible(User requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo) {
		CaiLunPageRequest pr = new CaiLunPageRequest(pagingInfo);
		if (languageTags == null || languageTags.size() == 0) {
			return contentRepository.findAll(requestUser, projectName, pr);
		} else {
			return contentRepository.findAll(requestUser, projectName, languageTags, pr);
		}
	}

	public void createLink(Content from, Content to) {
		// TODO maybe extract information about link start and end to speedup rendering of page with links
		// Linked link = new Linked(this, page);
		// this.links.add(link);
	}

}
