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

package org.mobicents.smsc.mproc.impl;

import java.util.Date;
import java.util.Random;
import java.util.UUID;

import org.mobicents.smsc.library.OriginationType;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.MProcNewMessage;
import org.mobicents.smsc.mproc.MProcRuleException;

/**
*
* @author sergey vetyutnev
*
*/
public class MProcUtility {
	
	private static final Random random = new Random();

    public static final int DataCodingGsm7 = 0;
    public static final int DataCodingGsm8 = 4;
    public static final int DataCodingUcs2 = 8;

    public static MProcNewMessage createNewEmptyMessage(int defaultValidityPeriodHours, int maxValidityPeriodHours, OriginationType originationType) {
        Sms sms = new Sms();

        sms.setDbId(UUID.randomUUID());
        sms.setOrigNetworkId(0);

        sms.setSourceAddr("111");
        sms.setSourceAddrNpi(1);
        sms.setSourceAddrTon(1);
        sms.setMessageId(0);

        sms.setShortMessageText("???");
        sms.setShortMessageBin(null);

        Date now = new Date();
        sms.setSubmitDate(now);
        sms.setValidityPeriod(addHours(now, defaultValidityPeriodHours));
        sms.setScheduleDeliveryTime(null);

        sms.setDataCoding(0);
        sms.setNationalLanguageLockingShift(0);
        sms.setNationalLanguageSingleShift(0);
        sms.setEsmClass(1); // datagram mode, normal message, no UDH

        sms.setPriority(0);
        sms.setRegisteredDelivery(0);

        sms.setOrigSystemId(null);
        sms.setOrigEsmeName(null);
        sms.setOriginationType(originationType);

        sms.setMoMessageRef(0);
        sms.setServiceType(null);
        sms.setProtocolId(0);

        sms.setReplaceIfPresent(0);
        sms.setDefaultMsgId(0);

        SmsSet smsSet = new SmsSet();
        smsSet.setDestAddr("222");
        smsSet.setDestAddrNpi(1);
        smsSet.setDestAddrTon(1);

        smsSet.setNetworkId(0);
        smsSet.setCorrelationId(null);
        smsSet.addSms(sms);

        return new MProcNewMessageImpl(sms, defaultValidityPeriodHours, maxValidityPeriodHours);
    }

    public static MProcNewMessage createNewCopyMessage(MProcMessage message, boolean backDest, int defaultValidityPeriodHours,
            int maxValidityPeriodHours) {
        MProcMessageImpl msg = (MProcMessageImpl) message;
        Sms sms0 = msg.getSmsContent();

        Sms sms = new Sms();

        sms.setDbId(UUID.randomUUID());
        sms.setOrigNetworkId(sms0.getOrigNetworkId());

        if (backDest) {
            sms.setSourceAddr(sms0.getSmsSet().getDestAddr());
            sms.setSourceAddrNpi(sms0.getSmsSet().getDestAddrNpi());
            sms.setSourceAddrTon(sms0.getSmsSet().getDestAddrTon());
        } else {
            sms.setSourceAddr(sms0.getSourceAddr());
            sms.setSourceAddrNpi(sms0.getSourceAddrNpi());
            sms.setSourceAddrTon(sms0.getSourceAddrTon());
        }
        sms.setMessageId(sms0.getMessageId());

        if (backDest) {
            sms.setShortMessageText("???");
            sms.setShortMessageBin(null);
        } else {
            sms.setShortMessageText(sms0.getShortMessageText());
            sms.setShortMessageBin(sms0.getShortMessageBin());
        }

        Date now = new Date();
        if (backDest) {
            sms.setSubmitDate(now);
            sms.setValidityPeriod(addHours(now, defaultValidityPeriodHours));
            sms.setScheduleDeliveryTime(null);
        } else {
            sms.setSubmitDate(sms0.getSubmitDate());
            sms.setValidityPeriod(sms0.getValidityPeriod());
            sms.setScheduleDeliveryTime(sms0.getScheduleDeliveryTime());
        }

        sms.setDataCoding(sms0.getDataCoding());
        sms.setNationalLanguageLockingShift(sms0.getNationalLanguageLockingShift());
        sms.setNationalLanguageSingleShift(sms0.getNationalLanguageSingleShift());
        if (backDest) {
            sms.setEsmClass(sms0.getEsmClass() & 0x03);
        } else {
            sms.setEsmClass(sms0.getEsmClass());
        }

        sms.setPriority(sms0.getPriority());
        if (backDest) {
        } else {
            sms.setRegisteredDelivery(sms0.getRegisteredDelivery());
        }

        if (backDest) {
        } else {
            sms.setOrigSystemId(sms0.getOrigSystemId());
            sms.setOrigEsmeName(sms0.getOrigEsmeName());
            sms.setOriginationType(sms0.getOriginationType());

            sms.setMoMessageRef(sms0.getMoMessageRef());
            sms.setServiceType(sms0.getServiceType());
            sms.setProtocolId(sms0.getProtocolId());

            sms.setReplaceIfPresent(sms0.getReplaceIfPresent());
        }

        if (backDest) {
        } else {
            sms.setDefaultMsgId(sms0.getDefaultMsgId());
            sms.getTlvSet().addAllOptionalParameter(sms0.getTlvSet().getOptionalParameters());
        }

        SmsSet smsSet = new SmsSet();
        if (backDest) {
            smsSet.setDestAddr(sms0.getSourceAddr());
            smsSet.setDestAddrNpi(sms0.getSourceAddrNpi());
            smsSet.setDestAddrTon(sms0.getSourceAddrTon());
        } else {
            smsSet.setDestAddr(sms0.getSmsSet().getDestAddr());
            smsSet.setDestAddrNpi(sms0.getSmsSet().getDestAddrNpi());
            smsSet.setDestAddrTon(sms0.getSmsSet().getDestAddrTon());
            smsSet.setCorrelationId(sms0.getSmsSet().getCorrelationId());
        }

        smsSet.setNetworkId(sms0.getSmsSet().getNetworkId());
        smsSet.addSms(sms);

        return new MProcNewMessageImpl(sms, defaultValidityPeriodHours, maxValidityPeriodHours);
    }

