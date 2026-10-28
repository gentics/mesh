package com.gentics.mesh.plugin.comment.model;

import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.user.UserPermissionResponse;
import com.gentics.mesh.json.JsonUtil;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class NestedCommentSection implements CommentChildren, RestModel {
	private String uuid;
	private List<NestedComment> children;
	private Fields fields;
	private PermissionInfo permissions;

	public static NestedCommentSection fromGraphQl(JsonObject json, UserPermissionResponse permissions) {
		trimChildren(json);
		nestNodeReference(json);
		return JsonUtil.readValue(json.encode(), NestedCommentSection.class).setPermissions(permissions);
	}

	/**
	 * Unpacks the children.elements structure from GraphQL to have a cleaner json output
	 */
	private static void trimChildren(JsonObject element) {
		JsonObject children = element.getJsonObject("children");
		if (children != null) {
			JsonArray array = children.getJsonArray("elements");
			element.put("children", array);
			array.stream()
				.map(obj -> (JsonObject) obj)
				.forEach(NestedCommentSection::trimChildren);
		}
	}

	/**
	 * Nests the node uuid into objects to keep the original behaviour.
	 * This change was necessary for https://jira.gentics.com/browse/SUP-8987
	 * @param json
	 */
	private static void nestNodeReference(JsonObject json) {
		JsonObject fields = json.getJsonObject("fields");
		fields.put("node", new JsonObject().put("uuid", fields.getString("node")));
	}

	public String getUuid() {
		return uuid;
	}

	public NestedCommentSection setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	@Override
	public List<NestedComment> getChildren() {
		return children;
	}

	public NestedCommentSection setChildren(List<NestedComment> children) {
		this.children = children;
		return this;
	}

	public Fields getFields() {
		return fields;
	}

	public NestedCommentSection setFields(Fields fields) {
		this.fields = fields;
		return this;
	}

	public PermissionInfo getPermissions() {
		return permissions;
	}

	private NestedCommentSection setPermissions(PermissionInfo permissions) {
		this.permissions = permissions;
		return this;
	}

	public static class Fields {
		private Node node;
		private String uuid;
		private String language;
		private Boolean allowAnonymous;
		private Boolean requiresApproval;
		private List<String> requiredForAnonymous;

		public Node getNode() {
			return node;
		}

		public Fields setNode(Node node) {
			this.node = node;
			return this;
		}

		public String getUuid() {
			return uuid;
		}

		public Fields setUuid(String uuid) {
			this.uuid = uuid;
			return this;
		}

		public String getLanguage() {
			return language;
		}

		public Fields setLanguage(String language) {
			this.language = language;
			return this;
		}

		public Boolean getAllowAnonymous() {
			return allowAnonymous;
		}

		public Fields setAllowAnonymous(Boolean allowAnonymous) {
			this.allowAnonymous = allowAnonymous;
			return this;
		}

		public Boolean getRequiresApproval() {
			return requiresApproval;
		}

		public Fields setRequiresApproval(Boolean requiresApproval) {
			this.requiresApproval = requiresApproval;
			return this;
		}

		public List<String> getRequiredForAnonymous() {
			return requiredForAnonymous;
		}

		public Fields setRequiredForAnonymous(List<String> requiredForAnonymous) {
			this.requiredForAnonymous = requiredForAnonymous;
			return this;
		}
	}

	public static class Node {
		private String uuid;

		public String getUuid() {
			return uuid;
		}

		public Node setUuid(String uuid) {
			this.uuid = uuid;
			return this;
		}
	}
}
