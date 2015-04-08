package com.gentics.cailun.core.data.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.I18NProperties;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.model.generic.GenericPropertyContainer;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;
import com.gentics.cailun.core.data.model.relationship.Translated;
import com.gentics.cailun.core.data.service.generic.GenericTagServiceImpl;
import com.gentics.cailun.core.repository.TagRepository;
import com.gentics.cailun.core.rest.tag.request.TagCreateRequest;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.path.PagingInfo;
import com.gentics.cailun.path.Path;
import com.gentics.cailun.path.PathSegment;
import com.google.common.collect.Lists;

@Component
@Transactional
public class TagServiceImpl extends GenericTagServiceImpl<Tag, GenericFile> implements TagService {

	private static final Logger log = LoggerFactory.getLogger(TagServiceImpl.class);

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private LanguageService languageService;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private UserService userService;

	@Override
	public Path findByProjectPath(String projectName, String path) {
		String parts[] = path.split("/");
		Project project = projectService.findByName(projectName);

		Path tagPath = new Path();

		// Traverse the graph and buildup the result path while doing so
		Node currentNode = neo4jTemplate.getPersistentState(project.getRootTag());
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			if (log.isDebugEnabled()) {
				log.debug("Looking for path segment {" + part + "}");
			}
			Node nextNode = addPathSegment(tagPath, currentNode, part);
			if (nextNode != null) {
				currentNode = nextNode;
			} else {
				currentNode = null;
				break;
			}
		}

		return tagPath;

	}

	/**
	 * Find the next sub tag that has a name with the given value.
	 * 
	 * @param path
	 *            Path to which new segments should be added
	 * @param node
	 *            start node
	 * @param i18nTagName
	 *            Name of the tag which should be looked up
	 * @return Found node or null if no node could be found
	 */
	private Node addPathSegment(Path path, Node node, String i18nTagName) {
		if (node == null) {
			return null;
		}
		AtomicReference<Node> foundNode = new AtomicReference<>();
		// TODO i wonder whether streams are useful in this case. We need to benchmark this section
		Lists.newArrayList(node.getRelationships(BasicRelationships.TYPES.HAS_SUB_TAG, Direction.OUTGOING)).stream().forEach(rel -> {
			Node nextHop = rel.getEndNode();
			if (nextHop.hasLabel(Tag.getLabel())) {
				String languageTag = getI18nPropertyLanguageTag(nextHop, GenericPropertyContainer.NAME_KEYWORD, i18nTagName);
				if (languageTag != null) {
					foundNode.set(nextHop);
					path.addSegment(new PathSegment(nextHop, languageTag));
					return;
				}
			}
		});

		return foundNode.get();
	}

	/**
	 * Check whether the given node has a i18n property with the given value for the specified key.
	 * 
	 * @param node
	 * @param key
	 * @param value
	 * @return The found language tag, otherwise null
	 */
	private String getI18nPropertyLanguageTag(Node node, String key, String value) {
		if (StringUtils.isEmpty(key)) {
			return null;
		}
		key = "properties-" + key;
		for (Relationship rel : node.getRelationships(BasicRelationships.TYPES.HAS_I18N_PROPERTIES, Direction.OUTGOING)) {
			String languageTag = (String) rel.getProperty("languageTag");
			Node i18nPropertiesNode = rel.getEndNode();
			if (i18nPropertiesNode.hasProperty(key)) {
				String i18nValue = (String) i18nPropertiesNode.getProperty(key);
				if (i18nValue.equals(value)) {
					return languageTag;
				}
			}
		}
		return null;
	}

	@Override
	public TagResponse transformToRest(Tag tag, List<String> languageTags) {
		TagResponse response = new TagResponse();

		for (String languageTag : languageTags) {
			Language language = languageService.findByLanguageTag(languageTag);
			if (language == null) {
				// TODO should we just omit the language or abort?
				log.error("No language found for language tag {" + languageTag + "}. Skipping lanuage.");
				continue;
			}
			// TODO tags can also be dynamically enhanced. Maybe we should check the schema here? This would be costly. Currently we are just returning all
			// found i18n properties for the language.

			// Add all i18n properties for the selected language to the response
			I18NProperties i18nProperties = tag.getI18NProperties(language);
			if (i18nProperties != null) {
				for (String key : i18nProperties.getProperties().getPropertyKeys()) {
					response.addProperty(languageTag, key, i18nProperties.getProperty(key));
				}
			} else {
				log.error("Could not find any i18n properties for language {" + languageTag + "}. Skipping language.");
				continue;
			}
		}
		response.setUuid(tag.getUuid());
		response.setSchemaName(tag.getSchemaName());

		// TODO handle files and subtags:
		// tag.getTags()
		// tag.getFiles()
		response.setCreator(userService.transformToRest(tag.getCreator()));
		// TODO handle properties for the type of tag
		return response;

	}

	@Override
	public Page<Tag> findAllVisible(User requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo) {
		PageRequest pr = new PageRequest(pagingInfo.getPage(), pagingInfo.getPerPage());
		if (languageTags == null || languageTags.size() == 0) {
			return tagRepository.findAll(requestUser, projectName, pr);
		} else {
			return tagRepository.findAll(requestUser, projectName, languageTags, pr);
		}
	}

}
