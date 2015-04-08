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

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.generic.GenericNode;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.google.common.collect.Lists;

@Component
@Scope("singleton")
public class Neo4jGenericContentUtils {

	@Autowired
	CaiLunSpringConfiguration configuration;

	@Autowired
	Neo4jTemplate template;

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
		Node fromNode = template.getPersistentState(from);
		for (Node node : graphDB.traversalDescription().depthFirst().relationships(BasicRelationships.TYPES.HAS_SUB_TAG)
				.uniqueness(Uniqueness.RELATIONSHIP_GLOBAL).traverse(fromNode).nodes()) {
			System.out.println(node.getId() + " " + node.getLabels());

			if (node.hasLabel(DynamicLabel.label(Content.class.getSimpleName()))) {
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
