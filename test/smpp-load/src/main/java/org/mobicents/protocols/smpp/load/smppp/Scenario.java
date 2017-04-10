package org.mobicents.protocols.smpp.load.smppp;

import com.cloudhopper.smpp.SmppSessionListener;

public interface Scenario extends SmppSessionListener {
    void init(GlobalContext ctx);
    void createDialog(GlobalContext ctx) throws Exception ;
}
