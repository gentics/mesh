package com.gentics.cailun.core.data.service.generic;

import java.util.concurrent.atomic.AtomicReference;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.repository.generic.GenericFileRepository;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.google.common.collect.Lists;

@Component
@Transactional
public class GenericFileServiceImpl<T extends GenericFile> extends GenericPropertyContainerServiceImpl<T> implements GenericFileService<T> {

	@Autowired
	private CaiLunSpringConfiguration springConfig;

	// @Autowired
	// private ProjectService projectService;

	@Autowired
	private GenericFileRepository<T> fileRepository;

	@Autowired
	private Neo4jTemplate template;

	public void setFilename(T file, Language language, String filename) throws UnsupportedOperationException {
		// TODO check for conflicting i18n filenames
		setProperty(file, language, GenericFile.FILENAME_KEYWORD, filename);
	}

	@Override
	public T findByProject(String projectName, String path) {
		// Delegate to project repository since this request is project specific
		return (T) fileRepository.findByProjectPath(projectName, path);
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
