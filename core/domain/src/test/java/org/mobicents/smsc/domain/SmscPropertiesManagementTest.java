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

        man.stop();

        man.start();

        assertEquals(man.getFirstDueDelay(), 678);
        assertEquals(man.getServiceCenterGt(1), "22229");
        assertEquals(man.getServiceCenterGt(2), "0");

    }
}
