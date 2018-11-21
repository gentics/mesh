package com.gentics.diktyo.orientdb3.dagger;

import com.gentics.diktyo.Diktyo;
import com.gentics.diktyo.DiktyoFactory;

public class DiktyoFactoryImpl implements DiktyoFactory {

	public static Diktyo instance;

	@Override
	public Diktyo instance() {
		// Create the instance if missing
		if (instance == null) {
			DiktyoComponent component = DaggerDiktyoComponent.create();
			instance = component.diktyo();
		}
		return instance;
	}

}
