package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MicroschemaContainer;

@Component
public class MicroschemaContainerIndexHandler extends AbstractIndexHandler<MicroschemaContainer> {

	@Override
	String getIndex() {
		return "microschema";
	}

	@Override
	String getType() {
		return "microschema";
	}

	@Override
	public void store(MicroschemaContainer microschema) {
		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, microschema);
		map.put("name", microschema.getName());
		store(microschema.getUuid(), map);
	}

	@Override
	public void store(String uuid) {
		boot.microschemaContainerRoot().findByUuid(uuid, rh -> {
			if (rh.result() != null && rh.succeeded()) {
				MicroschemaContainer microschema = rh.result();
				store(microschema);
			} else {
				//TODO reply error? discard? log?
			}
		});

	}

	public void update(String uuid) {
		// TODO Auto-generated method stub
		
	}

}
