package com.gentics.cailun.core.repository;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.model.generic.GenericTag;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;
import com.gentics.cailun.core.repository.action.ProjectActions;
import com.gentics.cailun.core.repository.generic.GenericNodeRepositoryImpl;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

public class ProjectRepositoryImpl extends GenericNodeRepositoryImpl<Project> implements ProjectActions {

	private static final Logger log = LoggerFactory.getLogger(ProjectRepositoryImpl.class);

	@Autowired
	ProjectRepository projectRepository;

	@Autowired
	private Neo4jTemplate template;

	@Autowired
	private CaiLunSpringConfiguration springConfig;

	@SuppressWarnings("rawtypes")
	@Override
	public GenericFile findFileByPath(String projectName, String path) {
		if (log.isDebugEnabled()) {
			log.debug("Searching for path {" + path + "}");
		}
		String parts[] = path.split("/");
		Project project = projectRepository.findByName(projectName);
		GenericTag tag = project.getRootTag();
		if (log.isDebugEnabled()) {
			log.debug("Found {" + tag.getTags().size() + "} subtags.");
		}
		Node rootTag = template.getPersistentState(tag);

		Node currentTag = rootTag;
		Node lastTag = null;
		for (int i = 1; i < parts.length; i++) {
			if (log.isDebugEnabled()) {
				log.debug("Searching part {" + parts[i] + "}");
			}
			if (currentTag != null) {
				currentTag = findSubTagWithName(currentTag, parts[i]);
				if (currentTag != null) {
					lastTag = currentTag;
				}
				// Check for files with the given filename for the last segment of the path
				if (i == parts.length - 1) {
					Node fileNode = findFileNodeWithFilename(lastTag, parts[i]);
					// No file found
					if (fileNode == null) {
						return null;
					} else {
						return template.load(fileNode, GenericFile.class);
					}
				}
			}
		}
		return null;
	}

	/**
	 * Check the file relationships of this node and a return the node of the file that matches the given filename.
	 * 
	 * @param tagNode
	 *            node of the tag which has file relationships that could be checked
	 * @param filename
	 *            name of the file that we want to locate
	 * @return node of the file or null if no file with the given filename could be found
	 */
	private Node findFileNodeWithFilename(Node tagNode, String filename) {
		if (tagNode == null) {
			return null;
		}
		for (Relationship rel : tagNode.getRelationships(BasicRelationships.TYPES.HAS_FILE, Direction.OUTGOING)) {
			if (rel.getEndNode() == null) {
				return null;
			}
			boolean hasI18nFilenameProperty = hasNodeI18NProperty(rel.getEndNode(), "filename", filename);
			if (hasI18nFilenameProperty) {
				return rel.getEndNode();
			}
			if (rel.getEndNode().hasProperty("filename") && filename.equals(rel.getEndNode().getProperty("filename"))) {
				return rel.getEndNode();
			}
		}
		return null;

	}

	/**
	 * Locate the tag with the given name within the subset of tags of the given tag.
	 * 
	 * @param rootTag
	 * @param name
	 * @return
	 */
	private Node findSubTagWithName(Node rootTag, String name) {
		for (Relationship rel : rootTag.getRelationships(BasicRelationships.TYPES.HAS_SUB_TAG, Direction.OUTGOING)) {
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
