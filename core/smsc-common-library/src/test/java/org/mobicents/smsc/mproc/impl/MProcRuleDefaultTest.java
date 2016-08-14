package org.mobicents.smsc.mproc.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.ProcessingType;
import org.testng.annotations.Test;

public class MProcRuleDefaultTest {

    @Test(groups = { "MProcRule" })
    public void testA() throws Exception {
        Sms sms = new Sms();
        SmsSet smsSet = new SmsSet();
        sms.setSmsSet(smsSet);
        smsSet.setStatus(ErrorCode.ABSENT_SUBSCRIBER); // code 8
        MProcMessage message = new MProcMessageImpl(sms, ProcessingType.SMPP);

        MProcRuleDefaultImpl pmr = new MProcRuleDefaultImpl();

        pmr.updateRuleParameters("processingtype SIP");
        assertEquals(pmr.getProcessingType(), ProcessingType.SIP);

        pmr.updateRuleParameters("processingtype -1");
        pmr.updateRuleParameters("errorcode 6");
        boolean res = pmr.matchesPostArrival(message);
        assertFalse(res);

        pmr.updateRuleParameters("errorcode 6,7,8");
        res = pmr.matchesPostArrival(message);
        assertTrue(res);
    }
}
