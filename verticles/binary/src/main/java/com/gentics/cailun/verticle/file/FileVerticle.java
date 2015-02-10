package com.gentics.cailun.verticle.file;

import com.gentics.cailun.core.AbstractCaiLunProjectRestVerticle;

public class FileVerticle extends AbstractCaiLunProjectRestVerticle {

	protected FileVerticle(String basePath) {
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
