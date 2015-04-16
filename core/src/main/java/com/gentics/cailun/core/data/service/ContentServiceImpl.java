package com.gentics.cailun.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.I18NProperties;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.relationship.Translated;
import com.gentics.cailun.core.data.service.content.ContentTransformationTask;
import com.gentics.cailun.core.data.service.content.TransformationInfo;
import com.gentics.cailun.core.data.service.generic.GenericPropertyContainerServiceImpl;
import com.gentics.cailun.core.repository.ContentRepository;
import com.gentics.cailun.core.repository.GroupRepository;
import com.gentics.cailun.core.rest.content.response.ContentResponse;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.path.PagingInfo;

@Component
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
	public ContentResponse transformToRest(RoutingContext rc, Content content, List<String> languageTags, int depth) {
		TransformationInfo info = new TransformationInfo(rc, depth, languageTags);
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
		PageRequest pr = new PageRequest(pagingInfo.getPage(), pagingInfo.getPerPage());
		if (languageTags == null || languageTags.size() == 0) {
			return contentRepository.findAll(requestUser, projectName, pr);
		} else {
			return contentRepository.findAll(requestUser, projectName, languageTags, pr);
		}
	}

	// private Node getChildNodePageFromNodeTag(Node node, String pageFilename) {
	// AtomicReference<Node> foundNode = new AtomicReference<>();
	// //TODO check the performance of this iteration
	// Lists.newArrayList(node.getRelationships(BasicRelationships.TYPES.HAS_SUB_TAG, Direction.OUTGOING)).stream().forEach(rel -> {
	// Node nextHop = rel.getStartNode();
	// if (nextHop.hasLabel(DynamicLabel.label(Content.class.getSimpleName()))) {
	// String currentName = (String) nextHop.getProperty(GenericFile.FILENAME_KEYWORD);
	// if (pageFilename.equalsIgnoreCase(currentName)) {
	// foundNode.set(nextHop);
	// return;
	// }
	// }
	// });
	// return foundNode.get();
	//
	// }
	// find content code
	// System.out.println("Tag: " + currentNode);
	// if (currentNode != null) {
	// // Finally search for the page and assume the last part of the request as filename
	// Node pageNode = getChildNodePageFromNodeTag(currentNode, parts[parts.length - 1]);
	// if (pageNode != null) {
	// // return pageNode.getId();
	// return null;
	// } else {
	// return null;
	// }
	// }
	//
	// System.out.println("looking for " + path + " in project " + projectName);
	// return null;

	// @Override
	// public Content save(String projectName, String path, ContentResponse requestModel) {
	//
	// // TODO check permissions
	// if (requestModel.getUUID() == null) {
	// Project project = projectService.findByName(projectName);
	// // Language language = languageService.findByLanguageTag(requestModel.getLanguageTag());
	// Language language = null;
	// // TODO save given languages individually, TODO how can we delete a single language?
	// if (language == null || requestModel.getSchemaName() == null) {
	// // TODO handle this case
	// throw new NullPointerException("No language or type specified");
	// }
	//
	// // We need to validate the saved data using the object schema
	// ObjectSchema objectSchema = objectSchemaService.findByName(projectName, requestModel.getSchemaName());
	// if (objectSchema == null) {
	// // TODO handle this case
	// throw new NullPointerException("Could not find object schema for type {" + requestModel.getSchemaName() + "} and project {" + projectName
	// + "}");
	// }
	//
	// // TODO handle types , verify that type exists
	// Content content = new Content();
	// content.setProject(project);
	// content.setSchema(requestModel.getSchemaName());
	// // for (Entry<String, String> entry : requestModel.getProperties().entrySet()) {
	// // PropertyTypeSchema propertyTypeSchema = objectSchema.getPropertyTypeSchema(entry.getKey());
	// // // TODO we should abort when we encounter a property with an unknown key.
	// // // Determine whether the property is an i18n one or not
	// // if (propertyTypeSchema == null) {
	// // content.setProperty(entry.getKey(), entry.getValue());
	// // } else if (propertyTypeSchema.getType().equals(PropertyType.I18N_STRING)) {
	// // setProperty(content, language, entry.getKey(), entry.getValue());
	// // } else {
	// // // TODO handle this case
	// // }
	// // }
	// return save(content);
	//
	// } else {
	//
	// }
	// return null;
	// }

	public void createLink(Content from, Content to) {
		// TODO maybe extract information about link start and end to speedup rendering of page with links
		// Linked link = new Linked(this, page);
		// this.links.add(link);
	}


}
