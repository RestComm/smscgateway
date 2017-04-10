package org.mobicents.smsc.mproc.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.ProcessingType;
import org.restcomm.smpp.parameter.TlvSet;
import org.testng.annotations.Test;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.tlv.Tlv;

public class MProcRuleDefaultTest {

    @Test(groups = { "MProcRule" })
    public void testA() throws Exception {
        Sms sms = new Sms();
        SmsSet smsSet = new SmsSet();
        sms.setSmsSet(smsSet);
        smsSet.setStatus(ErrorCode.ABSENT_SUBSCRIBER); // code 8
        MProcMessage message = new MProcMessageImpl(sms, ProcessingType.SMPP, null);

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

    @Test(groups = { "MProcRule" })
    public void testTlvOptions() throws Exception {
        Sms sms = new Sms();
        MProcRuleDefaultImpl pmr = null;
        MProcMessage message = null;
        TlvSet tlvSet = null;

        //FIXME:add more failing tests
        //test tlvbyte_*
        tlvSet = new TlvSet();
        tlvSet.addOptionalParameter(new Tlv(SmppConstants.TAG_DEST_NETWORK_ID, new byte[]{7}));
        sms.setTlvSet(tlvSet);
        message = new MProcMessageImpl(sms, ProcessingType.SMPP, null);

        pmr = new MProcRuleDefaultImpl();
        pmr.setInitialRuleParameters("tlv_byte_1550 7");
        assertEquals(pmr.getRuleParameters(), "tlvByte_1550=7", "error");
        assertTrue(pmr.matchesPostArrival(message));

        tlvSet = new TlvSet();
        tlvSet.addOptionalParameter(new Tlv(SmppConstants.TAG_DEST_NETWORK_ID, new byte[]{13}));
        sms.setTlvSet(tlvSet);
        message = new MProcMessageImpl(sms, ProcessingType.SMPP, null);
        assertFalse(pmr.matchesPostArrival(message));

        pmr.setInitialRuleParameters("tlv_byte_1550 13");
        assertEquals(pmr.getRuleParameters(), "tlvByte_1550=13", "error");
        assertTrue(pmr.matchesPostArrival(message));

        //test tlvint_*
        tlvSet = new TlvSet();
        tlvSet.addOptionalParameter(new Tlv(SmppConstants.TAG_DEST_NETWORK_ID, new byte[]{0,0,0,7}));
        sms.setTlvSet(tlvSet);
        message = new MProcMessageImpl(sms, ProcessingType.SMPP, null);

        pmr = new MProcRuleDefaultImpl();
        pmr.setInitialRuleParameters("tlv_int_1550 7");
        assertEquals(pmr.getRuleParameters(), "tlvInt_1550=7", "error");
        assertTrue(pmr.matchesPostArrival(message));

        tlvSet = new TlvSet();
        tlvSet.addOptionalParameter(new Tlv(SmppConstants.TAG_DEST_NETWORK_ID, new byte[]{0,0,0,13}));
        sms.setTlvSet(tlvSet);
        message = new MProcMessageImpl(sms, ProcessingType.SMPP, null);
        assertFalse(pmr.matchesPostArrival(message));

        pmr.setInitialRuleParameters("tlv_int_1550 13");
        assertEquals(pmr.getRuleParameters(), "tlvInt_1550=13", "error");
        assertTrue(pmr.matchesPostArrival(message));

        //test tlvstring_*
        tlvSet = new TlvSet();
        tlvSet.addOptionalParameter(new Tlv(SmppConstants.TAG_DEST_NETWORK_ID, "7".getBytes()));
        sms.setTlvSet(tlvSet);
        message = new MProcMessageImpl(sms, ProcessingType.SMPP, null);

        pmr = new MProcRuleDefaultImpl();
        pmr.setInitialRuleParameters("tlv_string_1550 7");
        assertEquals(pmr.getRuleParameters(), "tlvString_1550=7", "error");
        assertTrue(pmr.matchesPostArrival(message));

        tlvSet = new TlvSet();
        tlvSet.addOptionalParameter(new Tlv(SmppConstants.TAG_DEST_NETWORK_ID, "13".getBytes()));
        sms.setTlvSet(tlvSet);
        message = new MProcMessageImpl(sms, ProcessingType.SMPP, null);
        assertFalse(pmr.matchesPostArrival(message));

        pmr.setInitialRuleParameters("tlv_string_1550 13");
        assertEquals(pmr.getRuleParameters(), "tlvString_1550=13", "error");
        assertTrue(pmr.matchesPostArrival(message));
    }
}
