package com.gentics.cailun.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;
import com.gentics.cailun.core.data.service.content.TransformationInfo;
import com.gentics.cailun.core.data.service.generic.GenericPropertyContainerServiceImpl;
import com.gentics.cailun.core.data.service.tag.TagTransformationTask;
import com.gentics.cailun.core.repository.TagRepository;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.paging.CaiLunPageRequest;
import com.gentics.cailun.paging.PagingInfo;
import com.gentics.cailun.path.Path;
import com.gentics.cailun.path.PathSegment;
import com.google.common.collect.Lists;

@Component
@Transactional(readOnly = true)
public class TagServiceImpl extends GenericPropertyContainerServiceImpl<Tag> implements TagService {

	private static final Logger log = LoggerFactory.getLogger(TagServiceImpl.class);

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private LanguageService languageService;

	@Autowired
	private ContentService contentService;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private CaiLunSpringConfiguration springConfiguration;

	@Autowired
	private GraphDatabaseService graphDb;

	@Autowired
	private UserService userService;

	private static ForkJoinPool pool = new ForkJoinPool(8);

	@Override
	public Path findByProjectPath(String projectName, String path) {
		String parts[] = path.split("/");
		Project project = projectService.findByName(projectName);

		Path tagPath = new Path();

		// Traverse the graph and buildup the result path while doing so
		Node currentNode = neo4jTemplate.getPersistentState(project.getRootTag());
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			if (log.isDebugEnabled()) {
				log.debug("Looking for path segment {" + part + "}");
			}
			Node nextNode = addPathSegment(tagPath, currentNode, part);
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
	 * @return Found node or null if no node could be found
	 */
	private Node addPathSegment(Path path, Node node, String i18nTagName) {
		if (node == null) {
			return null;
		}
		AtomicReference<Node> foundNode = new AtomicReference<>();
		// TODO i wonder whether streams are useful in this case. We need to benchmark this section
		Lists.newArrayList(node.getRelationships(BasicRelationships.TYPES.HAS_SUB_TAG, Direction.OUTGOING)).stream().forEach(rel -> {
			Node nextHop = rel.getEndNode();
			if (nextHop.hasLabel(Tag.getLabel())) {
				String languageTag = getI18nPropertyLanguageTag(nextHop, ObjectSchema.NAME_KEYWORD, i18nTagName);
				if (languageTag != null) {
					foundNode.set(nextHop);
					path.addSegment(new PathSegment(nextHop, languageTag));
					return;
				}
			}
		});

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

	@Override
	public TagResponse transformToRest(RoutingContext rc, Tag tag, List<String> languageTags, int depth) {

		TransformationInfo info = new TransformationInfo(rc, depth, languageTags);
		info.setUserService(userService);
		info.setLanguageService(languageService);
		info.setGraphDb(graphDb);
		info.setContentService(contentService);
		info.setSpringConfiguration(springConfiguration);
		info.setTagService(this);
		info.setNeo4jTemplate(neo4jTemplate);

		TagResponse restTag = new TagResponse();
		TagTransformationTask task = new TagTransformationTask(tag, info, restTag);

		pool.invoke(task);
		return restTag;
	}

	@Override
	public Page<Tag> findAllVisible(RoutingContext rc, String projectName, List<String> languageTags, PagingInfo pagingInfo) {
		CaiLunPageRequest pr = new CaiLunPageRequest(pagingInfo);
		String userUuid = rc.session().getPrincipal().getString("uuid");
		if (languageTags == null || languageTags.size() == 0) {
			return tagRepository.findAll(userUuid, projectName, pr);
		} else {
			return tagRepository.findAll(userUuid, projectName, languageTags, pr);
		}
	}

	@Override
	public Page<Tag> findAllVisibleSubTags(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo) {
		CaiLunPageRequest pr = new CaiLunPageRequest(pagingInfo);
		String userUuid = rc.session().getPrincipal().getString("uuid");

		if (languageTags == null || languageTags.size() == 0) {
			return tagRepository.findAllSubTags(userUuid, projectName, rootTag, pr);
		} else {
			return tagRepository.findAllSubTags(userUuid, projectName, rootTag, languageTags, pr);
		}
	}

	@Override
	public Page<Content> findAllVisibleSubContents(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags,
			PagingInfo pagingInfo) {
		CaiLunPageRequest pr = new CaiLunPageRequest(pagingInfo);
		String userUuid = rc.session().getPrincipal().getString("uuid");

		if (languageTags == null || languageTags.size() == 0) {
			return tagRepository.findAllSubContents(userUuid, projectName, rootTag, pr);
		} else {
			return tagRepository.findAllSubContents(userUuid, projectName, rootTag, languageTags, pr);
		}
	}

}
