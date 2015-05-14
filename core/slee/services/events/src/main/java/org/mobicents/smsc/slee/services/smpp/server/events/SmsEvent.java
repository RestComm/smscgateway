package org.mobicents.smsc.slee.services.smpp.server.events;

import java.io.Serializable;

import org.mobicents.smsc.domain.library.Sms;

/**
 * 
 * @author amit bhayani
 * @author sergey vetyutnev
 *
 */
public class SmsEvent implements Serializable {

    private static final long serialVersionUID = 3064061597891865748L;

    private Sms sms;

    public Sms getSms() {
        return sms;
    }

    public void setSms(Sms sms) {
        this.sms = sms;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SmsEvent [");

        if (this.sms != null) {
            sb.append(this.sms.toString());
        }

        sb.append("]");
        return sb.toString();
    }

}
