/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.smsc.cassandra;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.slee.facilities.Tracer;

import javolution.util.FastList;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.map.api.primitives.IMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.smsc.library.DbSmsRoutingRule;
import org.mobicents.smsc.library.ErrorCode;
import org.mobicents.smsc.library.SmType;
import org.mobicents.smsc.library.Sms;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.library.SmsSetCache;
import org.mobicents.smsc.library.TargetAddress;
import org.mobicents.smsc.smpp.TlvSet;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

/**
 * 
 * @author baranowb
 * @author sergey vetyutnev
 * 
 */
public class DBOperations_C1 {
	private static final Logger logger = Logger.getLogger(DBOperations_C1.class);

	public static final String TLV_SET = "tlvSet";

	protected Cluster cluster;
	protected Session session;

	private PreparedStatement smsSetExist;
	private PreparedStatement obtainSmsSet;
	private PreparedStatement obtainSmsSet2;
	private PreparedStatement setNewMessageScheduled;
	private PreparedStatement setDeliveringProcessScheduled;
	private PreparedStatement setDeliveryStart;
	private PreparedStatement setDeliveryStart2;
	private PreparedStatement setDeliverySuccess;
	private PreparedStatement setDeliveryFailure;
	private PreparedStatement setAlertingSupported;
	private PreparedStatement deleteSmsSet;
	private PreparedStatement createLiveSms;
	private PreparedStatement obtainLiveSms;
	private PreparedStatement obtainLiveSms2;
	// private PreparedStatement doFetchSchedulableSmsSets;
	// private PreparedStatement doFetchSchedulableSmsSets2;
	private PreparedStatement fetchSchedulableSms;
	private PreparedStatement getSmsRoutingRule;
	private PreparedStatement updateDbSmsRoutingRule;
	private PreparedStatement deleteDbSmsRoutingRule;
	private PreparedStatement getSmsRoutingRulesRange;
	private PreparedStatement getSmsRoutingRulesRange2;
	private PreparedStatement deleteLiveSms;
	private PreparedStatement doArchiveDeliveredSms;

	private static final DBOperations_C1 instance = new DBOperations_C1();

	private volatile boolean started = false;

	protected DBOperations_C1() {
		super();
	}

	public static DBOperations_C1 getInstance() {
		return instance;
	}

	public boolean isStarted() {
		return started;
	}

	protected Session getSession() {
		return this.session;
	}

