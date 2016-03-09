package org.mobicents.smsc.domain;

import static org.testng.Assert.*;

import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.testng.annotations.Test;

public class SmscPropertiesManagementTest {

    @Test(groups = { "management" })
    public void testPropertiesLoad() throws Exception {
        SmscPropertiesManagement man = SmscPropertiesManagement.getInstance("SmscPropertiesManagementTest");

        
        man.start();

        man.setFirstDueDelay(678);
        man.setServiceCenterGt(1, "22229");

        man.setHrHlrNumber("00000");
        man.setHrHlrNumber(2, "22222");

        man.setHrSriBypass(true);
        man.setHrSriBypass(3, false);
        man.setHrSriBypass(4, false);

        man.stop();

        man.start();

        assertEquals(man.getFirstDueDelay(), 678);
        assertEquals(man.getServiceCenterGt(1), "22229");
        assertEquals(man.getServiceCenterGt(2), "0");

        assertEquals(man.getHrHlrNumber(0), "00000");
        assertEquals(man.getHrHlrNumber(2), "22222");

        assertTrue(man.getHrSriBypass(0));
        assertFalse(man.getHrSriBypass(3));
        assertFalse(man.getHrSriBypass(4));

        man.removeHrSriBypassForNetworkId(4);
        
        assertTrue(man.getHrSriBypass(0));
        assertFalse(man.getHrSriBypass(3));
        assertTrue(man.getHrSriBypass(4));

    }
}
