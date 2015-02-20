package com.gentics.cailun.core.repository;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.cailun.core.repository.action.ProjectActions;
import com.gentics.cailun.core.repository.generic.GenericNodeRepositoryImpl;
import com.gentics.cailun.core.rest.model.Project;
import com.gentics.cailun.core.rest.model.generic.GenericFile;
import com.gentics.cailun.core.rest.model.generic.GenericTag;
import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

public class ProjectRepositoryImpl extends GenericNodeRepositoryImpl<Project> implements ProjectActions {

	private static final Logger log = LoggerFactory.getLogger(ProjectRepositoryImpl.class);

	@Autowired
	ProjectRepository projectRepository;

	@Autowired
	private Neo4jTemplate template;

	@Autowired
	private CaiLunSpringConfiguration springConfig;

	@Override
	public GenericFile findFileByPath(String projectName, String path) {
		System.out.println("Searchign for path {" + path + "}");
		String parts[] = path.split("/");
		Project project = projectRepository.findByName(projectName);
		GenericTag tag = project.getRootTag();
		System.out.println(tag.getTags().size());
		GraphDatabaseService db = springConfig.getGraphDatabaseService();
		Node rootTag = template.getPersistentState(tag);

		Node currentTag = rootTag;
		Node lastTag = null;
		for (int i = 1; i < parts.length; i++) {
			System.out.println("searching part {" + parts[i] + "}");
			if (currentTag != null) {
				currentTag = findSubTagWithName(currentTag, parts[i]);
				if (currentTag != null) {
					lastTag = currentTag;
				}
				// Check for files with the given filename for the last segment of the path
				if (i == parts.length - 1) {
					Node fileNode = findFileNodeWithFilename(lastTag, parts[i]);
					return template.load(fileNode, GenericFile.class);
				}
			}
		}

		// TraversalDescription tagTraversal = db.traversalDescription().depthFirst().relationships(BasicRelationships.TYPES.HAS_SUB_TAG)
		// .evaluator(includeWhereI18NameIs(parts)).uniqueness(Uniqueness.RELATIONSHIP_GLOBAL);
		// for (Path graphPath : tagTraversal.traverse(rootTag)) {
		// System.out.println(graphPath);
		// // System.out.println(rel.getStartNode() + " " + rel.getType() + " " + rel.getEndNode());
		// }
		return null;
		// for (Relationship rel : graphDb.traversalDescription().depthFirst().relationships(AuthRelationships.TYPES.MEMBER_OF, Direction.OUTGOING)
		// .relationships(AuthRelationships.TYPES.HAS_ROLE, Direction.INCOMING)
		// .relationships(AuthRelationships.TYPES.HAS_PERMISSION, Direction.OUTGOING).uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
		// .traverse(userNode).relationships()) {

	}

	private Node findFileNodeWithFilename(Node currentTag, String name) {
		for (Relationship rel : currentTag.getRelationships(BasicRelationships.TYPES.HAS_FILE, Direction.OUTGOING)) {
			if (rel.getEndNode() == null) {
				return null;
			}
			boolean hasI18nFilenameProperty = hasNodeI18NProperty(rel.getEndNode(), "filename", name);
			if (hasI18nFilenameProperty) {
				return rel.getEndNode();
			}
			if (rel.getEndNode().hasProperty("filename") && name.equals(rel.getEndNode().getProperty("filename"))) {
				return rel.getEndNode();
			}
		}
		return null;

	}

	private Node findSubTagWithName(Node rootTag, String name) {
		for (Relationship rel : rootTag.getRelationships(BasicRelationships.TYPES.HAS_SUB_TAG, Direction.OUTGOING)) {
			Node endNode = rel.getEndNode();
			if (endNode != null && hasNodeI18NProperty(endNode, "name", name)) {
				return endNode;
			}
		}
		return null;

	}

	private boolean hasNodeI18NProperty(Node node, String key, String value) {
		for (Relationship rel : node.getRelationships(BasicRelationships.TYPES.HAS_I18N_PROPERTIES, Direction.OUTGOING)) {
			if (rel.getEndNode() != null && value.equals(rel.getEndNode().getProperty("properties-" + key))) {
				return true;
			}
		}
		return false;
	}

	// public static PathEvaluator<Integer> includeWhereI18NameIs(final String[] parts) {
	// return new PathEvaluator.Adapter<Integer>() {
	//
	// @Override
	// public Evaluation evaluate(Path path, BranchState<Integer> state) {
	// if (state.getState() == null) {
	// state.setState(0);
	// }
	// // System.out.println(path.endNode().getLabels());
	// if (path.endNode().hasLabel(DynamicLabel.label(GenericTag.class.getSimpleName()))) {
	// // System.out.println("is Tag");
	// Iterable<Relationship> i18NRelationships = path.endNode().getRelationships(BasicRelationships.TYPES.HAS_I18N_PROPERTIES,
	// Direction.OUTGOING);
	// for (Relationship i18NRelationship : i18NRelationships) {
	// Node i18NPropertiesNode = i18NRelationship.getEndNode();
	// if (parts[state.getState()].equals(i18NPropertiesNode.getProperty("properties-name"))) {
	// log.debug("Found matching i18n name for given node. Continuing traversal.");
	// return Evaluation.INCLUDE_AND_CONTINUE;
	// } else {
	// return Evaluation.EXCLUDE_AND_CONTINUE;
	// }
	// // for (String key : i18NPropertiesNode.getPropertyKeys()) {
	// // System.out.println(key);
	// // }
	// }
	// return Evaluation.EXCLUDE_AND_CONTINUE;
	// }
	//
	// if (path.endNode().hasLabel(DynamicLabel.label(GenericFile.class.getSimpleName()))) {
	// System.out.println("check file");
	// return Evaluation.INCLUDE_AND_CONTINUE;
	// }
	//
	// // Only traverse tags and files
	// return Evaluation.EXCLUDE_AND_CONTINUE;
	// }
	// };
	// }
}
