package com.gentics.mesh.plugin.comment.integration;

import com.gentics.mesh.plugin.comment.model.Comment;
import com.gentics.mesh.plugin.comment.model.NestedCommentSection;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.gentics.mesh.plugin.comment.TestCommentClient.User.ANONYMOUS;
import static com.gentics.mesh.plugin.comment.TestCommentClient.User.EDITOR;
import static org.junit.Assert.assertEquals;

public class NeedsApprovalTest extends AbstractLocalIntegrationTest {

	@BeforeClass
	public static void createProject() throws Exception {
		initMesh("anonymousApprovalNoFieldsRequired");
	}

	@Test
	public void testCommentNotApproved() {
		Comment comment = client.addComment("test");
		assertStatusCode(403, client.getCommentResponse(comment.getUuid()));
	}

	@Test
	public void testFetchAllCommentsNotApproved() {
		client.addComment("test");
		NestedCommentSection comments = commentsOfNodeResponse((client.getCommentsOfNode()));
		assertEquals(0, comments.getChildren().size());
	}

	@Test
	public void testApproved() {
		Comment comment = client.addComment("test");
		client.setUser(EDITOR);
		client.approve(comment);
		client.setUser(ANONYMOUS);
		assertStatusCode(200, client.getCommentResponse(comment.getUuid()));
	}

	@Test
	public void testSomeApproved() {
		Comment approved = client.addComment("approved");
		client.addComment("not approved");

		client.setUser(EDITOR);
		client.approve(approved);
		client.setUser(ANONYMOUS);

		NestedCommentSection comments = commentsOfNodeResponse((client.getCommentsOfNode()));
		assertEquals(1, comments.getChildren().size());
	}

	@Test
	public void testDenyAnonymousApprove() {
		Comment comment = client.addComment("test");
		assertStatusCode(403, client.approve(comment));
	}
}