	public void start(String ip, int port, String keyspace) throws Exception {
		if (this.started) {
			throw new Exception("DBOperations already started");
		}

		Builder builder = Cluster.builder();

		builder.withPort(port);
		builder.addContactPoint(ip);

		this.cluster = builder.build();
		Metadata metadata = cluster.getMetadata();

		logger.info(String.format("Connected to cluster: %s\n", metadata.getClusterName()));
		for (Host host : metadata.getAllHosts()) {
			logger.info(String.format("Datacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(),
					host.getRack()));
		}

		session = cluster.connect();

		session.execute("USE \"" + keyspace + "\"");

		smsSetExist = session.prepare("SELECT count(*) FROM \"LIVE\" WHERE \"TARGET_ID\"=?;");
		obtainSmsSet = session.prepare("select * from \"" + Schema.FAMILY_LIVE + "\" where \""
				+ Schema.COLUMN_TARGET_ID + "\"=?;");
		obtainSmsSet2 = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE + "\" (\"" + Schema.COLUMN_TARGET_ID
				+ "\", \"" + Schema.COLUMN_ADDR_DST_DIGITS + "\", \"" + Schema.COLUMN_ADDR_DST_TON + "\", \""
				+ Schema.COLUMN_ADDR_DST_NPI + "\", \"" + Schema.COLUMN_IN_SYSTEM + "\") VALUES (?, ?, ?, ?, ?);");
		setNewMessageScheduled = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE + "\" (\""
				+ Schema.COLUMN_TARGET_ID + "\", \"" + Schema.COLUMN_DUE_DATE + "\", \"" + Schema.COLUMN_IN_SYSTEM
				+ "\", \"" + Schema.COLUMN_DUE_DELAY + "\") VALUES (?, ?, ?, ?);");
		setDeliveringProcessScheduled = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE + "\" (\""
				+ Schema.COLUMN_TARGET_ID + "\", \"" + Schema.COLUMN_DUE_DATE + "\", \"" + Schema.COLUMN_IN_SYSTEM
				+ "\", \"" + Schema.COLUMN_DUE_DELAY + "\") VALUES (?, ?, ?, ?);");
		setDeliveryStart = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE + "\" (\"" + Schema.COLUMN_TARGET_ID
				+ "\", \"" + Schema.COLUMN_IN_SYSTEM + "\", \"" + Schema.COLUMN_IN_SYSTEM_DATE
				+ "\") VALUES (?, ?, ?);");
		setDeliveryStart2 = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE_SMS + "\" (\"" + Schema.COLUMN_ID
				+ "\", \"" + Schema.COLUMN_DELIVERY_COUNT + "\") VALUES (?, ?);");
		setDeliverySuccess = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE + "\" (\"" + Schema.COLUMN_TARGET_ID
				+ "\", \"" + Schema.COLUMN_IN_SYSTEM + "\", \"" + Schema.COLUMN_SM_STATUS + "\") VALUES (?, ?, ?);");
		setDeliveryFailure = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE + "\" (\"" + Schema.COLUMN_TARGET_ID
				+ "\", \"" + Schema.COLUMN_IN_SYSTEM + "\", \"" + Schema.COLUMN_SM_STATUS + "\", \""
				+ Schema.COLUMN_ALERTING_SUPPORTED + "\") VALUES (?, ?, ?, ?);");
		setAlertingSupported = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE + "\" (\""
				+ Schema.COLUMN_TARGET_ID + "\", \"" + Schema.COLUMN_ALERTING_SUPPORTED + "\") VALUES (?, ?);");
		deleteSmsSet = session.prepare("delete from \"" + Schema.FAMILY_LIVE + "\" where \"" + Schema.COLUMN_TARGET_ID
				+ "\"=?;");
		String s1 = getFillUpdateFields();
		String s2 = getFillUpdateFields2();
		createLiveSms = session.prepare("INSERT INTO \"" + Schema.FAMILY_LIVE_SMS + "\" (\"" + Schema.COLUMN_TARGET_ID
				+ "\", " + s1 + ") VALUES (? " + s2 + ");");
		obtainLiveSms = session.prepare("select * from \"" + Schema.FAMILY_LIVE_SMS + "\" where \"" + Schema.COLUMN_ID
				+ "\"=?;");
		obtainLiveSms2 = session.prepare("select * from \"" + Schema.FAMILY_LIVE_SMS + "\" where \""
				+ Schema.COLUMN_MESSAGE_ID + "\"=?;");
		// doFetchSchedulableSmsSets = session.prepare("select * from \"" +
		// Schema.FAMILY_LIVE + "\" where \"" + Schema.COLUMN_IN_SYSTEM +
		// "\"=? and \""
		// + Schema.COLUMN_IN_SYSTEM_DATE + "\"<=?  LIMIT ?  ALLOW FILTERING;");
		// doFetchSchedulableSmsSets2 = session.prepare("select * from \"" +
		// Schema.FAMILY_LIVE + "\" where \"" + Schema.COLUMN_IN_SYSTEM +
		// "\"=? and \""
		// + Schema.COLUMN_DUE_DATE + "\"<=?  LIMIT ?  ALLOW FILTERING;");
		fetchSchedulableSms = session.prepare("select * from \"" + Schema.FAMILY_LIVE_SMS + "\" where \""
				+ Schema.COLUMN_TARGET_ID + "\"=?;");
		getSmsRoutingRule = session.prepare("select * from \"" + Schema.FAMILY_SMPP_SMS_ROUTING_RULE + "\" where \""
				+ Schema.COLUMN_ADDRESS + "\"=?;");
		updateDbSmsRoutingRule = session.prepare("INSERT INTO \"" + Schema.FAMILY_SMPP_SMS_ROUTING_RULE + "\" (\""
				+ Schema.COLUMN_ADDRESS + "\", \"" + Schema.COLUMN_CLUSTER_NAME + "\") VALUES (?, ?);");
		deleteDbSmsRoutingRule = session.prepare("delete from \"" + Schema.FAMILY_SMPP_SMS_ROUTING_RULE + "\" where \""
				+ Schema.COLUMN_ADDRESS + "\"=?;");
		int row_count = 100;
		getSmsRoutingRulesRange = session.prepare("select * from \"" + Schema.FAMILY_SMPP_SMS_ROUTING_RULE
				+ "\" where token(\"" + Schema.COLUMN_ADDRESS + "\") >= token(?) LIMIT " + row_count + ";");
		getSmsRoutingRulesRange2 = session.prepare("select * from \"" + Schema.FAMILY_SMPP_SMS_ROUTING_RULE
				+ "\"  LIMIT " + row_count + ";");
		deleteLiveSms = session.prepare("delete from \"" + Schema.FAMILY_LIVE_SMS + "\" where \"" + Schema.COLUMN_ID
				+ "\"=?;");

		s1 = getFillUpdateFields();
		StringBuilder sb = new StringBuilder();
		sb.append("\"");
		sb.append(Schema.COLUMN_IN_SYSTEM);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_DEST_CLUSTER_NAME);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_DEST_ESME_NAME);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_DEST_SYSTEM_ID);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_DELIVERY_DATE);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_IMSI);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_NNN_DIGITS);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_NNN_AN);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_NNN_NP);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_SM_STATUS);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_SM_TYPE);
		sb.append("\"");
		String s11 = sb.toString();

		s2 = getFillUpdateFields2();
		String s22 = ", ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";
		doArchiveDeliveredSms = session.prepare("INSERT INTO \"" + Schema.FAMILY_ARCHIVE + "\" (" + s1 + ", " + s11
				+ ") VALUES (? " + s2 + s22 + ");");

		this.started = true;
	}

	public void stop() throws Exception {
		if (!this.started)
			return;

//        cluster.shutdown();
        cluster.close();
		Metadata metadata = cluster.getMetadata();
		logger.info(String.format("Disconnected from cluster: %s\n", metadata.getClusterName()));

		this.started = false;
	}

	public boolean checkSmsSetExists(final TargetAddress ta) throws PersistenceException {

		try {
			BoundStatement boundStatement = new BoundStatement(smsSetExist);
			boundStatement.bind(ta.getTargetId());
			ResultSet results = session.execute(boundStatement);

			Row row = results.one();

			long count = row.getLong(0);

			return (count > 0);
		} catch (Exception e) {
			String msg = "Failed to checkSmsSetExists SMS for '" + ta.getAddr() + ",Ton=" + ta.getAddrTon() + ",Npi="
					+ ta.getAddrNpi() + "'!";
			throw new PersistenceException(msg, e);
		}
	}

	public SmsSet obtainSmsSet(final TargetAddress ta) throws PersistenceException {

		TargetAddress lock = SmsSetCache.getInstance().addSmsSet(ta);
		try {
			synchronized (lock) {
				try {
					BoundStatement boundStatement = new BoundStatement(obtainSmsSet);
					boundStatement.bind(ta.getTargetId());
					ResultSet res = session.execute(boundStatement);

					Row row = res.one();
					SmsSet smsSet = createSmsSet(row);

					if (smsSet.getDestAddr() == null) {
						smsSet = new SmsSet();

						smsSet.setDestAddr(ta.getAddr());
						smsSet.setDestAddrTon(ta.getAddrTon());
						smsSet.setDestAddrNpi(ta.getAddrNpi());
						smsSet.setInSystem(0);

						boundStatement = new BoundStatement(obtainSmsSet2);
						boundStatement.bind(ta.getTargetId(), ta.getAddr(), ta.getAddrTon(), ta.getAddrNpi(), 0);
						session.execute(boundStatement);
					}

					return smsSet;
				} catch (Exception e) {
					String msg = "Failed to obtainSmsSet SMS for '" + ta.getAddr() + ",Ton=" + ta.getAddrTon()
							+ ",Npi=" + ta.getAddrNpi() + "'!";
					throw new PersistenceException(msg, e);
				}
			}
		} finally {
			SmsSetCache.getInstance().removeSmsSet(lock);
		}
	}

	public void setNewMessageScheduled(final SmsSet smsSet, final Date newDueDate) throws PersistenceException {

		if (smsSet.getInSystem() == 2)
			// we do not update Scheduled if it is a new message insertion and
			// target is under delivering process
			return;

		if (smsSet.getInSystem() == 1 && smsSet.getDueDate() != null && newDueDate.after(smsSet.getDueDate()))
			// we do not update Scheduled if it is already schedulered for a
			// earlier time
			return;

		try {

			BoundStatement boundStatement = new BoundStatement(setNewMessageScheduled);
			boundStatement.bind(smsSet.getTargetId(), newDueDate, 1, 0);
			session.execute(boundStatement);

			smsSet.setInSystem(1);
			smsSet.setDueDate(newDueDate);
			smsSet.setDueDelay(0);
		} catch (Exception e) {
			String msg = "Failed to setScheduled for '" + smsSet.getDestAddr() + ",Ton=" + smsSet.getDestAddrTon()
					+ ",Npi=" + smsSet.getDestAddrNpi() + "'!";
			throw new PersistenceException(msg, e);
		}
	}

	public void setDeliveringProcessScheduled(final SmsSet smsSet, Date newDueDate, final int newDueDelay)
			throws PersistenceException {

		try {

			BoundStatement boundStatement = new BoundStatement(setDeliveringProcessScheduled);
			boundStatement.bind(smsSet.getTargetId(), newDueDate, 1, newDueDelay);
			session.execute(boundStatement);

			smsSet.setInSystem(1);
			smsSet.setDueDate(newDueDate);
			smsSet.setDueDelay(newDueDelay);
		} catch (Exception e) {
			String msg = "Failed to setScheduled for '" + smsSet.getDestAddr() + ",Ton=" + smsSet.getDestAddrTon()
					+ ",Npi=" + smsSet.getDestAddrNpi() + "'!";
			throw new PersistenceException(msg, e);
		}
	}

	public void setDestination(SmsSet smsSet, String destClusterName, String destSystemId, String destEsmeId,
			SmType type) {

		smsSet.setDestClusterName(destClusterName);
		smsSet.setDestSystemId(destSystemId);
		smsSet.setDestEsmeName(destEsmeId);
		smsSet.setType(type);
	}

	public void setRoutingInfo(SmsSet smsSet, IMSI imsi, LocationInfoWithLMSI locationInfoWithLMSI) {

		smsSet.setImsi(imsi.getData());
		smsSet.setLocationInfoWithLMSI(locationInfoWithLMSI);
	}

	public void setDeliveryStart(final SmsSet smsSet, final Date newInSystemDate) throws PersistenceException {

		try {
			BoundStatement boundStatement = new BoundStatement(setDeliveryStart);
			boundStatement.bind(smsSet.getTargetId(), 2, newInSystemDate);
			session.execute(boundStatement);

			smsSet.setInSystem(2);
			smsSet.setInSystemDate(newInSystemDate);
		} catch (Exception e) {
			String msg = "Failed to setDeliveryStart smsSet for '" + smsSet.getDestAddr() + ",Ton="
					+ smsSet.getDestAddrTon() + ",Npi=" + smsSet.getDestAddrNpi() + "'!";
			throw new PersistenceException(msg, e);
		}
	}

	public void setDeliveryStart(final Sms sms) throws PersistenceException {

		try {
			BoundStatement boundStatement = new BoundStatement(setDeliveryStart2);
			boundStatement.bind(sms.getDbId(), sms.getDeliveryCount() + 1);
			session.execute(boundStatement);

			sms.setDeliveryCount(sms.getDeliveryCount() + 1);
		} catch (Exception e) {
			String msg = "Failed to setDeliveryStart sms for '" + sms.getDbId() + "'!";
			throw new PersistenceException(msg, e);
		}
	}

	public void setDeliverySuccess(final SmsSet smsSet, final Date lastDelivery) throws PersistenceException {
		try {
			BoundStatement boundStatement = new BoundStatement(setDeliverySuccess);
			boundStatement.bind(smsSet.getTargetId(), 0, 0);
			session.execute(boundStatement);

			smsSet.setInSystem(0);
			smsSet.setStatus(ErrorCode.SUCCESS);
			smsSet.setLastDelivery(lastDelivery);
		} catch (Exception e) {
			String msg = "Failed to setDeliverySuccess for '" + smsSet.getDestAddr() + ",Ton="
					+ smsSet.getDestAddrTon() + ",Npi=" + smsSet.getDestAddrNpi() + "'!";
			throw new PersistenceException(msg, e);
		}
	}

	public void setDeliveryFailure(final SmsSet smsSet, final ErrorCode smStatus, final Date lastDelivery)
			throws PersistenceException {

		try {
			BoundStatement boundStatement = new BoundStatement(setDeliveryFailure);
			boundStatement.bind(smsSet.getTargetId(), 0, smStatus.getCode(), false);
			session.execute(boundStatement);

			smsSet.setInSystem(0);
			smsSet.setStatus(smStatus);
			smsSet.setLastDelivery(lastDelivery);
		} catch (Exception e) {
			String msg = "Failed to setDeliverySuccess for '" + smsSet.getDestAddr() + ",Ton="
					+ smsSet.getDestAddrTon() + ",Npi=" + smsSet.getDestAddrNpi() + "'!";
			throw new PersistenceException(msg, e);
		}
	}

	public void setAlertingSupported(final String targetId, final boolean alertingSupported)
			throws PersistenceException {

		try {
			BoundStatement boundStatement = new BoundStatement(setAlertingSupported);
			boundStatement.bind(targetId, alertingSupported);
			session.execute(boundStatement);
		} catch (Exception e) {
			String msg = "Failed to setAlertingSupported for '" + targetId + "'!";
			throw new PersistenceException(msg, e);
		}
	}

	public boolean deleteSmsSet(final SmsSet smsSet) throws PersistenceException {
		TargetAddress lock = SmsSetCache.getInstance().addSmsSet(new TargetAddress(smsSet));
		try {
			synchronized (lock) {

				// firstly we are looking for corresponded records in LIVE_SMS
				// table
				smsSet.clearSmsList();
				fetchSchedulableSms(smsSet, false);
				if (smsSet.getSmsCount() > 0) {
					// there are corresponded records in LIVE_SMS table - we
					// will not delete LIVE record
					return false;
				}

				try {
					BoundStatement boundStatement = new BoundStatement(deleteSmsSet);
					boundStatement.bind(smsSet.getTargetId());
					session.execute(boundStatement);
				} catch (Exception e) {
					String msg = "Failed to deleteSmsSet for '" + smsSet.getDestAddr() + ",Ton="
							+ smsSet.getDestAddrTon() + ",Npi=" + smsSet.getDestAddrNpi() + "'!";
					throw new PersistenceException(msg, e);
				}

				return true;
			}
		} finally {
			SmsSetCache.getInstance().removeSmsSet(lock);
		}
	}

	public void createLiveSms(final Sms sms) throws PersistenceException {
		try {
			BoundStatement boundStatement = new BoundStatement(createLiveSms);

			boundStatement.setString(Schema.COLUMN_TARGET_ID, sms.getSmsSet().getTargetId());
			this.FillUpdateFields(sms, boundStatement, Schema.FAMILY_LIVE_SMS);

			session.execute(boundStatement);
		} catch (Exception e) {
			String msg = "Failed to createLiveSms SMS for '" + sms.getDbId() + "'!";

			throw new PersistenceException(msg, e);
		}
	}

	public Sms obtainLiveSms(final UUID dbId) throws PersistenceException {
		try {
			BoundStatement boundStatement = new BoundStatement(obtainLiveSms);
			boundStatement.bind(dbId);
			ResultSet res = session.execute(boundStatement);

			Row row = res.one();
			try {
				return createSms(row, null, dbId);
			} catch (Exception e) {
				throw new PersistenceException("Failed to deserialize SMS at key '" + dbId + "'!", e);
			}
		} catch (Exception e) {
			String msg = "Failed to obtainLiveSms SMS for '" + dbId + "'!";

			throw new PersistenceException(msg, e);
		}
	}

	public Sms obtainLiveSms(final long messageId) throws PersistenceException {

		try {
			BoundStatement boundStatement = new BoundStatement(obtainLiveSms2);
			boundStatement.bind(messageId);
			ResultSet res = session.execute(boundStatement);

			Row row = res.one();
			try {
				UUID key = row.getUUID(Schema.COLUMN_ID);
				return createSms(row, null, key);
			} catch (Exception e) {
				throw new PersistenceException("Failed to deserialize SMS at key '" + messageId + "'!", e);
			}
		} catch (Exception e) {
			String msg = "Failed to obtainLiveSms SMS for '" + messageId + "'!";

			throw new PersistenceException(msg, e);
		}
	}

	public void updateLiveSms(Sms sms) throws PersistenceException {
		// TODO: implement it
		// .....................................
	}

	public void archiveDeliveredSms(final Sms sms, Date deliveryDate) throws PersistenceException {
		deleteLiveSms(sms);
		sms.setDeliveryDate(deliveryDate);
		sms.getSmsSet().setStatus(ErrorCode.SUCCESS);
		doArchiveDeliveredSms(sms);
	}

	public void archiveFailuredSms(final Sms sms) throws PersistenceException {
		deleteLiveSms(sms);
		sms.setDeliveryDate(sms.getSmsSet().getLastDelivery());
		doArchiveDeliveredSms(sms);
	}

	public List<SmsSet> fetchSchedulableSmsSets(final int maxRecordCount, Tracer tracer) throws PersistenceException {
		try {
			List<SmsSet> lst = new ArrayList<SmsSet>();

			doFetchSchedulableSmsSets(maxRecordCount, lst, 1);
			if (tracer != null) {
				for (SmsSet smsSet : lst) {
					tracer.severe("SmsSet was scheduled with inSystem==2 and InSystemDate+30min > Now: " + smsSet);
				}
			}

			doFetchSchedulableSmsSets(maxRecordCount - lst.size(), lst, 2);

			return lst;
		} catch (Exception e) {
			String msg = "Failed to fetchSchedulableSmsSets!";

			throw new PersistenceException(msg, e);
		}
	}

	private void doFetchSchedulableSmsSets(final int maxRecordCount, List<SmsSet> lst, int opt)
			throws PersistenceException {

		if (maxRecordCount <= 0)
			return;

		PreparedStatement ps;
		Date date;
		int inSyst;
		// doFetchSchedulableSmsSets = session.prepare("select * from \"" +
		// Schema.FAMILY_LIVE + "\" where \"" + Schema.COLUMN_IN_SYSTEM +
		// "\"=? and \""
		// + Schema.COLUMN_IN_SYSTEM_DATE + "\"<=?  LIMIT " + maxRecordCount +
		// "  ALLOW FILTERING;");
		// doFetchSchedulableSmsSets2 = session.prepare("select * from \"" +
		// Schema.FAMILY_LIVE + "\" where \"" + Schema.COLUMN_IN_SYSTEM +
		// "\"=? and \""
		// + Schema.COLUMN_DUE_DATE + "\"<=?  LIMIT " + maxRecordCount +
		// "  ALLOW FILTERING;");
		if (opt == 1) {
			ps = session.prepare("select * from \"" + Schema.FAMILY_LIVE + "\" where \"" + Schema.COLUMN_IN_SYSTEM
					+ "\"=? and \"" + Schema.COLUMN_IN_SYSTEM_DATE + "\"<=?  LIMIT " + maxRecordCount
					+ "  ALLOW FILTERING;");
			inSyst = 2;
			date = new Date((new Date()).getTime() - 30 * 60 * 1000);
		} else {
			inSyst = 1;
			date = new Date();
			String s1 = "select * from \"" + Schema.FAMILY_LIVE + "\" where \"" + Schema.COLUMN_IN_SYSTEM
					+ "\"=? and \"" + Schema.COLUMN_DUE_DATE + "\"<=?  LIMIT " + maxRecordCount + "  ALLOW FILTERING;";

			// ps = session.prepare("select * from \"" + Schema.FAMILY_LIVE +
			// "\" where \"" + Schema.COLUMN_IN_SYSTEM + "\"=? and \"" +
			// Schema.COLUMN_DUE_DATE
			// + "\"<=?  ALLOW FILTERING;");
			// String s1 = "select * from \"" + Schema.FAMILY_LIVE +
			// "\" where \"" + Schema.COLUMN_IN_SYSTEM + "\"=" + inSyst +
			// " and \"" + Schema.COLUMN_DUE_DATE
			// + "\"<='" + date + "'  LIMIT " + maxRecordCount +
			// "  ALLOW FILTERING;";
			ps = session.prepare(s1);
		}
		BoundStatement boundStatement = new BoundStatement(ps);
		boundStatement.bind(inSyst, date);
		ResultSet res = session.execute(boundStatement);

		for (Row row : res) {
			String s = "???";
			try {
				s = row.getString(Schema.COLUMN_TARGET_ID);
				SmsSet smsSet = createSmsSet(row);
				lst.add(smsSet);
			} catch (Exception e) {
				throw new PersistenceException("Failed to deserialize SMS at key '" + s + "!", e);
			}
		}
	}

	public void fetchSchedulableSms(final SmsSet smsSet, boolean excludeNonScheduleDeliveryTime)
			throws PersistenceException {

		try {
			BoundStatement boundStatement = new BoundStatement(fetchSchedulableSms);
			boundStatement.bind(smsSet.getTargetId());
			ResultSet res = session.execute(boundStatement);
			smsSet.clearSmsList();
			Date curDate = new Date();
			for (Row row : res) {
				try {
					UUID key = row.getUUID(Schema.COLUMN_ID);
					Sms sms = createSms(row, smsSet, key);
					if (excludeNonScheduleDeliveryTime && sms.getScheduleDeliveryTime() != null
							&& sms.getScheduleDeliveryTime().after(curDate))
						continue;
					// if (excludeReceiptMessages && (sms.getEsmClass() &
					// MessageUtil.ESME_DELIVERY_ACK) != 0)
					// continue;
					smsSet.addSms(sms);
				} catch (Exception e) {
					String msg = "Failed to deserialize SMS at key '" + row.getUUID(0) + "'!";

					throw new PersistenceException(msg, e);
				}
			}
			smsSet.resortSms();
		} catch (Exception e) {
			String msg = "Failed to fetchSchedulableSms SMS for '" + smsSet.getTargetId() + "'!";

			throw new PersistenceException(msg, e);
		}
	}

	private String getFillUpdateFields() {
		StringBuilder sb = new StringBuilder();

		sb.append("\"");
		sb.append(Schema.COLUMN_ID);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_ADDR_DST_DIGITS);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_ADDR_DST_TON);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_ADDR_DST_NPI);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_ADDR_SRC_DIGITS);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_ADDR_SRC_TON);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_ADDR_SRC_NPI);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_MESSAGE_ID);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_MO_MESSAGE_REF);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_ORIG_ESME_NAME);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_ORIG_SYSTEM_ID);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_SUBMIT_DATE);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_SERVICE_TYPE);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_ESM_CLASS);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_PROTOCOL_ID);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_PRIORITY);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_REGISTERED_DELIVERY);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_REPLACE);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_DATA_CODING);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_DEFAULT_MSG_ID);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_MESSAGE);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_SCHEDULE_DELIVERY_TIME);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_VALIDITY_PERIOD);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_DELIVERY_COUNT);
		sb.append("\", \"");
		sb.append(Schema.COLUMN_OPTIONAL_PARAMETERS);
		sb.append("\"");

		return sb.toString();
	}

	private String getFillUpdateFields2() {
		int cnt = 25;
		StringBuilder sb = new StringBuilder();
		for (int i1 = 0; i1 < cnt; i1++) {
			sb.append(", ?");
		}
		return sb.toString();
	}

	private void FillUpdateFields(Sms sms, BoundStatement boundStatement, String columnFamilyName)
			throws PersistenceException {

		boundStatement.setUUID(Schema.COLUMN_ID, sms.getDbId());

		boundStatement.setString(Schema.COLUMN_ADDR_DST_DIGITS, sms.getSmsSet().getDestAddr());
		boundStatement.setInt(Schema.COLUMN_ADDR_DST_TON, sms.getSmsSet().getDestAddrTon());
		boundStatement.setInt(Schema.COLUMN_ADDR_DST_NPI, sms.getSmsSet().getDestAddrNpi());

		if (sms.getSourceAddr() != null) {
			boundStatement.setString(Schema.COLUMN_ADDR_SRC_DIGITS, sms.getSourceAddr());
		}
		boundStatement.setInt(Schema.COLUMN_ADDR_SRC_TON, sms.getSourceAddrTon());
		boundStatement.setInt(Schema.COLUMN_ADDR_SRC_NPI, sms.getSourceAddrNpi());

		boundStatement.setLong(Schema.COLUMN_MESSAGE_ID, sms.getMessageId());
		boundStatement.setInt(Schema.COLUMN_MO_MESSAGE_REF, sms.getMoMessageRef());

		if (sms.getOrigEsmeName() != null) {
			boundStatement.setString(Schema.COLUMN_ORIG_ESME_NAME, sms.getOrigEsmeName());
		}
		if (sms.getOrigSystemId() != null) {
			boundStatement.setString(Schema.COLUMN_ORIG_SYSTEM_ID, sms.getOrigSystemId());
		}
		if (sms.getSubmitDate() != null) {
            DBOperations_C2.setBoundStatementDate(boundStatement, Schema.COLUMN_SUBMIT_DATE, sms.getSubmitDate());
		}
		if (sms.getServiceType() != null) {
			boundStatement.setString(Schema.COLUMN_SERVICE_TYPE, sms.getServiceType());
		}
		boundStatement.setInt(Schema.COLUMN_ESM_CLASS, sms.getEsmClass());
		boundStatement.setInt(Schema.COLUMN_PROTOCOL_ID, sms.getProtocolId());
		boundStatement.setInt(Schema.COLUMN_PRIORITY, sms.getPriority());

		boundStatement.setInt(Schema.COLUMN_REGISTERED_DELIVERY, sms.getRegisteredDelivery());
		boundStatement.setInt(Schema.COLUMN_REPLACE, sms.getReplaceIfPresent());
		boundStatement.setInt(Schema.COLUMN_DATA_CODING, sms.getDataCoding());
		boundStatement.setInt(Schema.COLUMN_DEFAULT_MSG_ID, sms.getDefaultMsgId());

		if (sms.getShortMessage() != null) {
			boundStatement.setBytes(Schema.COLUMN_MESSAGE, ByteBuffer.wrap(sms.getShortMessage()));
		}
		if (sms.getScheduleDeliveryTime() != null) {
            DBOperations_C2.setBoundStatementDate(boundStatement, Schema.COLUMN_SCHEDULE_DELIVERY_TIME,
                    sms.getScheduleDeliveryTime());
		}
		if (sms.getValidityPeriod() != null) {
		    DBOperations_C2.setBoundStatementDate(boundStatement, Schema.COLUMN_VALIDITY_PERIOD, sms.getValidityPeriod());
		}

		boundStatement.setInt(Schema.COLUMN_DELIVERY_COUNT, sms.getDeliveryCount());

		if (sms.getTlvSet().getOptionalParameterCount() > 0) {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				XMLObjectWriter writer = XMLObjectWriter.newInstance(baos);
				writer.setIndentation("\t");
				writer.write(sms.getTlvSet(), TLV_SET, TlvSet.class);
				writer.close();
				byte[] rawData = baos.toByteArray();
				String serializedEvent = new String(rawData);

				boundStatement.setString(Schema.COLUMN_OPTIONAL_PARAMETERS, serializedEvent);
			} catch (XMLStreamException e) {
				String msg = "XMLStreamException when serializing optional parameters for '" + sms.getDbId() + "'!";

				throw new PersistenceException(msg, e);
			}
		}

	}

	protected Sms createSms(final Row row, SmsSet smsSet, UUID dbId) throws PersistenceException {

		if (row == null)
			return null;

		Sms sms = new Sms();
		sms.setDbId(row.getUUID(Schema.COLUMN_ID));
		String destAddr = null;
		int destAddrTon = -1;
		int destAddrNpi = -1;

		destAddr = row.getString(Schema.COLUMN_ADDR_DST_DIGITS);
		destAddrTon = row.getInt(Schema.COLUMN_ADDR_DST_TON);
		destAddrNpi = row.getInt(Schema.COLUMN_ADDR_DST_NPI);

		sms.setMessageId(row.getLong(Schema.COLUMN_MESSAGE_ID));
		sms.setMoMessageRef(row.getInt(Schema.COLUMN_MO_MESSAGE_REF));
		sms.setOrigEsmeName(row.getString(Schema.COLUMN_ORIG_ESME_NAME));
		sms.setOrigSystemId(row.getString(Schema.COLUMN_ORIG_SYSTEM_ID));
        sms.setSubmitDate(DBOperations_C2.getRowDate(row, Schema.COLUMN_SUBMIT_DATE));

		sms.setSourceAddr(row.getString(Schema.COLUMN_ADDR_SRC_DIGITS));
		sms.setSourceAddrTon(row.getInt(Schema.COLUMN_ADDR_SRC_TON));
		sms.setSourceAddrNpi(row.getInt(Schema.COLUMN_ADDR_SRC_NPI));

		sms.setServiceType(row.getString(Schema.COLUMN_SERVICE_TYPE));
		sms.setEsmClass(row.getInt(Schema.COLUMN_ESM_CLASS));
		sms.setProtocolId(row.getInt(Schema.COLUMN_PROTOCOL_ID));
		sms.setPriority(row.getInt(Schema.COLUMN_PRIORITY));
		sms.setRegisteredDelivery(row.getInt(Schema.COLUMN_REGISTERED_DELIVERY));
		sms.setReplaceIfPresent(row.getInt(Schema.COLUMN_REPLACE));
		sms.setDataCoding(row.getInt(Schema.COLUMN_DATA_CODING));
		sms.setDefaultMsgId(row.getInt(Schema.COLUMN_DEFAULT_MSG_ID));

		ByteBuffer bb = row.getBytes(Schema.COLUMN_MESSAGE);
		byte[] buf = new byte[bb.limit() - bb.position()];
		bb.get(buf);
		sms.setShortMessage(buf);
		sms.setScheduleDeliveryTime(DBOperations_C2.getRowDate(row, Schema.COLUMN_SCHEDULE_DELIVERY_TIME));
		sms.setValidityPeriod(DBOperations_C2.getRowDate(row, Schema.COLUMN_VALIDITY_PERIOD));
		sms.setDeliveryCount(row.getInt(Schema.COLUMN_DELIVERY_COUNT));

		String s = row.getString(Schema.COLUMN_OPTIONAL_PARAMETERS);
		if (s != null) {
			try {
				ByteArrayInputStream bais = new ByteArrayInputStream(s.getBytes());
				XMLObjectReader reader = XMLObjectReader.newInstance(bais);
				TlvSet copy = reader.read(TLV_SET, TlvSet.class);
				sms.getTlvSet().clearAllOptionalParameter();
				sms.getTlvSet().addAllOptionalParameter(copy.getOptionalParameters());
			} catch (XMLStreamException e) {
				String msg = "XMLStreamException when deserializing optional parameters for '" + sms.getDbId() + "'!";

				throw new PersistenceException(msg, e);
			}
		}

		if (destAddr == null || destAddrTon == -1 || destAddrNpi == -1) {
			throw new PersistenceException("destAddr or destAddrTon or destAddrNpi is absent in LIVE_SMS for ID='"
					+ dbId + "'");
		}

		if (smsSet == null) {
			TargetAddress ta = new TargetAddress(destAddrTon, destAddrNpi, destAddr, 0);
			smsSet = obtainSmsSet(ta);
		} else {
			if (smsSet.getDestAddr() == null) {
				smsSet.setDestAddr(destAddr);
				smsSet.setDestAddrTon(destAddrTon);
				smsSet.setDestAddrNpi(destAddrNpi);

				// TODO: here we can add fields that are present only in ARCHIVE
				// table (into extra fields of Sms class)
				// "DEST_CLUSTER_NAME" text,
				// "DEST_ESME_ID" text,
				// "DEST_SYSTEM_ID" text,
				// "DELIVERY_DATE" timestamp,
				// "IMSI" ascii,
				// "NNN_DIGITS" ascii,
				// "NNN_AN" int,
				// "NNN_NP" int,
				// "SM_STATUS" int,
				// "SM_TYPE" int,
				// "DELIVERY_COUNT" int,
			}
		}
		sms.setSmsSet(smsSet);

		return sms;
	}

	private static SmsSet createSmsSet(Row row) {
		SmsSet smsSet = new SmsSet();

		if (row != null) {
			smsSet.setDestAddr(row.getString(Schema.COLUMN_ADDR_DST_DIGITS));
			smsSet.setDestAddrTon(row.getInt(Schema.COLUMN_ADDR_DST_TON));
			smsSet.setDestAddrNpi(row.getInt(Schema.COLUMN_ADDR_DST_NPI));

			smsSet.setInSystem(row.getInt(Schema.COLUMN_IN_SYSTEM));
			smsSet.setInSystemDate(DBOperations_C2.getRowDate(row, Schema.COLUMN_IN_SYSTEM_DATE));
			smsSet.setDueDate(DBOperations_C2.getRowDate(row, Schema.COLUMN_DUE_DATE));

			if (!row.isNull(Schema.COLUMN_SM_STATUS))
				smsSet.setStatus(ErrorCode.fromInt(row.getInt(Schema.COLUMN_SM_STATUS)));
			smsSet.setDueDelay(row.getInt(Schema.COLUMN_DUE_DELAY));
			smsSet.setAlertingSupported(row.getBool(Schema.COLUMN_ALERTING_SUPPORTED));
		}
		return smsSet;
	}

	public DbSmsRoutingRule getSmsRoutingRule(final String address, int networkId) throws PersistenceException {

		try {
			BoundStatement boundStatement = new BoundStatement(getSmsRoutingRule);
			boundStatement.bind(address);
			ResultSet result = session.execute(boundStatement);

			Row row = result.one();
			if (row == null) {
				return null;
			} else {
				String name = row.getString(Schema.COLUMN_CLUSTER_NAME);
				DbSmsRoutingRule res = new DbSmsRoutingRule(SmsRoutingRuleType.SMPP, address, networkId, name);
				return res;
			}
		} catch (Exception e) {
			String msg = "Failed to getSmsRoutingRule DbSmsRoutingRule for id='" + address + "'!";

			throw new PersistenceException(msg, e);
		}
	}

	public void updateDbSmsRoutingRule(DbSmsRoutingRule dbSmsRoutingRule) throws PersistenceException {
		try {
			BoundStatement boundStatement = new BoundStatement(updateDbSmsRoutingRule);
			boundStatement.bind(dbSmsRoutingRule.getAddress(), dbSmsRoutingRule.getClusterName());
			session.execute(boundStatement);
		} catch (Exception e) {
			String msg = "Failed to addDbSmsRoutingRule for '" + dbSmsRoutingRule.getAddress() + "'!";

			throw new PersistenceException(msg, e);
		}
	}

	public void deleteDbSmsRoutingRule(final String address) throws PersistenceException {
		try {
			BoundStatement boundStatement = new BoundStatement(deleteDbSmsRoutingRule);
			boundStatement.bind(address);
			session.execute(boundStatement);
		} catch (Exception e) {
			String msg = "Failed to deleteDbSmsRoutingRule for '" + address + "'!";
			throw new PersistenceException(msg, e);
		}
	}

	public List<DbSmsRoutingRule> getSmsRoutingRulesRange() throws PersistenceException {
		return getSmsRoutingRulesRange(null);
	}

	public List<DbSmsRoutingRule> getSmsRoutingRulesRange(String lastAdress) throws PersistenceException {

		List<DbSmsRoutingRule> ress = new FastList<DbSmsRoutingRule>();
		try {
			PreparedStatement ps = lastAdress != null ? getSmsRoutingRulesRange : getSmsRoutingRulesRange2;
			BoundStatement boundStatement = new BoundStatement(ps);
			if (lastAdress != null) {
				boundStatement.bind(lastAdress);
			}
			ResultSet result = session.execute(boundStatement);

			int i1 = 0;
			for (Row row : result) {
				String name = row.getString(Schema.COLUMN_CLUSTER_NAME);
				String address = row.getString(Schema.COLUMN_ADDRESS);
				int networkId = row.getInt(Schema.COLUMN_NETWORK_ID);
				DbSmsRoutingRule res = new DbSmsRoutingRule(SmsRoutingRuleType.SMPP, address, networkId, name);

				if (i1 == 0) {
					i1 = 1;
					if (lastAdress == null)
						ress.add(res);
				} else {
					ress.add(res);
				}
			}

			return ress;
		} catch (Exception e) {
			String msg = "Failed to getSmsRoutingRule DbSmsRoutingRule for all records: " + e;

			throw new PersistenceException(msg, e);
		}
	}

	protected void deleteLiveSms(final Sms sms) throws PersistenceException {

		try {
			BoundStatement boundStatement = new BoundStatement(deleteLiveSms);
			boundStatement.bind(sms.getDbId());
			session.execute(boundStatement);
		} catch (Exception e) {
			String msg = "Failed to deleteLiveSms for '" + sms.getDbId() + "'!";

			throw new PersistenceException(msg, e);
		}
	}

	private void doArchiveDeliveredSms(final Sms sms) throws PersistenceException {
		try {
			BoundStatement boundStatement = new BoundStatement(doArchiveDeliveredSms);

			this.FillUpdateFields(sms, boundStatement, Schema.FAMILY_LIVE_SMS);

			boundStatement.setInt(Schema.COLUMN_IN_SYSTEM, 0);
			if (sms.getSmsSet().getDestClusterName() != null) {
				boundStatement.setString(Schema.COLUMN_DEST_CLUSTER_NAME, sms.getSmsSet().getDestClusterName());
			}
			if (sms.getSmsSet().getDestEsmeName() != null) {
				boundStatement.setString(Schema.COLUMN_DEST_ESME_NAME, sms.getSmsSet().getDestEsmeName());
			}
			if (sms.getSmsSet().getDestSystemId() != null) {
				boundStatement.setString(Schema.COLUMN_DEST_SYSTEM_ID, sms.getSmsSet().getDestSystemId());
			}
			if (sms.getDeliverDate() != null) {
                DBOperations_C2.setBoundStatementDate(boundStatement, Schema.COLUMN_DELIVERY_DATE, sms.getDeliverDate());
			}
			if (sms.getSmsSet().getImsi() != null) {
				boundStatement.setString(Schema.COLUMN_IMSI, sms.getSmsSet().getImsi());
			}
			if (sms.getSmsSet().getLocationInfoWithLMSI() != null) {
				boundStatement.setString(Schema.COLUMN_NNN_DIGITS, sms.getSmsSet().getLocationInfoWithLMSI()
						.getNetworkNodeNumber().getAddress());
				boundStatement.setInt(Schema.COLUMN_NNN_AN, sms.getSmsSet().getLocationInfoWithLMSI()
						.getNetworkNodeNumber().getAddressNature().getIndicator());
				boundStatement.setInt(Schema.COLUMN_NNN_NP, sms.getSmsSet().getLocationInfoWithLMSI()
						.getNetworkNodeNumber().getNumberingPlan().getIndicator());
			}
			if (sms.getSmsSet().getStatus() != null) {
				boundStatement.setInt(Schema.COLUMN_SM_STATUS, sms.getSmsSet().getStatus().getCode());
			}
			if (sms.getSmsSet().getType() != null) {
				boundStatement.setInt(Schema.COLUMN_SM_TYPE, sms.getSmsSet().getType().getCode());
			}

			session.execute(boundStatement);
		} catch (Exception e) {
			String msg = "Failed to archiveDeliveredSms SMS for '" + sms.getDbId() + "'!";

			throw new PersistenceException(msg, e);
		}
	}

}
