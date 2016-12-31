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
}
