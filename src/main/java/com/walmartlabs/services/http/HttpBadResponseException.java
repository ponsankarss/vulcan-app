package com.walmartlabs.services.http;

import org.apache.http.client.HttpResponseException;

/**
 * Extension of {@link HttpResponseException} to hold more information about the
 * bad response
 * 
 * @version 0.1
 *
 * @author <a href="mailto:psemman@walmartlabs.com">Ponsankar S</a>
 */
public class HttpBadResponseException extends HttpResponseException {

	private static final long serialVersionUID = -5791905381274240857L;

	private String responseBody;

	public HttpBadResponseException(int statusCode, String reasonPhrase, String responseBody) {
		super(statusCode, reasonPhrase);
		this.responseBody = responseBody;
	}

	/**
	 * @return the responseBody
	 */
	public String getResponseBody() {
		return responseBody;
	}

	/**
	 * @param responseBody
	 *            the responseBody to set
	 */
	public void setResponseBody(String responseBody) {
		this.responseBody = responseBody;
	}
}
