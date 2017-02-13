/*
 * Telestax, Open Source Cloud Communications Copyright 2011-2017,
 * Telestax Inc and individual contributors by the @authors tag.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.smsc.slee.services.http.server.tx.data;

import org.mobicents.smsc.domain.HttpUser;
import org.mobicents.smsc.domain.HttpUsersManagement;
import org.mobicents.smsc.slee.services.http.server.tx.enums.RequestParameter;
import org.mobicents.smsc.slee.services.http.server.tx.enums.ResponseFormat;
import org.mobicents.smsc.slee.services.http.server.tx.exceptions.HttpApiException;
import org.mobicents.smsc.slee.services.http.server.tx.exceptions.UnauthorizedException;
import org.mobicents.smsc.slee.services.http.server.tx.utils.HttpRequestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.slee.facilities.Tracer;
import java.util.Map;

/**
 * Created by mniemiec on 19.10.16.
 */
public abstract class BaseIncomingData {

    protected String userId;
    protected String password;
    protected ResponseFormat format;
    private Integer itsNetworkId;

    public BaseIncomingData(String userId, String password, String formatParam, HttpUsersManagement httpUsersManagement) throws UnauthorizedException {

        this.format = formatParam == null ? ResponseFormat.STRING : ResponseFormat.fromString(formatParam);
        // checking if mandatory fields are present
        if (isEmptyOrNull(userId)) {
            throw new UnauthorizedException("Unauthorized: Access is denied due to invalid credentials - " + RequestParameter.USER_ID.getName() + " is null.", userId, password);
        }
        if (isEmptyOrNull(password)) {
            throw new UnauthorizedException("Unauthorized: Access is denied due to invalid credentials - " + RequestParameter.PASSWORD.getName() + " is null.", userId, password);
        }
        if (!checkUsernameAndPassword(userId, password, httpUsersManagement)) {
            throw new UnauthorizedException("Unauthorized: Access is denied due to invalid credentials - "
                    + RequestParameter.USER_ID.getName() + " or " + RequestParameter.PASSWORD.getName() + " is incorrect.", userId, password);
        }
        this.userId = userId;
        this.password = password;
    }

    public boolean isEmptyOrNull(String toCheck) {
        if (toCheck == null) {
            return true;
        }
        if ("".equals(toCheck)) {
            return true;
        }
        return false;
    }

    public boolean checkUsernameAndPassword(String userId, String password, HttpUsersManagement httpUsersManagement) {
        HttpUser httpUser = httpUsersManagement.getHttpUserByName(userId);
        if(httpUser != null) {
            itsNetworkId = httpUser.getNetworkId();
            return password.equals(httpUser.getPassword());
        }
        return false;
    }

    public static ResponseFormat getFormat(Tracer tracer, HttpServletRequest request) throws HttpApiException {
        String formatParameter = request.getParameter("format");
        if(formatParameter!= null) {
            return ResponseFormat.fromString(formatParameter);
        } else {
            try {
                Map<String, String[]> stringMap = HttpRequestUtils.extractParametersFromPost(tracer, request);
                String[] format = stringMap.get(HttpRequestUtils.P_FORMAT);
                return ResponseFormat.fromString(format != null && format.length > 0 ? format[0] : null);
            } catch (Exception e){
                return ResponseFormat.STRING;
            }
        }
    }

    public static ResponseFormat getFormat(HttpServletRequest request) {
        String param = request.getParameter("format");
        return ResponseFormat.fromString(param);
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public ResponseFormat getFormat() {
        return format;
    }

    /**
     * Gets the network ID.
     *
     * @return the network ID
     */
    public Integer getNetworkId() {
        return itsNetworkId;
    }
}
