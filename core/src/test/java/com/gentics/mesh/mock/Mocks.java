package com.gentics.mesh.mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.mockito.Mockito;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.router.ProjectsRouter;
import com.gentics.mesh.util.HttpQueryUtils;
import com.gentics.madl.tx.Tx;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

public final class Mocks {

	private Mocks() {

	}

	public static InternalActionContext getMockedInternalActionContext(String query, User user, Project project) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(getMockedRoutingContext(query, false, user, null));
		ac.data().put(ProjectsRouter.PROJECT_CONTEXT_KEY, project);
		return ac;
	}

	public static RoutingContext getMockedRoutingContext(String query, boolean noInternalMap, User user, Project project) {
		Map<String, Object> map = new HashMap<>();
		if (noInternalMap) {
			map = null;
		}
		RoutingContext rc = mock(RoutingContext.class);
		Session session = mock(Session.class);
		HttpServerRequest request = mock(HttpServerRequest.class);
		when(request.query()).thenReturn(query);
		Map<String, String> paramMap = HttpQueryUtils.splitQuery(query);
		MultiMap paramMultiMap = MultiMap.caseInsensitiveMultiMap();
		for (Entry<String, String> entry : paramMap.entrySet()) {
			paramMultiMap.add(entry.getKey(), entry.getValue());
		}
		when(request.params()).thenReturn(paramMultiMap);
		when(request.getParam(Mockito.anyString())).thenAnswer(in -> {
			String key = (String) in.getArguments()[0];
			return paramMap.get(key);
		});
		paramMap.entrySet().stream().forEach(entry -> when(request.getParam(entry.getKey())).thenReturn(entry.getValue()));
		if (user != null) {
			MeshAuthUserImpl requestUser = Tx.getActive().getGraph().frameElement(user.getElement(), MeshAuthUserImpl.class);
			when(rc.user()).thenReturn(requestUser);
			// JsonObject principal = new JsonObject();
			// principal.put("uuid", user.getUuid());
		}
		when(rc.data()).thenReturn(map);
		MultiMap headerMap = mock(MultiMap.class);
		when(headerMap.get("Accept-Language")).thenReturn("en, en-gb;q=0.8, en;q=0.72");
		when(request.headers()).thenReturn(headerMap);
		when(rc.request()).thenReturn(request);
		when(rc.session()).thenReturn(session);

		if (project != null) {
			when(rc.get(ProjectsRouter.PROJECT_CONTEXT_KEY)).thenReturn(project);
		}
		return rc;

	}
}
