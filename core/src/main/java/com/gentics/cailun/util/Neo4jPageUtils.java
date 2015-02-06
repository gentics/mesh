package com.gentics.cailun.util;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.rest.model.BasicRelationships;
import com.gentics.cailun.core.rest.model.GenericNode;
import com.gentics.cailun.core.rest.model.Tag;
import com.gentics.cailun.etc.Neo4jSpringConfiguration;
import com.google.common.collect.Lists;

@Component
@Scope("singleton")
public class Neo4jPageUtils {

	@Autowired
	Neo4jSpringConfiguration configuration;

	public String getPath(Tag from, GenericNode to) {
		GraphDatabaseService graphDB = configuration.getGraphDatabaseService();
		// @Query("MATCH (page:Page),(tag:Tag { name:'/' }), p = shortestPath((tag)-[:TAGGED]-(page)) WHERE id(page) = {0} WITH page, reduce(a='', n IN FILTER(x in nodes(p) WHERE id(page)<> id(x))| a + \"/\"+ n.name) as path return substring(path,2,length(path)) + \"/\" + page.filename")
		Neo4jTemplate template = new Neo4jTemplate(graphDB);
		Node toNode = template.getPersistentState(to);
		List<String> segments = new ArrayList<>();
		for (Node node : graphDB.traversalDescription().depthFirst().relationships(BasicRelationships.TYPES.TAGGED)
				.uniqueness(Uniqueness.RELATIONSHIP_GLOBAL).traverse(toNode).nodes()) {
			if (node.hasLabel(DynamicLabel.label("Page"))) {
				segments.add((String) node.getProperty("filename"));
			}
			if (node.hasLabel(DynamicLabel.label("Tag"))) {
				segments.add((String) node.getProperty("name"));
			}

			if (node.hasLabel(DynamicLabel.label("Tag")) && node.getId() == from.getId()) {
				break;
			}
			// System.out.println(node.getId() + ":" + node.getLabels());
			// System.out.println(rel.getStartNode() + ":" + rel.getStartNode().getId() + "|" + rel.getStartNode().getLabels() + "-[r:"
			// + rel.getType().name() + "]-" + rel.getEndNode() + ":" + rel.getEndNode().getId() + "|" + rel.getEndNode().getLabels());
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
