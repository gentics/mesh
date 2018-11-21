package com.gentics.diktyo;

import java.util.Iterator;
import java.util.ServiceLoader;

import com.gentics.diktyo.db.DatabaseManager;

public interface Diktyo {

	static Diktyo diktyo() {
		ServiceLoader<DiktyoFactory> factories = ServiceLoader.load(DiktyoFactory.class);
		Iterator<DiktyoFactory> it = factories.iterator();
		if (!it.hasNext()) {
			throw new RuntimeException("Could not find DiktyoFactory service. It seems that diktyo-core is not part of the classpath.");
		} else {
			DiktyoFactory factory = it.next();

			if (it.hasNext()) {
				throw new RuntimeException("Found more than one factory implementation in the classpath.");
			}

			return factory.instance();
		}
	}

	/**
	 * Return the database manager which is used to access,create and delete databases.
	 * 
	 * @return
	 */
	DatabaseManager db();

}
