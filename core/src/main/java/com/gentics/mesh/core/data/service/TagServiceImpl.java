package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.data.service.transformation.tag.TagTransformationTask;
import com.gentics.mesh.core.rest.tag.response.TagResponse;
import com.gentics.mesh.paging.PagingInfo;
import com.tinkerpop.blueprints.Vertex;

@Component
public class TagServiceImpl extends AbstractMeshService implements TagService {

	private static final Logger log = LoggerFactory.getLogger(TagServiceImpl.class);

	@Autowired
	private LanguageService languageService;

	@Autowired
	private MeshNodeService nodeService;

	@Autowired
	private ProjectService projectService;


	@Autowired
	private UserService userService;

	@Autowired
	private RoutingContextService rcs;

	private static ForkJoinPool pool = new ForkJoinPool(8);

	public TagResponse transformToRest(RoutingContext rc, Tag tag) {

		TransformationInfo info = new TransformationInfo(rc);
		info.setUserService(userService);
		info.setLanguageService(languageService);
		info.setContentService(nodeService);
		info.setSpringConfiguration(springConfiguration);
//		info.setTagService(this);

		// Configuration
		List<String> languageTags = rcs.getSelectedLanguageTags(rc);
		info.setLanguageTags(languageTags);

		TagResponse restTag = new TagResponse();
		TagTransformationTask task = new TagTransformationTask(tag, info, restTag);

		pool.invoke(task);
		return restTag;
	}

	public Page<Tag> findProjectTags(RoutingContext rc, String projectName, List<String> languageTags, PagingInfo pagingInfo) {
		String userUuid = rc.session().getPrincipal().getString("uuid");
		//tagRepository.findProjectTags(userUuid, projectName, languageTags, pagingInfo);
		return null;
	}

	public Page<Tag> findTags(RoutingContext rc, String projectName, MeshNode node, List<String> languageTags, PagingInfo pagingInfo) {
		String userUuid = rc.session().getPrincipal().getString("uuid");
		//tagRepository.findTags(userUuid, projectName, node, languageTags, pagingInfo);
		return null;
	}

	public Page<MeshNode> findTaggedNodes(RoutingContext rc, String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo) {
		String userUuid = rc.session().getPrincipal().getString("uuid");
		//findTaggedNodes(userUuid, projectName, tag, languageTags, pagingInfo);
		return null;
	}

	static String PERMISSION_PATTERN_ON_TAG = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(tag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) ";
	static String PERMISSION_PATTERN_ON_NODE = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(node:MeshNode)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) ";
	static String TAG_PROJECT_PATTERN = "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
	static String USER_PERMISSION_FILTER = " requestUser.uuid = {userUuid} AND perm.`permissions-read` = true ";
	static String PROJECT_FILTER = "pr.name = {projectName} ";
	static String ROOT_TAG_FILTER = "id(rootTag) = {rootTagId} ";
	static String ORDER_BY_NAME = "ORDER BY p.`properties-name` desc";

	public static String getLanguageFilter(String field) {
		String filter = " " + field + ".languageTag IN {languageTags} ";
		return filter;
	}

	public Page<Tag> findProjectTags(String userUuid, String projectName, List<String> languageTags, PagingInfo pagingInfo) {

		//		String langFilter = getLanguageFilter("l");
		//		if (languageTags == null || languageTags.isEmpty()) {
		//			langFilter = "";
		//		} else {
		//			langFilter += " AND ";
		//		}
		//		String baseQuery = PERMISSION_PATTERN_ON_TAG;
		//		baseQuery += TAG_PROJECT_PATTERN;
		//		baseQuery += "WHERE " + langFilter + USER_PERMISSION_FILTER + "AND " + PROJECT_FILTER;
		//
		//		String query = baseQuery + " WITH p, tag " + ORDER_BY_NAME + " RETURN DISTINCT tag as n";
		//		String countQuery = baseQuery + " RETURN count(DISTINCT tag) as count";
		//
		//		Map<String, Object> parameters = new HashMap<>();
		//		parameters.put("languageTags", languageTags);
		//		parameters.put("projectName", projectName);
		//		parameters.put("userUuid", userUuid);
		//		return queryService.query(query, countQuery, parameters, pagingInfo, Tag.class);
		return null;
	}

	public Page<Tag> findTags(String userUuid, String projectName, MeshNode node, List<String> languageTags, PagingInfo pagingInfo) {
		//		String langFilter = getLanguageFilter("l");
		//		if (languageTags == null || languageTags.isEmpty()) {
		//			langFilter = "";
		//		} else {
		//			langFilter += " AND ";
		//		}
		//
		//		String baseQuery = PERMISSION_PATTERN_ON_TAG;
		//		baseQuery += TAG_PROJECT_PATTERN;
		//		baseQuery += "MATCH (node:MeshNode)-[:HAS_TAG]->(tag)-[l:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		//		baseQuery += "WHERE " + langFilter + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER;
		//
		//		String query = baseQuery + " WITH sp, tag ORDER BY sp.`properties-name` desc RETURN DISTINCT tag as n";
		//		String countQuery = baseQuery + " RETURN count(DISTINCT tag) as count";
		//
		//		Map<String, Object> parameters = new HashMap<>();
		//		parameters.put("languageTags", languageTags);
		//		parameters.put("projectName", projectName);
		//		parameters.put("userUuid", userUuid);
		//		parameters.put("node", node);
		//		return queryService.query(query, countQuery, parameters, pagingInfo, Tag.class);
		return null;
	}

	public Page<MeshNode> findTaggedNodes(String userUuid, String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo) {
		//		String langFilter = getLanguageFilter("l");
		//		if (languageTags == null || languageTags.isEmpty()) {
		//			langFilter = "";
		//		} else {
		//			langFilter += " AND ";
		//		}
		//		String baseQuery = PERMISSION_PATTERN_ON_NODE;
		//		baseQuery += "MATCH (node)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
		//		baseQuery += "MATCH (tag:Tag)-[:HAS_TAG]->(node)-[l:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		//		baseQuery += "WHERE " + langFilter + " AND " + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER;
		//
		//		String query = baseQuery + " WITH sp, node " + ORDER_BY_NAME + " RETURN DISTINCT node as n";
		//		String countQuery = baseQuery + " RETURN count(DISTINCT node) as count";
		//
		//		Map<String, Object> parameters = new HashMap<>();
		//		parameters.put("languageTags", languageTags);
		//		parameters.put("projectName", projectName);
		//		parameters.put("userUuid", userUuid);
		//		parameters.put("tag", tag);
		//		return queryService.query(query, countQuery, parameters, pagingInfo, MeshNode.class);
		return null;
	}

	@Override
	public Tag create() {
		return framedGraph.addFramedVertex(Tag.class);
	}

	@Override
	public Tag findOne(Long id) {
		Vertex vertex = framedGraph.getVertex(id);
		if (vertex != null) {
			return framedGraph.frameElement(vertex, Tag.class);
		}
		return null;
	}

	@Override
	public void delete(Tag tag) {
		tag.getVertex().remove();
	}

	@Override
	public Tag findByName(String projectName, String name) {
		//TODO filter by i18n properties
		return framedGraph.v().has("name", name).next(Tag.class);
	}

	@Override
	public Object findByUUID(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

}
