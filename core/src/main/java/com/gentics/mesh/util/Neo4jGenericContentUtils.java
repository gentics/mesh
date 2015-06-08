package com.gentics.mesh.util;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class Neo4jGenericContentUtils {

//	@Autowired
//	MeshSpringConfiguration configuration;
//
//
//	/**
//	 * Returns the path between the given tag and the target node.
//	 * 
//	 * @param to
//	 *            Tag which will stop the traversal when encountered
//	 * @param from
//	 *            Page from which the traversal will start
//	 * @return
//	 */
//	public String getPath(Tag to, GenericNode from) {
//		fra
//		GraphDatabaseService graphDB = configuration.getGraphDatabaseService();
//		List<String> segments = new ArrayList<>();
////		Node fromNode = template.getPersistentState(from);
//		Vertex fromNode = from.asVertex();
//		for (Node node : graphDB.traversalDescription().depthFirst().relationships(BasicRelationships.TYPES.HAS_TAG)
//				.uniqueness(Uniqueness.RELATIONSHIP_GLOBAL).traverse(fromNode).nodes()) {
//			System.out.println(node.getId() + " " + node.getLabels());
//
//			if (node.hasLabel(DynamicLabel.label(MeshNode.class.getSimpleName()))) {
//				segments.add((String) node.getProperty("filename"));
//			}
//			if (node.hasLabel(DynamicLabel.label(Tag.class.getSimpleName()))) {
//				segments.add((String) node.getProperty("name"));
//			}
//
//			if (node.hasLabel(DynamicLabel.label(Tag.class.getSimpleName())) && node.getId() == to.getId()) {
//				break;
//			}
//		}
//
//		segments = Lists.reverse(segments);
//		StringBuilder pathBuilder = new StringBuilder();
//		pathBuilder.append("/");
//		final int nSegments = segments.size();
//		int i = 0;
//		for (String segment : segments) {
//			pathBuilder.append(segment);
//			i++;
//			// Skip the last slash
//			if (i < nSegments) {
//				pathBuilder.append("/");
//			}
//		}
//		return pathBuilder.toString();
//	}

}
