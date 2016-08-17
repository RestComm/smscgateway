package org.mobicents.smsc.tools.stresstool;

import java.util.Date;

import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.PreparedStatementCollection;

import com.datastax.driver.core.Session;

public class TT_DBOperationsProxy3 extends DBOperations {

    public Session getSession() {
        return this.session;
    }

    public PreparedStatementCollection getStatementCollection(Date dt) throws PersistenceException {
        return super.getStatementCollection(dt);
    }

}
