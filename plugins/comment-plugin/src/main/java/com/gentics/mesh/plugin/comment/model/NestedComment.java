package com.gentics.mesh.plugin.comment.model;

import java.util.List;

public class NestedComment implements CommentChildren {
	private String uuid;
	private String created;
	private Fields fields;
	private List<NestedComment> children;

	public String getUuid() {
		return uuid;
	}

	public NestedComment setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	public String getCreated() {
		return created;
	}

	public NestedComment setCreated(String created) {
		this.created = created;
		return this;
	}

	public Fields getFields() {
		return fields;
	}

	public NestedComment setFields(Fields fields) {
		this.fields = fields;
		return this;
	}

	@Override
	public List<NestedComment> getChildren() {
		return children;
	}

	public NestedComment setChildren(List<NestedComment> children) {
		this.children = children;
		return this;
	}

	public static class Fields {
		private String email;
		private String author;
		private String authorReference;
		private String subject;
		private String comment;
		private Boolean hidden;
		private CommentStatus status;

		public String getEmail() {
			return email;
		}

		public Fields setEmail(String email) {
			this.email = email;
			return this;
		}

		public String getAuthor() {
			return author;
		}

		public Fields setAuthor(String author) {
			this.author = author;
			return this;
		}

		public String getSubject() {
			return subject;
		}

		public Fields setSubject(String subject) {
			this.subject = subject;
			return this;
		}

		public String getComment() {
			return comment;
		}

		public Fields setComment(String comment) {
			this.comment = comment;
			return this;
		}

		public Boolean getHidden() {
			return hidden;
		}

		public Fields setHidden(Boolean hidden) {
			this.hidden = hidden;
			return this;
		}

		public CommentStatus getStatus() {
			return status;
		}

		public Fields setStatus(CommentStatus status) {
			this.status = status;
			return this;
		}

		public String getAuthorReference() {
			return authorReference;
		}

		public Fields setAuthorReference(String authorReference) {
			this.authorReference = authorReference;
			return this;
		}
	}
}