    public static Date addHours(Date time, int hours) {
        long tm = time.getTime();
        tm += hours * 3600 * 1000;
        return new Date(tm);
    }

    public static void checkDestAddrTon(int val) throws MProcRuleException {
        if (val < 0 || val > 6)
            throw new MProcRuleException("DestAddrTon must have values 0-6, found=" + val);
    }

    public static void checkDestAddrNpi(int val) throws MProcRuleException {
        if (val < 0 || val > 6)
            throw new MProcRuleException("DestAddrNpi must have values 0-6, found=" + val);
    }

    public static void checkDestAddr(String val) throws MProcRuleException {
        if (val == null)
            throw new MProcRuleException("DestAddr must not be null");
        if (val.length() == 0 || val.length() > 21)
            throw new MProcRuleException("DestAddr must have length 1-21, found=" + val.length());
    }

    public static void checkSourceAddrTon(int val) throws MProcRuleException {
        if (val < 0 || val > 6)
            throw new MProcRuleException("SourceAddrTon must have values 0-6, found=" + val);
    }

    public static void checkSourceAddrNpi(int val) throws MProcRuleException {
        if (val < 0 || val > 6)
            throw new MProcRuleException("SourceAddrNpi must have values 0-6, found=" + val);
    }

    public static void checkSourceAddr(String val) throws MProcRuleException {
        if (val == null)
            throw new MProcRuleException("SourceAddr must not be null");
        if (val.length() == 0 || val.length() > 21)
            throw new MProcRuleException("SourceAddr must have length 1-21, found=" + val.length());
    }
    
    public static void checkMtLocalSccpGt(String val) throws MProcRuleException {
        if (val == null)
            throw new MProcRuleException("MtLocalSccpGt must not be null");
    }
    
    public static void checkMtRemoteSccpTt(int val) throws MProcRuleException {
        if (val < 0 || val > 254)
            throw new MProcRuleException("MtRemoteSccpTt must be in 0-254 range, received=" + val);
        }

    public static void checkShortMessageText(String val) throws MProcRuleException {
        if (val == null)
            throw new MProcRuleException("ShortMessageText must not be null");
        if (val.length() > 4300)
            throw new MProcRuleException("ShortMessageText must have length 0-4300, found=" + val.length());
    }

    public static void checkShortMessageBin(byte[] val) throws MProcRuleException {
        if (val != null && val.length == 0)
            throw new MProcRuleException("ShortMessageBin must be null or has length > 0");
    }

    public static Date checkScheduleDeliveryTime(Sms sms, Date scheduleDeliveryTime) {
        Date maxSchDelTime = MProcUtility.addHours(sms.getValidityPeriod(), -3);
        if (scheduleDeliveryTime.after(maxSchDelTime)) {
            scheduleDeliveryTime = maxSchDelTime;
        }
        return scheduleDeliveryTime;
    }

    public static Date checkValidityPeriod(Date validityPeriod, int defaultValidityPeriodHours, int maxValidityPeriodHours) {
        Date now = new Date();
        if (validityPeriod == null) {
            validityPeriod = MProcUtility.addHours(now, defaultValidityPeriodHours);
        }
        Date maxValidityPeriod = MProcUtility.addHours(now, maxValidityPeriodHours);
        if (validityPeriod.after(maxValidityPeriod)) {
            validityPeriod = maxValidityPeriod;
        }
        if (validityPeriod.before(now)) {
            validityPeriod = maxValidityPeriod;
        }
        return validityPeriod;
    }

    public static int setEsmClass_ModeDatagram(int prevValue) {
        return (prevValue & 0xFC) + 1;
    }

    public static int setEsmClass_ModeTransaction(int prevValue) {
        return (prevValue & 0xFC) + 2;
    }

    public static int setEsmClass_ModeStoreAndForward(int prevValue) {
        return (prevValue & 0xFC) + 3;
    }

    public static int setEsmClass_TypeNormalMessage(int prevValue) {
        return (prevValue & 0xC3) + 0;
    }

    public static int setEsmClass_TypeDeliveryReceipt(int prevValue) {
        return (prevValue & 0xC3) + 4;
    }

    public static int setEsmClass_UDHIndicatorPresent(int prevValue) {
        return (prevValue & 0xBF) + 0x40;
    }

    public static int setEsmClass_UDHIndicatorAbsent(int prevValue) {
        return (prevValue & 0xBF) + 0;
    }

    public static int setRegisteredDelivery_DeliveryReceiptNo(int prevValue) {
        return (prevValue & 0xFC) + 0;
    }

    public static int setRegisteredDelivery_DeliveryReceiptOnSuccessOrFailure(int prevValue) {
        return (prevValue & 0xFC) + 1;
    }

    public static int setRegisteredDelivery_DeliveryReceiptOnFailure(int prevValue) {
        return (prevValue & 0xFC) + 2;
    }

    public static int setRegisteredDelivery_DeliveryReceiptOnSuccess(int prevValue) {
        return (prevValue & 0xFC) + 3;
    }
    
    public static boolean checkRuleProbability(int percent) {
    	int r = random.nextInt(100);
    	return percent >= r;
    }
}
