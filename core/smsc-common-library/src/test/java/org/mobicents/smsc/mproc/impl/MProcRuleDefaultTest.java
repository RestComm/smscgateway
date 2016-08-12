package org.mobicents.smsc.mproc.impl;

import org.testng.annotations.Test;

public class MProcRuleDefaultTest {

    @Test(groups = { "MProcRule" })
    public void testA() throws Exception {
        MProcRuleDefaultImpl pmr = new MProcRuleDefaultImpl();
        pmr.updateRuleParameters("processingtype SIP");
    }
}
