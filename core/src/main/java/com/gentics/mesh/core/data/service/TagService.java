package com.gentics.mesh.core.data.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.tinkerpop.MeshShiroUser;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.paging.PagingInfo;
import com.tinkerpop.blueprints.Vertex;

@Component
public class TagService extends AbstractMeshService {

	private static final Logger log = LoggerFactory.getLogger(TagService.class);

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

//	private static ForkJoinPool pool = new ForkJoinPool(8);

	public Page<Tag> findProjectTags(MeshShiroUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo) {

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

//	static String PERMISSION_PATTERN_ON_TAG = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(tag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) ";
//	static String PERMISSION_PATTERN_ON_NODE = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(node:MeshNode)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) ";
//	static String TAG_PROJECT_PATTERN = "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
//	static String USER_PERMISSION_FILTER = " requestUser.uuid = {userUuid} AND perm.`permissions-read` = true ";
//	static String PROJECT_FILTER = "pr.name = {projectName} ";
//	static String ROOT_TAG_FILTER = "id(rootTag) = {rootTagId} ";
//	static String ORDER_BY_NAME = "ORDER BY p.`properties-name` desc";

//	public static String getLanguageFilter(String field) {
//		String filter = " " + field + ".languageTag IN {languageTags} ";
//		return filter;
//	}

	public Tag create() {
		return fg.addFramedVertex(Tag.class);
	}

	public Tag findOne(Long id) {
		Vertex vertex = fg.getVertex(id);
		if (vertex != null) {
			return fg.frameElement(vertex, Tag.class);
		}
		return null;
	}

	public Tag findByName(String projectName, String name) {
		//TODO filter by i18n properties, projectname
		return fg.v().has("name", name).nextOrDefault(Tag.class, null);
	}

	public Tag findByUUID(String uuid) {
		return fg.v().has("uuid", uuid).nextOrDefault(Tag.class, null);
	}

}
