package com.gentics.cailun.core.rest.service.generic;

import java.util.concurrent.atomic.AtomicReference;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.generic.GenericFile;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.google.common.collect.Lists;

@Component
@Transactional
public class GenericFileServiceImpl<T extends GenericFile> extends GenericPropertyContainerServiceImpl<T> implements GenericFileService<T> {

	@Autowired
	private CaiLunSpringConfiguration springConfig;

	public void setFilename(T file, Language language, String filename) throws UnsupportedOperationException {
		// TODO check for conflicting i18n filenames
		setProperty(file, language, GenericFile.FILENAME_KEYWORD, filename);
	}

	@Override
	public T findByProject(String projectName, String path) {
		GraphDatabaseService graphDb = springConfig.getGraphDatabaseService();

		String parts[] = path.split("/");
		// Tag rootTag = tagRepository.findRootTag();
		try (Transaction tx = graphDb.beginTx()) {
			// Node currentNode = graphDb.getNodeById(rootTag.getId());
			Node currentNode = null;
			for (int i = 0; i < parts.length - 1; i++) {
				String part = parts[i];
				Node nextNode = getChildNodeTagFromNodeTag(currentNode, part);
				if (nextNode != null) {
					currentNode = nextNode;
				} else {
					currentNode = null;
					break;
				}
			}
			if (currentNode != null) {
				// Finally search for the page and assume the last part of the request as filename
				Node pageNode = getChildNodePageFromNodeTag(currentNode, parts[parts.length - 1]);
				if (pageNode != null) {
					// return pageNode.getId();
					return null;
				} else {
					return null;
				}
			}
		}

		System.out.println("looking for " + path + " in project " + projectName);
		return null;
	}

	private Node getChildNodePageFromNodeTag(Node node, String pageFilename) {
		AtomicReference<Node> foundNode = new AtomicReference<>();
		Lists.newArrayList(node.getRelationships()).stream().filter(rel -> "TAGGED".equalsIgnoreCase(rel.getType().name())).forEach(rel -> {
			Node nextHop = rel.getStartNode();
			if (nextHop.hasLabel(DynamicLabel.label("Page"))) {
				String currentName = (String) nextHop.getProperty("filename");
				if (pageFilename.equalsIgnoreCase(currentName)) {
					foundNode.set(nextHop);
					return;
				}
			}
		});
		return foundNode.get();

	}

	private Node getChildNodeTagFromNodeTag(Node node, String tagName) {
		AtomicReference<Node> foundNode = new AtomicReference<>();
		Lists.newArrayList(node.getRelationships()).stream().filter(rel -> "TAGGED".equalsIgnoreCase(rel.getType().name())).forEach(rel -> {
			Node nextHop = rel.getEndNode();
			if (nextHop.hasLabel(DynamicLabel.label("Tag"))) {
				String currentName = (String) nextHop.getProperty("name");
				if (tagName.equalsIgnoreCase(currentName)) {
					foundNode.set(nextHop);
					return;
				}
			}
		});

		return foundNode.get();
	}
}
