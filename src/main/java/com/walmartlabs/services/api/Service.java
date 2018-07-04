package com.walmartlabs.services.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Base service
 * 
 * @version 0.1
 * 
 * @author psemman
 *
 */
public interface Service {
	
	@GET
	@Path("/status")
	Response status();

	@POST
	@Path("/carriers/easypost/{accountName}")
	@Consumes({ MediaType.APPLICATION_JSON })
	Response trackingUpdate(@PathParam("accountName") String accountName, final String body, @QueryParam("id") String id);
}