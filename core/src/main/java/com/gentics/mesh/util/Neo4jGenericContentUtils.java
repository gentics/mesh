package com.gentics.mesh.util;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Vertex;

@Component
@Scope("singleton")
public class Neo4jGenericContentUtils {

	@Autowired
	MeshSpringConfiguration configuration;


	/**
	 * Returns the path between the given tag and the target node.
	 * 
	 * @param to
	 *            Tag which will stop the traversal when encountered
	 * @param from
	 *            Page from which the traversal will start
	 * @return
	 */
	public String getPath(Tag to, GenericNode from) {
		GraphDatabaseService graphDB = configuration.getGraphDatabaseService();
		List<String> segments = new ArrayList<>();
//		Node fromNode = template.getPersistentState(from);
		Vertex fromNode = from.asVertex();
		for (Node node : graphDB.traversalDescription().depthFirst().relationships(BasicRelationships.TYPES.HAS_TAG)
				.uniqueness(Uniqueness.RELATIONSHIP_GLOBAL).traverse(fromNode).nodes()) {
			System.out.println(node.getId() + " " + node.getLabels());

			if (node.hasLabel(DynamicLabel.label(MeshNode.class.getSimpleName()))) {
				segments.add((String) node.getProperty("filename"));
			}
			if (node.hasLabel(DynamicLabel.label(Tag.class.getSimpleName()))) {
				segments.add((String) node.getProperty("name"));
			}

			if (node.hasLabel(DynamicLabel.label(Tag.class.getSimpleName())) && node.getId() == to.getId()) {
				break;
			}
		}

		segments = Lists.reverse(segments);
		StringBuilder pathBuilder = new StringBuilder();
		pathBuilder.append("/");
		final int nSegments = segments.size();
		int i = 0;
		for (String segment : segments) {
			pathBuilder.append(segment);
			i++;
			// Skip the last slash
			if (i < nSegments) {
				pathBuilder.append("/");
			}
		}
		return pathBuilder.toString();
	}

}
