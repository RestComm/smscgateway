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

package org.mobicents.smsc.slee.services.smpp.server.events;

import java.io.Serializable;

import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.LMSI;
import org.mobicents.smsc.library.SmsSet;

/**
*
* @author sergey vetyutnev
*
*/
public class SendMtEvent implements Serializable {

    private static final long serialVersionUID = 4356951812011252069L;

    private SmsSet smsSet;
    private ISDNAddressString networkNode;
    private String imsiData;
    private LMSI lmsi;
    private InformServiceCenterContainer informServiceCenterContainer;
    private int sriMapVersion;
    private long currentMsgNum;
    private int sendingPoolMsgCount;

    public ISDNAddressString getNetworkNode() {
        return networkNode;
    }

    public void setNetworkNode(ISDNAddressString networkNode) {
        this.networkNode = networkNode;
    }

    public String getImsiData() {
        return imsiData;
    }

    public void setImsiData(String imsiData) {
        this.imsiData = imsiData;
    }

    public LMSI getLmsi() {
        return lmsi;
    }

    public void setLmsi(LMSI lmsi) {
        this.lmsi = lmsi;
    }

    public InformServiceCenterContainer getInformServiceCenterContainer() {
        return informServiceCenterContainer;
    }

    public void setInformServiceCenterContainer(InformServiceCenterContainer informServiceCenterContainer) {
        this.informServiceCenterContainer = informServiceCenterContainer;
    }

    public SmsSet getSmsSet() {
        return smsSet;
    }

    public void setSmsSet(SmsSet smsSet) {
        this.smsSet = smsSet;
    }

    public void setSriMapVersion(int sriMapVersion) {
        this.sriMapVersion = sriMapVersion;
    }

    public int getSriMapVersion() {
        return sriMapVersion;
    }

    public long getCurrentMsgNum() {
        return currentMsgNum;
    }

    public void setCurrentMsgNum(long currentMsgNum) {
        this.currentMsgNum = currentMsgNum;
    }

    public int getSendingPoolMsgCount() {
        return sendingPoolMsgCount;
    }

    public void setSendingPoolMsgCount(int sendingPoolMsgCount) {
        this.sendingPoolMsgCount = sendingPoolMsgCount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SendMtEvent [");

        if (this.networkNode != null) {
            sb.append("networkNode=");
            sb.append(this.networkNode);
            sb.append(", ");
        }
        if (this.imsiData != null) {
            sb.append("imsiData=");
            sb.append(this.imsiData);
            sb.append(", ");
        }
        if (this.lmsi != null) {
            sb.append("lmsi=");
            sb.append(this.lmsi);
            sb.append(", ");
        }
        if (this.informServiceCenterContainer != null) {
            sb.append("informServiceCenterContainer=");
            sb.append(this.informServiceCenterContainer);
            sb.append(", ");
        }
        sb.append("sriMapVersion=");
        sb.append(this.sriMapVersion);
        sb.append("currentMsgNum=");
        sb.append(this.currentMsgNum);
        sb.append("sendingPoolMsgCount=");
        sb.append(this.sendingPoolMsgCount);
        sb.append(", ");
        if (this.smsSet != null) {
            sb.append("smsSet=");
            sb.append(this.smsSet);
            sb.append(", ");
        }

        sb.append("]");
        return sb.toString();
    }

}
