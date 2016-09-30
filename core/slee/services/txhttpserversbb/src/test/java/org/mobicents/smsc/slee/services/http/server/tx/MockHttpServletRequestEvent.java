package org.mobicents.smsc.slee.services.http.server.tx;

import net.java.slee.resource.http.events.HttpServletRequestEvent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by tpalucki on 07.09.16.
 */
public class MockHttpServletRequestEvent implements HttpServletRequestEvent {
    private HttpServletResponse response;

    private HttpServletRequest request;

    private String id = "MockHttpServletRequestEventID";

    @Override
    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public HttpServletResponse getResponse() {
        return response;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setRequest(HttpServletRequest req){
        this.request = req;
    }

    public void setResponse(HttpServletResponse resp) {
        this.response = resp;
    }
}
