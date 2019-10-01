async function post(path, data) {
  const response = await fetch(`/api/v1${path}`, {
    method: "POST",
    body: JSON.stringify(data),
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/json"
    }
  })
  return response.json();
}

async function get(path) {
  const response = await fetch(`/api/v1${path}`, {
    credentials: "same-origin"
  })
  return response.json();
}

(async () => {
  await post('/auth/login', {
    username: "admin",
    password: "admin"
  });
  const node = await get('/test/webroot/someNode');
  comments = await get(`/test/plugins/comment/node/${node.uuid}/comments`);
  document.getElementById('comments').innerHTML = Handlebars.templates["list"](comments);
})();