package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.data.model.schema.ObjectSchema;
import com.gentics.mesh.core.data.service.generic.GenericPropertyContainerServiceImpl;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.data.service.transformation.content.MeshNodeTransformationTask;
import com.gentics.mesh.core.repository.MeshNodeRepository;
import com.gentics.mesh.core.repository.GroupRepository;
import com.gentics.mesh.core.rest.meshnode.response.MeshNodeResponse;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.paging.MeshPageRequest;
import com.gentics.mesh.paging.PagingInfo;

@Component
@Transactional(readOnly = true)
public class MeshNodeServiceImpl extends GenericPropertyContainerServiceImpl<MeshNode> implements MeshNodeService {

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
	private MeshSpringConfiguration springConfiguration;

	@Autowired
	private MeshNodeRepository contentRepository;

	@Autowired
	private I18NService i18n;

	@Autowired
	private RoutingContextService rcs;

	private static ForkJoinPool pool = new ForkJoinPool(8);

	public void setTeaser(MeshNode content, Language language, String text) {
		setProperty(content, language, ObjectSchema.TEASER_KEY, text);
	}

	public void setTitle(MeshNode content, Language language, String text) {
		setProperty(content, language, ObjectSchema.TITLE_KEY, text);
	}

	@Override
	public Iterable<MeshNode> findAll(String project) {
		return contentRepository.findAll(project);
	}

	@Override
	public MeshNodeResponse transformToRest(RoutingContext rc, MeshNode content) {

		TransformationInfo info = new TransformationInfo(rc);

		List<String> languageTags = rcs.getSelectedLanguageTags(rc);
		info.setLanguageTags(languageTags);
		info.setUserService(userService);
		info.setLanguageService(languageService);
		info.setGraphDb(graphDb);
		info.setTagService(tagService);
		info.setSpringConfiguration(springConfiguration);
		info.setContentService(this);
		info.setNeo4jTemplate(neo4jTemplate);
		info.setI18nService(i18n);
		MeshNodeResponse restContent = new MeshNodeResponse();
		MeshNodeTransformationTask task = new MeshNodeTransformationTask(content, info, restContent);
		pool.invoke(task);
		return restContent;

	}

	@Override
	public Page<MeshNode> findAllVisible(User requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo) {
		MeshPageRequest pr = new MeshPageRequest(pagingInfo);
		if (languageTags == null || languageTags.size() == 0) {
			return contentRepository.findAll(requestUser, projectName, pr);
		} else {
			return contentRepository.findAll(requestUser, projectName, languageTags, pr);
		}
	}

	public void createLink(MeshNode from, MeshNode to) {
		// TODO maybe extract information about link start and end to speedup rendering of page with links
		// Linked link = new Linked(this, page);
		// this.links.add(link);
	}

}
