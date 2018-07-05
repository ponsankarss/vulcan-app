package com.walmartlabs.services.api;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.cxf.helpers.IOUtils;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;

import com.walmartlabs.services.crypto.SignatureGenerator;
import com.walmartlabs.services.http.HttpClientPoolManager;

/**
 * Base service impl
 * 
 * @version 0.1
 * 
 * @author psemman
 *
 */
public class ServiceImpl implements Service {
	public static final String urlStg = "https://developer.api.stg.walmart.com/api-proxy/service/tracking/vulcan-app/v1/carrier-tracking-update/easypost/";
	public static final String urlProd = "https://developer.api.walmart.com/api-proxy/service/tracking/vulcan-app/v1/carrier-tracking-update/easypost/";

	public static final String consumerIdStg = "a2a1dc43-c177-4e84-a4be-5bc337bb7bcd";
	public static final String consumerIdProd = "72848c5e-30eb-4e0b-b779-ac163cf65565";

	@Autowired
	private HttpClientPoolManager httpClientPoolManager;

	@Autowired
	private SignatureGenerator signatureGenerator;

	@Override
	public Response status() {
		return Response.ok().entity("{\"status\":\"ok\"}").build();
	}

	@Override
	public Response trackingUpdate(String accountName, String body, String id) {
		long l = System.currentTimeMillis();
		String url = null;
		String consumerId = null;

		if ("e18bb426-bb02-4e65-937a-2cfa2b6d2cfd".equals(id)) {
			// PROD
			consumerId = consumerIdProd;
			url = urlProd;
		} else if ("0894c9f0-0b6e-434a-8fba-7129a0414159".equals(id)) {
			// STG
			consumerId = consumerIdStg;
			url = urlStg;
		} else {
			return Response.status(401)
					.entity("Query param {id} in request is missing or not valid. Please contact Vulcan team").build();
		}

		try {
			Map<String, String> headers = new HashMap<>();
			headers.put("Accept", "application/json");
			headers.put("Content-Type", "application/json");
			headers.putAll(signatureGenerator.generateSignature(consumerId));

			final HttpResponse response = httpClientPoolManager.doPost(url + accountName, headers, body.getBytes(),
					HttpResponse.class);
			if (null != response) {
				int statusCode = response.getStatusLine().getStatusCode();
				return Response.status(statusCode).entity(IOUtils.toString(response.getEntity().getContent())).build();
			}
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("Processing time.." + (System.currentTimeMillis() - l));
		}
		return Response.serverError().build();
	}
}