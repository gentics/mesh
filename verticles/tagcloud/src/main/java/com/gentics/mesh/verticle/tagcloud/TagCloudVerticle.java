package com.gentics.mesh.verticle.tagcloud;

import static com.gentics.mesh.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.GET;

import java.util.List;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.tagcloud.model.TagCloud;
import com.gentics.mesh.tagcloud.model.TagCloudEntry;
import com.gentics.mesh.tagcloud.model.TagCloudResult;

@Component
@Scope("singleton")
@SpringVerticle
public class TagCloudVerticle extends AbstractProjectRestVerticle {

	@Autowired
	private TagCloudService tagCloudService;

	public TagCloudVerticle() {
		super("page");
	}

	@Override
	public void registerEndPoints() throws Exception {
		addTagCloudHandler();
	}

	/**
	 * Add the tagcloud load handler.
	 */
	private void addTagCloudHandler() {
		// TODO handle languages
		Language language = null;

		route("/tagcloud").method(GET).handler(rc -> {
			TagCloud cloud = new TagCloud();
			// TODO transaction handling should be moved to abstract rest resource
				List<TagCloudResult> res = tagCloudService.getTagCloudInfo();
				for (TagCloudResult current : res) {
					TagCloudEntry entry = new TagCloudEntry();
					String name = current.getTag().getName(language);
					entry.setName(name);
					// TODO determine link
					entry.setLink("TBD");
					entry.setCount(current.getCounts());
					cloud.getEntries().add(entry);
				}
				rc.response().headers().add("Content-Type", APPLICATION_JSON);
				rc.response().end(toJson(cloud));
			});
	}
}
