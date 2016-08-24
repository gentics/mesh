package com.gentics.mesh.verticle.tagcloud;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static io.vertx.core.http.HttpMethod.GET;

import java.util.List;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.tagcloud.model.TagCloud;
import com.gentics.mesh.tagcloud.model.TagCloudEntry;
import com.gentics.mesh.tagcloud.model.TagCloudResult;

public class TagCloudVerticle extends AbstractProjectRestVerticle {

	private TagCloudService tagCloudService;

	@Inject
	public TagCloudVerticle(BootstrapInitializer boot, RouterStorage routerStorage, MeshSpringConfiguration springConfig,
			TagCloudService tagCloudService) {
		super("page", boot, routerStorage, springConfig);
		this.tagCloudService = tagCloudService;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow generation of tag clouds.";
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
				String name = current.getTag().getName();
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
