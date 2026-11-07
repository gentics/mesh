package com.gentics.mesh.plugin.search;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.search.config.CustomQueryOptions;
import com.gentics.mesh.plugin.search.extension.CustomHandler;
import com.gentics.mesh.plugin.search.handler.custom.CustomQueryHandler;
import com.gentics.mesh.plugin.search.service.QueryService;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.gentics.mesh.plugin.search.util.TestUtils.mockContext;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for the {@link CustomHandler}.
 */
public class CustomHandlerTest {

    private static final String DEFAULT_PROJECT = "dummy";
    private static final int CUSTOM_PAGE_SIZE = 10;
    private static final String CUSTOM_NAME = "custom_pagesize";
    private static final HandlerType CUSTOM_TYPE = HandlerType.SEARCH;
    private static final String CUSTOM_SEARCH_QUERY = new JsonObject().put("query", "search query").encode();
    private static QueryService queryServiceMock;
    private static CustomQueryOptions customQueryOptionsMock;

    /**
     * Setup the query store and query builder.
     */
    @BeforeClass
    public static void setupOnce() {
        queryServiceMock = mock(QueryService.class);

        when(queryServiceMock.getCustomQuery(any(), any())).thenReturn(Single.just(new JsonObject(CUSTOM_SEARCH_QUERY)));

    }

    /**
     * Make sure each test starts with the default configuration.
     */
    @Before
    public void setup() {
        customQueryOptionsMock = mock(CustomQueryOptions.class);

        when(customQueryOptionsMock.getPageSize()).thenReturn(CUSTOM_PAGE_SIZE);
        when(customQueryOptionsMock.getName()).thenReturn(CUSTOM_NAME);
        when(customQueryOptionsMock.getType()).thenReturn(CUSTOM_TYPE);
    }

    @Test
    public void testPageSizes() {
        for (int page = 0; page < 5; page++) {
            testPageSize(page);
        }
    }

    private void testPageSize(int page) {
        CustomQueryHandler handler = new CustomQueryHandler(queryServiceMock, customQueryOptionsMock);

        PluginContext pc = mockContext(DEFAULT_PROJECT, "/custom_pagesize", "q=test&p=" + page);
        JsonObject expected = new JsonObject(CUSTOM_SEARCH_QUERY);
        expected.put("size", CUSTOM_PAGE_SIZE);
        expected.put("from", CUSTOM_PAGE_SIZE * page);

        handler.getQuery(pc).test().assertComplete().assertValue(expected);
        verify(queryServiceMock).getCustomQuery(eq(pc), eq(CUSTOM_NAME));
        verify(customQueryOptionsMock, times(page + 1)).getPageSize();
    }
}
