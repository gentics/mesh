package com.gentics.mesh.context.impl;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpFrame;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.StreamPriority;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

public class LocalHttpServerRequest implements HttpServerRequest {

	MultiMap params = new CaseInsensitiveHeaders();

	MultiMap headers = new CaseInsensitiveHeaders();

	private HttpServerResponse response;

	public LocalHttpServerRequest(HttpServerResponse response) {
		this.response = response;
	}

	@Override
	public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerRequest handler(Handler<Buffer> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerRequest pause() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerRequest resume() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerRequest fetch(long amount) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerRequest endHandler(Handler<Void> endHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpVersion version() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpMethod method() {
		return HttpMethod.GET;
	}

	@Override
	public String rawMethod() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSSL() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public @Nullable String scheme() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String uri() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable String path() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable String query() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable String host() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long bytesRead() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public HttpServerResponse response() {
		return response;
	}

	@Override
	public MultiMap headers() {
		return headers;
	}

	@Override
	public @Nullable String getHeader(String headerName) {
		return headers.get(headerName);
	}

	@Override
	public String getHeader(CharSequence headerName) {
		return headers.get(headerName);
	}

	@Override
	public MultiMap params() {
		return params;
	}

	@Override
	public @Nullable String getParam(String paramName) {
		return params.get(paramName);
	}

	@Override
	public SocketAddress remoteAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SocketAddress localAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SSLSession sslSession() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String absoluteURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NetSocket netSocket() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerRequest setExpectMultipart(boolean expect) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isExpectMultipart() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public HttpServerRequest uploadHandler(@Nullable Handler<HttpServerFileUpload> uploadHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MultiMap formAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable String getFormAttribute(String attributeName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServerWebSocket upgrade() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEnded() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public HttpServerRequest customFrameHandler(Handler<HttpFrame> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpConnection connection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerRequest streamPriorityHandler(Handler<StreamPriority> handler) {
		// TODO Auto-generated method stub
		return null;
	}

}
