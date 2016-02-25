package org.mobicents.smsc.tools.stresstool;

import java.util.Date;

import org.mobicents.smsc.cassandra.DBOperations_C2;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.PreparedStatementCollection_C3;

import com.datastax.driver.core.Session;

public class TT_DBOperationsProxy3 extends DBOperations_C2 {

    public Session getSession() {
        return this.session;
    }

    public PreparedStatementCollection_C3 getStatementCollection(Date dt) throws PersistenceException {
        return super.getStatementCollection(dt);
    }

}
