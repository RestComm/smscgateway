package org.mobicents.smsc.slee.services.mt;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import org.mobicents.protocols.ss7.map.api.smstpdu.AbsoluteTimeStamp;
import org.mobicents.protocols.ss7.map.api.smstpdu.AddressField;
import org.mobicents.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.mobicents.protocols.ss7.map.api.smstpdu.ProtocolIdentifier;
import org.mobicents.protocols.ss7.map.api.smstpdu.TypeOfNumber;
import org.mobicents.protocols.ss7.map.service.sms.SmsSignalInfoImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AbsoluteTimeStampImpl;
import org.mobicents.protocols.ss7.map.smstpdu.AddressFieldImpl;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.protocols.ss7.map.smstpdu.ProtocolIdentifierImpl;
import org.mobicents.protocols.ss7.map.smstpdu.SmsDeliverTpduImpl;
import org.mobicents.protocols.ss7.map.smstpdu.UserDataImpl;
import org.testng.annotations.Test;

public class MtTest2 {

    @Test(groups = { "Mt" })
    public void Ucs2Test() throws Exception {

//        Long msgId = 15L;
//        String s1 = msgId.toString(10);
//        String sss2 = String.format("%010d", 15L);


        String s11 = "زمانیکه بررسی";
        Charset ucs2Charset = Charset.forName("UTF-16BE");
        Charset utf8 = Charset.forName("UTF-8");
//        ByteBuffer bb = ByteBuffer.wrap(textPart);
//        CharBuffer bf = ucs2Charset.decode(bb);
//        msg = bf.toString();
        ByteBuffer bb = utf8.encode(s11);
        byte[] buf = new byte[bb.limit()];
        bb.get(buf, 0, bb.limit());
        String s2 = new String(buf);

        UserDataImpl ud = new UserDataImpl(s2, new DataCodingSchemeImpl(8), null, null);
        AddressField originatingAddress = new AddressFieldImpl(TypeOfNumber.InternationalNumber, NumberingPlanIdentification.ISDNTelephoneNumberingPlan,
                "123456");
        ProtocolIdentifier pi = new ProtocolIdentifierImpl(0);
        AbsoluteTimeStamp serviceCentreTimeStamp = new AbsoluteTimeStampImpl(05, 3, 4, 5, 6, 7, 0);
        SmsDeliverTpduImpl smsDeliverTpduImpl = new SmsDeliverTpduImpl(false, false, false, true, originatingAddress, pi, serviceCentreTimeStamp, ud);

        SmsSignalInfoImpl SmsSignalInfoImpl = new SmsSignalInfoImpl(smsDeliverTpduImpl, null);

        int gg=0;
        gg++;
    }

    @Test(groups = { "Mt" })
    public void Ucs2Test2() throws Exception {
        byte[] msg1 = new byte[] { (byte) 0xd8, (byte) 0xa7, (byte) 0xdb, (byte) 0x8c, (byte) 0xda, (byte) 0xa9, (byte) 0xd8, (byte) 0xb3, (byte) 0xd9,
                (byte) 0xbe, (byte) 0xd8, (byte) 0xb1, (byte) 0xdb, (byte) 0x8c, (byte) 0xd8, (byte) 0xb3, 0x20, (byte) 0xd9, (byte) 0x86, (byte) 0xdb,
                (byte) 0x8c, (byte) 0xd9, (byte) 0x88, (byte) 0xd8, (byte) 0xb2, (byte) 0xd8, (byte) 0xa7, (byte) 0xdb, (byte) 0x8c, (byte) 0xda, (byte) 0xa9,
                (byte) 0xd8, (byte) 0xb3, (byte) 0xd9, (byte) 0xbe, (byte) 0xd8, (byte) 0xb1, (byte) 0xdb, (byte) 0x8c, (byte) 0xd8, (byte) 0xb3, 0x20,
                (byte) 0xd9, (byte) 0x86, (byte) 0xdb, (byte) 0x8c, (byte) 0xd9, (byte) 0x88, (byte) 0xd8, (byte) 0xb2, (byte) 0xd8, (byte) 0xa7, (byte) 0xdb,
                (byte) 0x8c, (byte) 0xda, (byte) 0xa9, (byte) 0xd8, (byte) 0xb3, (byte) 0xd9, (byte) 0xbe, (byte) 0xd8, (byte) 0xb1, (byte) 0xdb, (byte) 0x8c,
                (byte) 0xd8, (byte) 0xb3, 0x20, (byte) 0xd9, (byte) 0x86, (byte) 0xdb, (byte) 0x8c, (byte) 0xd9, (byte) 0x88, (byte) 0xd8, (byte) 0xb2,
                (byte) 0xd8, (byte) 0xa7, (byte) 0xdb, (byte) 0x8c, (byte) 0xda, (byte) 0xa9, (byte) 0xd8, (byte) 0xb3, (byte) 0xd9, (byte) 0xbe, (byte) 0xd8,
                (byte) 0xb1, (byte) 0xdb, (byte) 0x8c, (byte) 0xd8, (byte) 0xb3, 0x20, (byte) 0xd9, (byte) 0x86, (byte) 0xdb, (byte) 0x8c, (byte) 0xd9,
                (byte) 0x88, (byte) 0xd8, (byte) 0xb2, (byte) 0xd8, (byte) 0xa7, (byte) 0xdb, (byte) 0x8c, (byte) 0xda, (byte) 0xa9, (byte) 0xd8, (byte) 0xb3,
                (byte) 0xd9, (byte) 0xbe, (byte) 0xd8, (byte) 0xb1, (byte) 0xdb, (byte) 0x8c, (byte) 0xd8, (byte) 0xb3, 0x20, (byte) 0xd9, (byte) 0x86,
                (byte) 0xdb, (byte) 0x8c, (byte) 0xd9, (byte) 0x88, (byte) 0xd8, (byte) 0xb2, (byte) 0xd8, (byte) 0xa7, (byte) 0xdb, (byte) 0x8c, (byte) 0xda,
                (byte) 0xa9, (byte) 0xd8, (byte) 0xb3, (byte) 0xd9, (byte) 0xbe, (byte) 0xd8, (byte) 0xb1, (byte) 0xdb, (byte) 0x8c, (byte) 0xd8 };

        int len1 = msg1.length;
        Charset utf8 = Charset.forName("UTF-8");
        ByteBuffer bb = ByteBuffer.wrap(msg1);
        CharBuffer cb = utf8.decode(bb);
        String s1 = cb.toString();
        int len2 = s1.length();
    }

    @Test(groups = { "Mt" })
    public void Ucs2Test3() throws Exception {

        String s11 = "ura nus";
        byte[] buf = s11.getBytes();

        UserDataImpl ud = new UserDataImpl(s11, new DataCodingSchemeImpl(16), null, null);
        AddressField originatingAddress = new AddressFieldImpl(TypeOfNumber.InternationalNumber, NumberingPlanIdentification.ISDNTelephoneNumberingPlan,
                "123456");
        ProtocolIdentifier pi = new ProtocolIdentifierImpl(0);
        AbsoluteTimeStamp serviceCentreTimeStamp = new AbsoluteTimeStampImpl(05, 3, 4, 5, 6, 7, 0);
        SmsDeliverTpduImpl smsDeliverTpduImpl = new SmsDeliverTpduImpl(false, false, false, true, originatingAddress, pi, serviceCentreTimeStamp, ud);

        SmsSignalInfoImpl SmsSignalInfoImpl = new SmsSignalInfoImpl(smsDeliverTpduImpl, null);

        int gg=0;
        gg++;
    }
}
