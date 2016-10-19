/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import org.mobicents.smsc.domain.HttpUsersManagement;
import org.mobicents.smsc.slee.services.http.server.tx.enums.RequestParameter;
import org.mobicents.smsc.slee.services.http.server.tx.enums.ResponseFormat;
import org.mobicents.smsc.slee.services.http.server.tx.exceptions.HttpApiException;
import org.mobicents.smsc.slee.services.http.server.tx.exceptions.UnauthorizedException;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by tpalucki on 14.09.16.
 */
public class HttpGetMessageIdStatusIncomingData extends BaseIncomingData {

    private Long msgId;

    public HttpGetMessageIdStatusIncomingData(String userId, String password, String msgId, String formatParam, HttpUsersManagement httpUsersManagement) throws HttpApiException, UnauthorizedException {

        super(userId, password, formatParam, httpUsersManagement);

        if (isEmptyOrNull(msgId)) {
            throw new HttpApiException("'" + RequestParameter.MESSAGE_ID.getName() + "' parameter is not set properly or not valid in the Http Request.");
        }

        try {
            this.msgId = Long.parseLong(msgId);
        } catch (NumberFormatException e) {
            throw new HttpApiException("'" + RequestParameter.MESSAGE_ID.getName() + "' parameter in the Http Request is not valid long type");
        }
    }

    public Long getMsgId(){
        return this.msgId;
    }

    @Override
    public String toString() {
        return "HttpSendMessageIncomingData{" +
                "userId='" + userId + '\'' +
                ", password='" + password + '\'' +
                ", msgid='" + msgId + '\'' +
                '}';
    }

    public void setMsgId(Long msgId) {
        this.msgId = msgId;
    }
}
