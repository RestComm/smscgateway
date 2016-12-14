/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
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

package org.mobicents.smsc.domain;

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;

/**
*
* @author sergey vetyutnev
*
*/
public class HttpUser implements HttpUserMBean {

    private static final Logger logger = Logger.getLogger(HttpUser.class);

    private static final String USER_NAME = "userName";
    private static final String PASSWORD = "password";

    protected HttpUsersManagement httpUsersManagement;

    private String userName;
    private String password = "";

    public HttpUser() {
    }

    public HttpUser(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        this.store();
    }


    /**
     * XML Serialization/Deserialization
     */
    protected static final XMLFormat<HttpUser> HTTP_USER_XML = new XMLFormat<HttpUser>(HttpUser.class) {

        @Override
        public void read(javolution.xml.XMLFormat.InputElement xml, HttpUser httpUser) throws XMLStreamException {
            httpUser.userName = xml.getAttribute(USER_NAME, "");
            httpUser.password = xml.getAttribute(PASSWORD, "");
        }

        @Override
        public void write(HttpUser httpUser, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
            xml.setAttribute(USER_NAME, httpUser.userName);
            xml.setAttribute(PASSWORD, httpUser.password);
        }
    };

    public void show(StringBuffer sb) {
        sb.append(SMSCOAMMessages.SHOW_HTTPUSER_NAME).append(this.userName).append(SMSCOAMMessages.SHOW_HTTPUSER_PASSWORD)
                .append(this.password);

        sb.append(SMSCOAMMessages.NEW_LINE);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((userName == null) ? 0 : userName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HttpUser other = (HttpUser) obj;
        if (userName == null) {
            if (other.userName != null)
                return false;
        } else if (!userName.equals(other.userName))
            return false;
        return true;
    }

    public void store() {
        this.httpUsersManagement.store();
    }

}
