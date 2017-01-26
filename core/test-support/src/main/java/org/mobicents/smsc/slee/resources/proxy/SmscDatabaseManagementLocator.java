package org.mobicents.smsc.slee.resources.proxy;

import org.mobicents.smsc.domain.SmscDatabaseManagement;
import org.springframework.beans.factory.InitializingBean;

public class SmscDatabaseManagementLocator implements InitializingBean {

    private String itsName;

    /**
     * Sets the name.
     *
     * @param aName the new name
     */
    public void setName(final String aName) {
        itsName = aName;
    }

    /**
     * Gets the single instance of SmscManagement.
     *
     * @return single instance of SmscManagement
     */
    public SmscDatabaseManagement getInstance() {
        return SmscDatabaseManagement.getInstance();
    }

    @Override
    public void afterPropertiesSet() {
        SmscDatabaseManagement.getInstance(itsName).start();
    }

}
