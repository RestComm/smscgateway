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

package org.mobicents.smsc.slee.resources.persistence;

import static org.testng.Assert.*;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.library.MessageUtil;
import org.testng.annotations.Test;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class TimeProcessingTest {

	private String getTimeA1() {
		return "020615103429512+";
	}

	private String getTimeA2() {
		return "020615103429512-";
	}

	private String getTimeA3() {
		return "020615123429512+";
	}

	private String getTimeRel() {
		return "000000013000000R";
	}

	@Test(groups = { "timeProcessing" })
	public void testDateDecoding() throws Exception {
		Date curDate = new Date(2013, 7, 1); // summer time
		int i1 = curDate.getTimezoneOffset();

		Date d1 = MessageUtil.parseSmppDate(getTimeA1());
		Date d11 = (new GregorianCalendar(2002, 05, 15, 10, 34, 29)).getTime();
		Date d111 = new Date(d11.getTime() - 3 * 3600 * 1000 - i1 * 60 * 1000 + 5 * 100);
		assertTrue(d1.equals(d111));

		Date d2 = MessageUtil.parseSmppDate(getTimeA2());
		Date d22 = (new GregorianCalendar(2002, 05, 15, 10, 34, 29)).getTime();
		Date d222 = new Date(d22.getTime() + 3 * 3600 * 1000 - i1 * 60 * 1000 + 5 * 100);
		assertTrue(d2.equals(d222));

		d2 = MessageUtil.parseSmppDate(getTimeA3());
		d22 = (new GregorianCalendar(2002, 05, 15, 12, 34, 29)).getTime();
		d222 = new Date(d22.getTime() - 3 * 3600 * 1000 - i1 * 60 * 1000 + 5 * 100);
		assertTrue(d2.equals(d222));

        curDate = new Date(); // current time
		Date d3 = MessageUtil.parseSmppDate(getTimeRel());
		Date d333 = new Date(curDate.getTime() + 90 * 60 * 1000);
		this.testDateEq(d3, d333);
	}

	@Test(groups = { "timeProcessing" })
	public void testDateEncoding() throws Exception {
		Calendar c1 = new GregorianCalendar(2002, 05, 15, 10, 34, 29);
		c1.add(Calendar.MILLISECOND, 500);
		Date d1 = c1.getTime();
		String s1 = MessageUtil.printSmppAbsoluteDate(d1, 180);
		assertEquals(s1, getTimeA1());

		String s2 = MessageUtil.printSmppAbsoluteDate(d1, -180);
		assertEquals(s2, getTimeA2());

		c1 = new GregorianCalendar(2002, 05, 15, 12, 34, 29);
		c1.add(Calendar.MILLISECOND, 500);
		d1 = c1.getTime();
		s1 = MessageUtil.printSmppAbsoluteDate(d1, 180);
		assertEquals(s1, getTimeA3());

		String s3 = MessageUtil.printSmppRelativeDate(0, 0, 0, 1, 30, 0);
		assertEquals(s3, getTimeRel());
	}

	@Test(groups = { "timeProcessing" })
	public void testScheduleDeliveryTime_ValidityPeriod() throws Exception {
		SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance("TestSmscPropertiesManagement");




//		Date curDate = new Date();
//		Date curDate3 = MessageUtil.addHours(curDate, 24 * 3);
//		SimpleDateFormat dateFormat = new SimpleDateFormat(smscPropertiesManagement.getDateFormat());
//
//		Sms sms = new Sms();
//		String scheduleDeliveryTimeS = null;
//		String validityPeriodS = null;
//		MessageUtil.fillScheduleDeliveryTime_ValidityPeriod(sms, MessageUtil.parseSmppDate(scheduleDeliveryTimeS, null),
//				MessageUtil.parseSmppDate(validityPeriodS, null));
//		assertNull(sms.getScheduleDeliveryTime());
//		testDateEq(sms.getValidityPeriod(), curDate3);
//
//		sms = new Sms();
//		scheduleDeliveryTimeS = dateFormat.format(MessageUtil.addHours(curDate, 24 * 1));
//		validityPeriodS = null;
//		MessageUtil.fillScheduleDeliveryTime_ValidityPeriod(sms, MessageUtil.parseSmppDate(scheduleDeliveryTimeS, null),
//				MessageUtil.parseSmppDate(validityPeriodS, null));
//		testDateEq(sms.getScheduleDeliveryTime(), MessageUtil.addHours(curDate, 24 * 1));
//		testDateEq(sms.getValidityPeriod(), curDate3);
//
//		sms = new Sms();
//		scheduleDeliveryTimeS = dateFormat.format(MessageUtil.addHours(curDate, 24 * 4));
//		validityPeriodS = null;
//		MessageUtil.fillScheduleDeliveryTime_ValidityPeriod(sms, MessageUtil.parseSmppDate(scheduleDeliveryTimeS, null),
//				MessageUtil.parseSmppDate(validityPeriodS, null));
//		testDateEq(sms.getScheduleDeliveryTime(), MessageUtil.addHours(curDate, 24 * 4));
//		testDateEq(sms.getValidityPeriod(), curDate3);
//
//		sms = new Sms();
//		scheduleDeliveryTimeS = null;
//		validityPeriodS = dateFormat.format(MessageUtil.addHours(curDate, 24 * 1));
//		MessageUtil.fillScheduleDeliveryTime_ValidityPeriod(sms, MessageUtil.parseSmppDate(scheduleDeliveryTimeS, null),
//				MessageUtil.parseSmppDate(validityPeriodS, null));
//		assertNull(sms.getScheduleDeliveryTime());
//		testDateEq(sms.getValidityPeriod(), MessageUtil.addHours(curDate, 24 * 1));
//
//		sms = new Sms();
//		scheduleDeliveryTimeS = null;
//		validityPeriodS = dateFormat.format(MessageUtil.addHours(curDate, 24 * 4));
//		MessageUtil.fillScheduleDeliveryTime_ValidityPeriod(sms, MessageUtil.parseSmppDate(scheduleDeliveryTimeS, null),
//				MessageUtil.parseSmppDate(validityPeriodS, null));
//		assertNull(sms.getScheduleDeliveryTime());
//		testDateEq(sms.getValidityPeriod(), curDate3);
//
//		scheduleDeliveryTimeS = "2013-04-05 12:00:00+0200";
//		validityPeriodS = "2013-04-06 11:00:00+0200";
	}

	private void testDateEq(Date d1, Date d2) {
		// creating d3 = d1 + 2 min

		long tm = d2.getTime();
		tm -= 2 * 60 * 1000;
		Date d3 = new Date(tm);

		tm = d2.getTime();
		tm += 2 * 60 * 1000;
		Date d4 = new Date(tm);

		assertTrue(d1.after(d3));
		assertTrue(d1.before(d4));
	}

}

