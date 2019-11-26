package com.gentics.mesh.context.impl;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;

public class LocalHttpServerResponse implements HttpServerResponse {

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean writeQueueFull() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public HttpServerResponse exceptionHandler(Handler<Throwable> handler) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public HttpServerResponse write(Buffer data) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public HttpServerResponse write(Buffer data, Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public HttpServerResponse setWriteQueueMaxSize(int maxSize) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public HttpServerResponse drainHandler(Handler<Void> handler) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public int getStatusCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public HttpServerResponse setStatusCode(int statusCode) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public String getStatusMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerResponse setStatusMessage(String statusMessage) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public HttpServerResponse setChunked(boolean chunked) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public boolean isChunked() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public MultiMap headers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerResponse putHeader(String name, String value) {
		return this;
	}

	@Override
	public HttpServerResponse putHeader(CharSequence name, CharSequence value) {
		return this;
	}

	@Override
	public HttpServerResponse putHeader(String name, Iterable<String> values) {
		return this;
	}

	@Override
	public HttpServerResponse putHeader(CharSequence name, Iterable<CharSequence> values) {
		return this;
	}

	@Override
	public MultiMap trailers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerResponse putTrailer(String name, String value) {
		return this;
	}

	@Override
	public HttpServerResponse putTrailer(CharSequence name, CharSequence value) {
		return this;
	}

	@Override
	public HttpServerResponse putTrailer(String name, Iterable<String> values) {
		return this;
	}

	@Override
	public HttpServerResponse putTrailer(CharSequence name, Iterable<CharSequence> value) {
		return this;
	}

	@Override
	public HttpServerResponse closeHandler(@Nullable Handler<Void> handler) {
		return this;
	}

	@Override
	public HttpServerResponse endHandler(@Nullable Handler<Void> handler) {
		return this;
	}

	@Override
	public HttpServerResponse write(String chunk, String enc) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public HttpServerResponse write(String chunk, String enc, Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public HttpServerResponse write(String chunk) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public HttpServerResponse write(String chunk, Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public HttpServerResponse writeContinue() {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public void end(String chunk) {
		// TODO Auto-generated method stub

	}

	@Override
	public void end(String chunk, Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void end(String chunk, String enc) {
		// TODO Auto-generated method stub

	}

	@Override
	public void end(String chunk, String enc, Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void end(Buffer chunk) {
		// TODO Auto-generated method stub

	}

	@Override
	public void end(Buffer chunk, Handler<AsyncResult<Void>> handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void end() {
		// TODO Auto-generated method stub

	}

	@Override
	public HttpServerResponse sendFile(String filename, long offset, long length) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerResponse sendFile(String filename, long offset, long length, Handler<AsyncResult<Void>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean ended() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean closed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean headWritten() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public HttpServerResponse headersEndHandler(@Nullable Handler<Void> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpServerResponse bodyEndHandler(@Nullable Handler<Void> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long bytesWritten() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int streamId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public HttpServerResponse push(HttpMethod method, String host, String path, Handler<AsyncResult<HttpServerResponse>> handler) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public HttpServerResponse push(HttpMethod method, String path, MultiMap headers, Handler<AsyncResult<HttpServerResponse>> handler) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public HttpServerResponse push(HttpMethod method, String path, Handler<AsyncResult<HttpServerResponse>> handler) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public HttpServerResponse push(HttpMethod method, String host, String path, MultiMap headers, Handler<AsyncResult<HttpServerResponse>> handler) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public void reset(long code) {
		// TODO Auto-generated method stub

	}

	@Override
	public HttpServerResponse writeCustomFrame(int type, int flags, Buffer payload) {
		// TODO Auto-generated method stub
		return this;
	}

}
