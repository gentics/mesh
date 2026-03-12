package com.gentics.mesh.plugin.comment.integration;

import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.plugin.comment.TestCommentClient;
import com.gentics.mesh.plugin.comment.model.Comment;
import com.gentics.mesh.plugin.comment.model.CommentStatus;
import com.gentics.mesh.plugin.comment.model.NestedCommentSection;
import com.gentics.mesh.plugin.comment.model.request.UpdateCommentRequest;
import com.gentics.mesh.util.UUIDUtil;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static com.gentics.mesh.plugin.comment.TestCommentClient.User.ADMIN;
import static com.gentics.mesh.plugin.comment.TestCommentClient.User.EDITOR;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class AnonymousTest extends AbstractLocalIntegrationTest {
	@BeforeClass
	public static void createProject() throws Exception {
		initMesh("anonymousNoApproval");
	}

	@Test
	public void testCreateCommentNoPerm() {
		Comment comment = client.addComment("This is a test comment");
		comment = client.getComment(comment.getUuid());
		assertEquals("This is a test comment", comment.getComment());
	}

	@Test
	public void testUpdateCommentAnonymous() {
		Comment comment = client.addComment("test");
		comment = client.getComment(comment.getUuid());
		assertEquals("test", comment.getComment());

		UpdateCommentRequest request = new UpdateCommentRequest()
			.setComment("edited test");

		assertStatusCode(403, client.updateCommentResponse(comment.getUuid(), request));

		comment = client.getComment(comment.getUuid());
		assertEquals("test", comment.getComment());
	}

	@Test
	public void testUpdateAnonymousCommentAsEditor() {
		Comment comment = client.addComment("test");
		comment = client.getComment(comment.getUuid());
		assertEquals("test", comment.getComment());

		client.setUser(EDITOR);

		UpdateCommentRequest request = new UpdateCommentRequest()
			.setComment("edited test");

		client.updateCommentResponse(comment.getUuid(), request);

		comment = client.getComment(comment.getUuid());
		assertEquals("edited test", comment.getComment());
	}

	@Test
	public void testDeleteCommentNoPerm() {
		Comment comment = client.addComment("test");
		assertStatusCode(200, client.getCommentResponse(comment.getUuid()));
		assertStatusCode(403, client.deleteComment(comment.getUuid()));

		comment = client.getComment(comment.getUuid());
		assertEquals(comment.getComment(), "test");
	}

	@Test
	public void testDeleteCommentAsEditor() {
		Comment comment = client.addComment("test");
		assertStatusCode(200, client.getCommentResponse(comment.getUuid()));

		client.setUser(EDITOR);
		assertStatusCode(204, client.deleteComment(comment.getUuid()));
		assertStatusCode(404, client.getCommentResponse(comment.getUuid()));
	}

	@Test
	public void createMultipleComments() {
		Comment parent = client.addComment("parent");
		client.addComment("comment2");
		client.addComment("comment3");
		Comment subParent = client.addComment("subComment1", parent);
		client.addComment("subComment2", parent);
		client.addComment("subSubComment1", subParent);
	}

	@Test
	public void fetchCommentsOfBogusNodeMultiple() {
		assertStatusCode(404, client.getCommentsOfNode(UUIDUtil.randomUUID(), "en"));
		assertStatusCode(404, client.getCommentsOfNode(UUIDUtil.randomUUID(), "en"));
		assertStatusCode(404, client.getCommentsOfNode(UUIDUtil.randomUUID(), "en"));
	}

	@Test
	public void fetchCommentsOfNodeMultiple() {
		client.addComment("comment1");
		client.addComment("comment2");
		NestedCommentSection response = commentsOfNodeResponse(assertStatusCode(200, client.getCommentsOfNode()));
		assertEquals(2, response.getChildren().size());
	}

	@Test
	public void testTemplateEngine() {
		String js = stringResponse(assertStatusCode(200, client.get("/templates/js")));

		assertThat(js.length(), is(greaterThan(0)));
	}

	@Test
	public void testAuthorReference() {
		Comment comment = client.addComment("comment");
		assertEquals(anonymousUser.getUuid(), comment.getAuthorReference());
		client.setUser(EDITOR);
		comment = client.addComment("comment");
		assertEquals(editorUser.getUuid(), comment.getAuthorReference());
		client.setUser(ADMIN);
		comment = client.addComment("comment");
		assertEquals(adminUser.getUuid(), comment.getAuthorReference());

		NestedCommentSection comments = commentsOfNodeResponse(client.getCommentsOfNode());
		Set<String> authors = comments.getChildren().stream()
			.map(c -> c.getFields().getAuthorReference())
			.collect(Collectors.toSet());

		assertThat(authors, containsInAnyOrder(anonymousUser.getUuid(), editorUser.getUuid(), adminUser.getUuid()));
	}

	@Test
	public void testPermissions() {
		NestedCommentSection comments;

		client.addComment("comment");

		comments = commentsOfNodeResponse(client.getCommentsOfNode());
		assertThat(comments.getPermissions().asSet(), containsInAnyOrder(Permission.READ_PUBLISHED, Permission.READ));

		client.setUser(EDITOR);
		comments = commentsOfNodeResponse(client.getCommentsOfNode());
		assertThat(comments.getPermissions().asSet(), containsInAnyOrder(
			Permission.READ,
			Permission.READ_PUBLISHED,
			Permission.PUBLISH,
			Permission.CREATE,
			Permission.UPDATE,
			Permission.DELETE
		));

		client.setUser(ADMIN);
		comments = commentsOfNodeResponse(client.getCommentsOfNode());
		assertThat(comments.getPermissions().asSet(), containsInAnyOrder(
			Permission.READ,
			Permission.READ_PUBLISHED,
			Permission.PUBLISH,
			Permission.CREATE,
			Permission.UPDATE,
			Permission.DELETE
		));
	}

	@Test
	public void testHideDeletedCommentsFromEditor() {
		Comment comment = client.addComment("comment");
		client.setUser(EDITOR);
		client.updateComment(comment.getUuid(), new UpdateCommentRequest().setStatus(CommentStatus.DELETED));
		NestedCommentSection comments = commentsOfNodeResponse(client.getCommentsOfNode());

		assertEquals(0, comments.getChildren().size());
	}

	@Test
	public void testFetchDeeplyNestedCommentSection() {
		Comment comment = client.addComment("comment");
		for (int i = 0; i < 10; i++) {
			comment = client.addComment("comment", comment);
		}
		commentsOfNodeResponse(client.getCommentsOfNode());
	}
}
