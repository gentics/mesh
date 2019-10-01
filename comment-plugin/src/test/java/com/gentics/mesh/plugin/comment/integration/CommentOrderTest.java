package com.gentics.mesh.plugin.comment.integration;

import co.unruly.matchers.StreamMatchers;
import com.gentics.mesh.plugin.comment.model.NestedCommentSection;
import com.gentics.mesh.plugin.comment.model.Order;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.stream.Stream;

import static org.junit.Assert.assertThat;

public class CommentOrderTest extends AbstractLocalIntegrationTest {

	@BeforeClass
	public static void createProject() throws Exception {
		initMesh("anonymousNoApproval");
	}

	@Test
	public void testCommentOrderDefault() throws Exception {
		// Sleeps are necessary to prevent same creation date
		client.addComment("comment1");
		Thread.sleep(1000);
		client.addComment("comment2");
		Thread.sleep(1000);
		client.addComment("comment3");

		NestedCommentSection response = commentsOfNodeResponse(assertStatusCode(200, client.getCommentsOfNode()));

		Stream<String> comments = response.getChildren().stream().map(comment -> comment.getFields().getComment());
		assertThat(comments, StreamMatchers.contains("comment1", "comment2", "comment3"));
	}

	@Test
	public void testCommentOrderAsc() throws Exception {
		// Sleeps are necessary to prevent same creation date
		client.addComment("comment1");
		Thread.sleep(1000);
		client.addComment("comment2");
		Thread.sleep(1000);
		client.addComment("comment3");

		NestedCommentSection response = commentsOfNodeResponse(assertStatusCode(200, client.getCommentsOfNode(Order.ASCENDING)));
		Stream<String> comments = response.getChildren().stream().map(comment -> comment.getFields().getComment());
		assertThat(comments, StreamMatchers.contains("comment1", "comment2", "comment3"));
	}

	@Test
	public void testCommentOrderDesc() throws Exception {
		// Sleeps are necessary to prevent same creation date
		client.addComment("comment1");
		Thread.sleep(1000);
		client.addComment("comment2");
		Thread.sleep(1000);
		client.addComment("comment3");

		NestedCommentSection response = commentsOfNodeResponse(assertStatusCode(200, client.getCommentsOfNode(Order.DESCENDING)));
		Stream<String> comments = response.getChildren().stream().map(comment -> comment.getFields().getComment());
		assertThat(comments, StreamMatchers.contains("comment3", "comment2", "comment1"));
	}

}
