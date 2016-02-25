package org.mobicents.smsc.tools.stresstool;

import com.datastax.driver.core.Session;

public class TT_DBOperationsProxy2 extends NN_DBOper {

    public Session getSession() {
        return this.session;
    }

}
