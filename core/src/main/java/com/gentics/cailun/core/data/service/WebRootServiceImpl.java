package com.gentics.cailun.core.data.service;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;
import com.gentics.cailun.path.Path;
import com.gentics.cailun.path.PathSegment;

@Component
@Transactional(readOnly = true)
public class WebRootServiceImpl implements WebRootService {

	private static Logger log = LoggerFactory.getLogger(WebRootServiceImpl.class);

	@Autowired
	private ProjectService projectService;

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@Override
	public Path findByProjectPath(String projectName, String path) {
		String parts[] = path.split("/");
		Project project = projectService.findByName(projectName);

		Path tagPath = new Path();

		// Traverse the graph and buildup the result path while doing so
		Node currentNode = neo4jTemplate.getPersistentState(project.getRootTag());
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];

			boolean isLastSegment = i == parts.length - 1;

			if (log.isDebugEnabled()) {
				log.debug("Looking for path segment {" + part + "}");
			}
			Node nextNode = addPathSegment(tagPath, currentNode, part, isLastSegment);
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
	 * @param isLastSegment
	 * @return Found node or null if no node could be found
	 */
	private Node addPathSegment(Path path, Node node, String i18nTagName, boolean isLastSegment) {
		if (node == null) {
			return null;
		}
		AtomicReference<Node> foundNode = new AtomicReference<>();
		// TODO i wonder whether streams are useful in this case. We need to benchmark this section

		RelationshipType targetRelationship = BasicRelationships.TYPES.HAS_TAG;
		String keyword = ObjectSchema.NAME_KEYWORD;
		if (isLastSegment) {
			targetRelationship = BasicRelationships.TYPES.HAS_CONTENT;
			keyword = ObjectSchema.FILENAME_KEYWORD;
		}

		for (Relationship rel : node.getRelationships(targetRelationship, Direction.OUTGOING)) {
			Node nextHop = rel.getEndNode();
			// if (nextHop.hasLabel(Tag.getLabel())) {
			String languageTag = getI18nPropertyLanguageTag(nextHop, keyword, i18nTagName);
			if (languageTag != null) {
				foundNode.set(nextHop);
				path.addSegment(new PathSegment(nextHop, languageTag));
				break;
			}
			// }
		}

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

	// public Content findFileByPath(String projectName, String path) {
	// if (log.isDebugEnabled()) {
	// log.debug("Searching for path {" + path + "}");
	// }
	// String parts[] = path.split("/");
	// Project project = projectRepository.findByName(projectName);
	// Tag tag = project.getRootTag();
	// if (log.isDebugEnabled()) {
	// log.debug("Found {" + tag.getTags().size() + "} subtags.");
	// }
	// Node rootTag = neo4jTemplate.getPersistentState(tag);
	//
	// Node currentTag = rootTag;
	// Node lastTag = null;
	// for (int i = 1; i < parts.length; i++) {
	// if (log.isDebugEnabled()) {
	// log.debug("Searching part {" + parts[i] + "}");
	// }
	// if (currentTag != null) {
	// currentTag = findSubTagWithName(currentTag, parts[i]);
	// if (currentTag != null) {
	// lastTag = currentTag;
	// }
	// // Check for files with the given filename for the last segment of the path
	// if (i == parts.length - 1) {
	// Node fileNode = findFileNodeWithFilename(lastTag, parts[i]);
	// // No file found
	// if (fileNode == null) {
	// return null;
	// } else {
	// return neo4jTemplate.load(fileNode, Content.class);
	// }
	// }
	// }
	// }
	// return null;
	// }

	// /**
	// * Check the file relationships of this node and a return the node of the file that matches the given filename.
	// *
	// * @param tagNode
	// * node of the tag which has file relationships that could be checked
	// * @param filename
	// * name of the file that we want to locate
	// * @return node of the file or null if no file with the given filename could be found
	// */
	// private Node findFileNodeWithFilename(Node tagNode, String filename) {
	// if (tagNode == null) {
	// return null;
	// }
	// for (Relationship rel : tagNode.getRelationships(BasicRelationships.TYPES.HAS_FILE, Direction.OUTGOING)) {
	// if (rel.getEndNode() == null) {
	// return null;
	// }
	// boolean hasI18nFilenameProperty = hasNodeI18NProperty(rel.getEndNode(), "filename", filename);
	// if (hasI18nFilenameProperty) {
	// return rel.getEndNode();
	// }
	// if (rel.getEndNode().hasProperty("filename") && filename.equals(rel.getEndNode().getProperty("filename"))) {
	// return rel.getEndNode();
	// }
	// }
	// return null;
	//
	// }
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

	/**
	 * Locate the tag with the given name within the subset of tags of the given tag.
	 * 
	 * @param rootTag
	 * @param name
	 * @return
	 */
	private Node findSubTagWithName(Node rootTag, String name) {
		for (Relationship rel : rootTag.getRelationships(BasicRelationships.TYPES.HAS_TAG, Direction.OUTGOING)) {
			Node endNode = rel.getEndNode();
			if (endNode != null && hasNodeI18NProperty(endNode, "name", name)) {
				return endNode;
			}
		}
		return null;

	}

	/**
	 * Check whether the given node has an i18n property of the given name.
	 * 
	 * @param node
	 *            node which has i18n property relationships that will be checked
	 * @param key
	 *            property key
	 * @param value
	 *            property value
	 * @return true when the property could be found. Otherwise false.
	 */
	private boolean hasNodeI18NProperty(Node node, String key, String value) {
		for (Relationship rel : node.getRelationships(BasicRelationships.TYPES.HAS_I18N_PROPERTIES, Direction.OUTGOING)) {
			if (rel.getEndNode() != null && value.equals(rel.getEndNode().getProperty("properties-" + key))) {
				return true;
			}
		}
		return false;
	}
}
