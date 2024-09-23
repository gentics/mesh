
var defaultGraphQLUrl = "https://examples.getmesh.io/api/v2/demo/graphql";

var dummyStorage = {
	getItem: function (name) {
		return null;
	},
	setItem: function () {
	}
};

$(".graphql-example").each(function (i) {
	var element = $(this);
	var parameters = {};
	var baseAttr = element.data("url");
	var graphQLUrl = baseAttr ? baseAttr : defaultGraphQLUrl;

	var queryEle = element.find("#query");
	if (queryEle.length) {
		parameters.query = $.trim(queryEle.text());
	} else {
		parameters.query = $.trim(element.text());
	}
	var varsEle = element.find("#variables");
	if (varsEle.length) {
		var variables = JSON.stringify(JSON.parse($.trim(varsEle.text())));
		parameters.variables =  variables;
	}

	function onEditQuery(newQuery) {
		parameters.query = newQuery;
	}

	function onEditVariables(newVariables) {
		parameters.variables = newVariables;
	}

	function onEditOperationName(newOperationName) {
		parameters.operationName = newOperationName;
	}

	// Defines a GraphQL fetcher using the fetch API.
	function graphQLFetcher(graphQLParams) {
		return fetch(graphQLUrl, {
			method: 'post',
			headers: {
				'Accept': 'application/json',
				'Content-Type': 'application/json',
			},
			body: JSON.stringify(graphQLParams),
			credentials: 'include',
		}).then(function (response) {
			return response.text();
		}).then(function (responseBody) {
			try {
				return JSON.parse(responseBody);
			} catch (error) {
				return responseBody;
			}
		});
	}

	// Render <GraphiQL /> into the body.
	ReactDOM.render(
		React.createElement(GraphiQL, {
			storage: dummyStorage,
			fetcher: graphQLFetcher,
			query: parameters.query,
			variables: parameters.variables,
			operationName: parameters.operationName,
			onEditQuery: onEditQuery,
			onEditVariables: onEditVariables,
			onEditOperationName: onEditOperationName
		}),
		element[0]
	);
});
