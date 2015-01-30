package com.gentics.vertx.cailun.tagcloud;

import static io.vertx.core.http.HttpMethod.GET;

import java.util.List;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCailunRestVerticle;
import com.gentics.vertx.cailun.tagcloud.model.TagCloud;
import com.gentics.vertx.cailun.tagcloud.model.TagCloudEntry;
import com.gentics.vertx.cailun.tagcloud.model.TagCloudResult;

@Component
@Scope("singleton")
@SpringVerticle
public class TagCloudVerticle extends AbstractCailunRestVerticle {

	@Autowired
	private TagCloudRepository pageRepository;

	@Autowired
	GraphDatabaseService graphDb;

	public TagCloudVerticle() {
		super("page");
	}

	@Override
	public void start() throws Exception {
		super.start();
		addTagCloudHandler();
	}

	/**
	 * Add the tagcloud load handler.
	 */
	private void addTagCloudHandler() {
		route("/tagcloud").method(GET).handler(rc -> {
			TagCloud cloud = new TagCloud();
			// TODO transaction handling should be moved to abstract rest resource
				try (Transaction tx = graphDb.beginTx()) {
					List<TagCloudResult> res = pageRepository.getTagCloudInfo();
					for (TagCloudResult current : res) {
						TagCloudEntry entry = new TagCloudEntry();
						entry.setName(current.getTag().getName());
						// TODO determine link
						entry.setLink("TBD");
						entry.setCount(current.getCounts());
						cloud.getEntries().add(entry);
					}
				}
				rc.response().headers().add("Content-Type", APPLICATION_JSON);
				rc.response().end(toJson(cloud));
			});
	}
}
