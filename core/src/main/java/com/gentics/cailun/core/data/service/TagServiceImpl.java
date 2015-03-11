package com.gentics.cailun.core.data.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.I18NProperties;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.model.generic.GenericPropertyContainer;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;
import com.gentics.cailun.core.data.service.generic.GenericTagServiceImpl;
import com.gentics.cailun.core.repository.TagRepository;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
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

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@Override
	public Tag findByProjectPath(String projectName, String path) {
		String parts[] = path.split("/");

		Project project = projectService.findByName(projectName);

		// try (Transaction tx = graphDb.beginTx()) {
		Node currentNode = neo4jTemplate.getPersistentState(project.getRootTag());
		// Node currentNode = graphDb.getNodeById(rootTag.getId());
		for (int i = 0; i < parts.length - 1; i++) {
			String part = parts[i];
			System.out.println("Looking for part " + part);
			Node nextNode = getChildNodeTagFromNodeTag(currentNode, part);
			if (nextNode != null) {
				currentNode = nextNode;
			} else {
				currentNode = null;
				break;
			}
		}
		return neo4jTemplate.projectTo(currentNode, Tag.class);

	}

	/**
	 * Find the next sub tag that has a name with the given value.
	 * 
	 * @param node
	 *            start node
	 * @param tagName
	 *            Name of the tag which should be looked up
	 * @return Found node or null if no node could be found
	 */
	private Node getChildNodeTagFromNodeTag(Node node, String tagName) {
		if (node == null) {
			return null;
		}
		AtomicReference<Node> foundNode = new AtomicReference<>();
		// TODO i wonder whether streams are useful in this case. We need to benchmark this section
		Lists.newArrayList(node.getRelationships(BasicRelationships.TYPES.HAS_SUB_TAG, Direction.OUTGOING)).stream().forEach(rel -> {
			Node nextHop = rel.getEndNode();
			if (nextHop.hasLabel(Tag.getLabel())) {
				if (hasI18nProperty(nextHop, GenericPropertyContainer.NAME_KEYWORD, tagName)) {
					foundNode.set(nextHop);
					return;
				}
			}
		});

		return foundNode.get();
	}

	/**
	 * Check whether the given node has a i18n property with the given value for the specified key
	 * 
	 * @param node
	 * @param key
	 * @param value
	 * @return true if the i18n property with the given value was found. Otherwise false.
	 */
	private boolean hasI18nProperty(Node node, String key, String value) {
		if (StringUtils.isEmpty(key)) {
			return false;
		}
		key = "properties-" + key;
		for (Relationship rel : node.getRelationships(BasicRelationships.TYPES.HAS_I18N_PROPERTIES, Direction.OUTGOING)) {
			Node i18nPropertiesNode = rel.getEndNode();
			if (i18nPropertiesNode.hasProperty(key)) {
				String i18nValue = (String) i18nPropertiesNode.getProperty(key);
				if (i18nValue.equals(value)) {
					return true;
				}
			}
		}
		return false;
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

		// TODO handle files and subtags:
		// tag.getTags()
		// tag.getFiles()
		response.setCreator(userService.transformToRest(tag.getCreator()));
		// TODO handle properties for the type of tag
		return response;

	}
}
