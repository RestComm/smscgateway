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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.library.SmsSetCache;

/**
 * 
 * @author Amit Bhayani
 * 
 */
public class SmscStatProvider implements SmscStatProviderMBean {
	private static final Logger logger = Logger.getLogger(SmscStatProvider.class);

	private long messageScheduledTotal = 0;
	private long messagesPendingInDatabase = 0;
	private int param1 = 0;
	private int param2 = 0;
	private long currentMessageId = 0;
	private Date smscStartTime = new Date();

	private static SmscStatProvider instance = new SmscStatProvider();

	private static final SmsSetCache smsSetCashe = SmsSetCache.getInstance();
	private static final DBOperations dbOperations_C2 = DBOperations.getInstance();

	public static SmscStatProvider getInstance() {
		return instance;
	}

	public int getMessageInProcess() {
		return smsSetCashe.getProcessingSmsSetSize();
	}

    public int getDueSlotProcessingLag() {
        long current = dbOperations_C2.c2_getCurrentDueSlot();
        long inTime = dbOperations_C2.c2_getDueSlotForTime(new Date());
        return (int) (inTime - current);
    }

    public Date getDueSlotProcessingTime() {
        long current = dbOperations_C2.c2_getCurrentDueSlot();
        Date currentDate = dbOperations_C2.c2_getTimeForDueSlot(current);
        return currentDate;
    }
	
	public long getMessageScheduledTotal() {
        return messageScheduledTotal;
    }

    public void setMessageScheduledTotal(long messageScheduledTotal) {
        this.messageScheduledTotal = messageScheduledTotal;
    }
    
    public long getMessagesPendingInDatabase() {
        return smsSetCashe.getMessagesStoredInDatabase() - smsSetCashe.getMessagesSentInDatabase();
    }

	public int getParam1() {
		return param1;
	}

	public void setParam1(int param1) {
		this.param1 = param1;
	}

	public int getParam2() {
		return param2;
	}

	public void setParam2(int param2) {
		this.param2 = param2;
	}

	public long getCurrentMessageId() {
		return currentMessageId;
	}

	public void setCurrentMessageId(long currentMessageId) {
		this.currentMessageId = currentMessageId;
	}

	public Date getSmscStartTime() {
		return smscStartTime;
	}

	public void setSmscStartTime(Date smscStartTime) {
		this.smscStartTime = smscStartTime;
	}

}
