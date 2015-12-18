package com.gentics.mesh.util;

import static com.gentics.mesh.json.JsonUtil.toJson;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.handler.InternalActionContext;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * 
 * @deprecated Please don't add stuff to this class since it should be removed anyway.
 *
 */
public class VerticleHelper {

	/**
	 * Transform the given vertex to a rest model and send with a JSON document response.
	 * 
	 * @param ac
	 * @param vertex
	 * @param statusCode
	 */
	public static <T extends RestModel> void transformAndRespond(InternalActionContext ac, MeshCoreVertex<?, ?> vertex,
			HttpResponseStatus statusCode) {
		vertex.transformToRest(ac, th -> {
			if (ac.failOnError(th)) {
				ac.send(toJson(th.result()), statusCode);
			}
		});
	}

}
