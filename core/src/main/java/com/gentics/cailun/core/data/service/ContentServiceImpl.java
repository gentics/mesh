package com.gentics.cailun.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.GraphPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.relationship.Translated;
import com.gentics.cailun.core.data.service.generic.GenericPropertyContainerServiceImpl;
import com.gentics.cailun.core.repository.ContentRepository;
import com.gentics.cailun.core.repository.GroupRepository;
import com.gentics.cailun.core.rest.content.response.ContentResponse;
import com.gentics.cailun.core.rest.user.response.UserResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
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
	private CaiLunSpringConfiguration springConfiguration;

	@Autowired
	private ContentRepository contentRepository;

	@Autowired
	private I18NService i18n;

	public void setTeaser(Content page, Language language, String text) {
		setProperty(page, language, Content.TEASER_KEY, text);
	}

	public void setTitle(Content page, Language language, String text) {
		setProperty(page, language, Content.TITLE_KEY, text);
	}

	@Override
	public Iterable<Content> findAll(String project) {
		return contentRepository.findAll(project);
	}

	@Override
	public ContentResponse transformToRest(RoutingContext rc, Content content, List<String> languageTags, int depth) {
		ContentResponse response = new ContentResponse();
		response.setUuid(content.getUuid());
		response.setSchemaName(content.getSchemaName());
		UserResponse restUser = userService.transformToRest(content.getCreator());
		response.setAuthor(restUser);
		response.setPerms(userService.getPerms(rc, content));

		if (languageTags.size() == 0) {
			for (Translated transalated : content.getI18nTranslations()) {
				String languageTag = transalated.getLanguageTag();
				// TODO handle schema
				response.addProperty(languageTag, "name", transalated.getI18nValue().getProperty("name"));
				response.addProperty(languageTag, "filename", transalated.getI18nValue().getProperty("filename"));
				response.addProperty(languageTag, "content", transalated.getI18nValue().getProperty("content"));
				response.addProperty(languageTag, "teaser", transalated.getI18nValue().getProperty("teaser"));
			}
		} else {
			for (String languageTag : languageTags) {
				Language language = languageService.findByLanguageTag(languageTag);
				if (language == null) {
					// TODO use request locale
					throw new HttpStatusCodeErrorException(400, i18n.get(Locale.getDefault(), "error_language_not_found", languageTag));
				}
				// TODO handle schema
				response.addProperty(languageTag, "name", content.getName(language));
				response.addProperty(languageTag, "filename", content.getFilename(language));
				response.addProperty(languageTag, "content", content.getContent(language));
				response.addProperty(languageTag, "teaser", content.getTeaser(language));
			}

		}

		if (depth > 0) {
			Set<Tag> tags = neo4jTemplate.fetch(content.getTags());
			for (Tag currentTag : tags) {
				boolean hasPerm = springConfiguration.authService().hasPermission(rc.session().getLoginID(),
						new CaiLunPermission(currentTag, PermissionType.READ));
				if (hasPerm) {
					response.getTags().add(tagService.transformToRest(rc, currentTag, languageTags, depth - 1));
				}
			}
		}

		return response;

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

	public void addI18NContent(Content content, Language language, String text) {
		setProperty(content, language, Content.CONTENT_KEYWORD, text);
	}

	public void setContent(Content content, Language language, String text) {
		setProperty(content, language, Content.CONTENT_KEYWORD, text);
	}

	@Override
	public void setFilename(Content content, Language language, String filename) {
		// TODO Auto-generated method stub

	}
}
