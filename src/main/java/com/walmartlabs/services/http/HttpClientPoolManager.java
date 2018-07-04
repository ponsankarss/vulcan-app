package com.walmartlabs.services.http;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.Validate;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

/**
 * Http Client Pooling manager provides better abstraction for http operations.
 * It follows Singleton pattern for better performance. Manager is implemented
 * considering simplicity, unique experience for various usage and operations.
 *
 * @version 0.1
 *
 * @author <a href="mailto:psemman@walmartlabs.com">Ponsankar S</a>
 */
@Component
public class HttpClientPoolManager {

	private CloseableHttpClient httpclient;

	private int HTTP_BAD_RESPONSE_MARK = 400;
	private int INT_HTTP_DEFAULT_MAX_PER_ROUTE = 20;
	private int INT_HTTP_MAX_TOTAL = 2 * INT_HTTP_DEFAULT_MAX_PER_ROUTE;

	private int INT_HTTP_CONN_TIMEOUT = 60000; // 1min
	private int INT_HTTP_READ_TIMEOUT = 60000; // 1min
	private long INT_HTTP_MANAGER_TIMEOUT = 500l;

	@PostConstruct
	public void initialization() {
		final PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
		poolingHttpClientConnectionManager.setDefaultMaxPerRoute(INT_HTTP_DEFAULT_MAX_PER_ROUTE);
		poolingHttpClientConnectionManager.setMaxTotal(INT_HTTP_MAX_TOTAL);

		final Builder requestConfigBuilder = RequestConfig.custom();
		requestConfigBuilder.setConnectTimeout(INT_HTTP_CONN_TIMEOUT);
		requestConfigBuilder.setConnectionRequestTimeout(INT_HTTP_READ_TIMEOUT);
		// requestConfigBuilder.setSocketTimeout(INT_HTTP_READ_TIMEOUT);
		requestConfigBuilder.setCookieSpec(CookieSpecs.IGNORE_COOKIES);
		final RequestConfig requestConfig = requestConfigBuilder.build();

		final HttpClientBuilder httpClientBuilder = HttpClients.custom();
		httpClientBuilder.setConnectionManager(poolingHttpClientConnectionManager);
		httpClientBuilder.setDefaultRequestConfig(requestConfig);
		httpClientBuilder.evictIdleConnections(INT_HTTP_MANAGER_TIMEOUT, TimeUnit.MICROSECONDS);

		httpclient = httpClientBuilder.build();
	}

	/**
	 * Shutdowns http client
	 *
	 */
	@PreDestroy
	public void shutdown() {
		if (null != httpclient) {
			try {
				httpclient.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Performance Http GET operation
	 *
	 * @param uri
	 * @param headers
	 * @param responseClass
	 * @return {@link T}
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public <T> T doGet(final String uri, final Map<String, String> headers, final Class<T> responseClass)
			throws ClientProtocolException, IOException {
		T response = null;
		final HttpGet httpGet = new HttpGet(uri);
		response = execute(httpGet, uri, headers, responseClass);
		return response;
	}

	/**
	 * Performance Http Put operation
	 *
	 * @param uri
	 * @param headers
	 * @param body
	 * @param responseClass
	 * @return {@link T}
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public <T> T doPut(final String uri, final Map<String, String> headers, final byte[] body,
			final Class<T> responseClass) throws ClientProtocolException, IOException {
		T response = null;
		final HttpPut httpPut = new HttpPut(uri);
		if (null != body) {
			httpPut.setEntity(new ByteArrayEntity(body));
		}
		response = execute(httpPut, uri, headers, responseClass);
		return response;
	}

	/**
	 * Performance Http POST operation
	 *
	 * @param uri
	 * @param headers
	 * @param body
	 * @param responseClass
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public <T> T doPost(final String uri, final Map<String, String> headers, final byte[] body,
			final Class<T> responseClass) throws ClientProtocolException, IOException {
		T response = null;
		final HttpPost httpPost = new HttpPost(uri);
		if (null != body) {
			httpPost.setEntity(new ByteArrayEntity(body));
		}
		response = execute(httpPost, uri, headers, responseClass);
		return response;
	}

	/**
	 * Performance Http PATCH operation
	 *
	 * @param uri
	 * @param headers
	 * @param body
	 * @param responseClass
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public <T> T doPatch(final String uri, final Map<String, String> headers, final byte[] body,
			final Class<T> responseClass) throws ClientProtocolException, IOException {
		T response = null;
		final HttpPatch httpPatch = new HttpPatch(uri);
		if (null != body) {
			httpPatch.setEntity(new ByteArrayEntity(body));
		}
		response = execute(httpPatch, uri, headers, responseClass);
		return response;
	}

	@SuppressWarnings("unchecked")
	private <T> T execute(final HttpRequestBase httpRequestBase, final String uri, final Map<String, String> headers,
			final Class<T> responseClass) throws ClientProtocolException, IOException {
		Validate.notEmpty(uri, "URI is NULL!");

		/*
		 * Extracts and stamps headers to request base
		 */
		final Set<Entry<String, String>> headersSet = headers.entrySet();
		for (final Entry<String, String> entry : headersSet) {
			httpRequestBase.addHeader(entry.getKey(), entry.getValue());
		}

		/*
		 * Lambda implementation of response handler. It provides unique experience to
		 * various method invocations
		 */
		final ResponseHandler<T> responseHandler = response -> {
			T httpResponse = null;
			final StatusLine statusLine = response.getStatusLine();
			final HttpEntity entity = response.getEntity();
			if ((null != entity) && (null != responseClass)) {
				if (responseClass.equals(String.class)) {
					httpResponse = (T) readString(entity);

					if (statusLine.getStatusCode() >= HTTP_BAD_RESPONSE_MARK) {
						throw new HttpBadResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase(),
								(null != httpResponse) ? (String) httpResponse : null);
					}
				}
			}
			return httpResponse;
		};

		// decides whether response has to be handled or returned
		T httpResponse = null;
		final HttpResponse httpResponseRaw = httpclient.execute(httpRequestBase);

		if (responseClass.equals(HttpResponse.class)) {
			httpResponse = (T) httpResponseRaw;
		} else {
			httpResponse = responseHandler.handleResponse(httpResponseRaw);
		}

		return httpResponse;
	}

	/**
	 * Reads {@link HttpEntity} response into {@link String}
	 * 
	 * @param entity
	 * @return
	 */
	public static String readString(final HttpEntity entity) {
		String response = "";
		if (entity != null) {
			final Header header = entity.getContentEncoding();
			if (entity.getContentLength() < 2147483647L) {
				if ((header != null) && "gzip".equals(header.getValue())) {
					try {
						response = EntityUtils.toString(new GzipDecompressingEntity(entity));
					} catch (final Exception e) {
						e.printStackTrace();
					}
				} else {
					try {
						response = EntityUtils.toString(entity);
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			} else {

			}
		}
		return response;
	}
}
