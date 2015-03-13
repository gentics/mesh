package com.gentics.cailun.core.data.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.service.generic.GenericContentServiceImpl;
import com.gentics.cailun.core.rest.content.response.ContentResponse;
import com.gentics.cailun.core.rest.user.response.UserResponse;

@Component
@Transactional
public class ContentServiceImpl extends GenericContentServiceImpl<Content> implements ContentService {

	@Autowired
	LanguageService languageService;

	@Autowired
	ProjectService projectService;

	@Autowired
	UserService userService;

	@Autowired
	ObjectSchemaService objectSchemaService;

	public void setTeaser(Content page, Language language, String text) {
		setProperty(page, language, Content.TEASER_KEY, text);
	}

	public void setTitle(Content page, Language language, String text) {
		setProperty(page, language, Content.TITLE_KEY, text);
	}

	@Override
	public ContentResponse transformToRest(Content content, List<String> languages) {
		if (languages.size() == 0) {
			// TODO return page with all languages?
			return null;
		}
		// Return page with flatted properties since only one language has been specified
		else if (languages.size() == 1) {
			String languageTag = languages.iterator().next();
			Language language = languageService.findByLanguageTag(languageTag);
			if (language == null) {
				// TODO fail early
			}

			ContentResponse response = new ContentResponse();
			response.setUuid(content.getUuid());
			response.setType(content.getType());
			response.addProperty(languageTag, "name", content.getName(language));
			response.addProperty(languageTag, "filename", content.getFilename(language));
			UserResponse restUser = userService.transformToRest(content.getCreator());
			response.setAuthor(restUser);
			response.addProperty(languageTag, "content", content.getContent(language));
			response.addProperty(languageTag, "teaser", content.getTeaser(language));
			return response;
		} else {
			// TODO return all languages

			return null;
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

	@Override
	public Content save(String projectName, String path, ContentResponse requestModel) {

		// TODO check permissions
		if (requestModel.getUUID() == null) {
			Project project = projectService.findByName(projectName);
			// Language language = languageService.findByLanguageTag(requestModel.getLanguageTag());
			Language language = null;
			// TODO save given languages individually, TODO how can we delete a single language?
			if (language == null || requestModel.getType() == null) {
				// TODO handle this case
				throw new NullPointerException("No language or type specified");
			}

			// We need to validate the saved data using the object schema
			ObjectSchema objectSchema = objectSchemaService.findByName(projectName, requestModel.getType());
			if (objectSchema == null) {
				// TODO handle this case
				throw new NullPointerException("Could not find object schema for type {" + requestModel.getType() + "} and project {" + projectName
						+ "}");
			}

			// TODO handle types , verify that type exists
			Content content = new Content();
			content.setProject(project);
			content.setType(requestModel.getType());
			// for (Entry<String, String> entry : requestModel.getProperties().entrySet()) {
			// PropertyTypeSchema propertyTypeSchema = objectSchema.getPropertyTypeSchema(entry.getKey());
			// // TODO we should abort when we encounter a property with an unknown key.
			// // Determine whether the property is an i18n one or not
			// if (propertyTypeSchema == null) {
			// content.setProperty(entry.getKey(), entry.getValue());
			// } else if (propertyTypeSchema.getType().equals(PropertyType.I18N_STRING)) {
			// setProperty(content, language, entry.getKey(), entry.getValue());
			// } else {
			// // TODO handle this case
			// }
			// }
			return save(content);

		} else {

		}
		return null;
	}
}
