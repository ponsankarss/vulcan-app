package com.walmartlabs.services.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
@Path("/carriers/easypost")
public interface Service {

	@POST
	@Path("/{accountName}")
	@Consumes({ MediaType.APPLICATION_JSON })
	Response trackingUpdate(@PathParam("accountName") String accountName, final String body);
}