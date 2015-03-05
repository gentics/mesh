package com.gentics.cailun.verticle.file;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;

@Component
@Scope("singleton")
@SpringVerticle
public class FileVerticle extends AbstractProjectRestVerticle {

	public FileVerticle() {
		super("file");
	}

	@Override
	public void registerEndPoints() throws Exception {
		// http://docs.openstack.org/api/openstack-object-storage/1.0/content/ch_object-storage-dev-api-storage.html
		// http://docs.openstack.org/api/openstack-object-storage/1.0/content/storage_object_services.html
		// upload
		// download
		// delete
		// update
	}
}
