/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * TeleStax and individual contributors
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

package org.mobicents.smsc.tools.cassandratool;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.log4j.BasicConfigurator;
import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.Schema;
import org.mobicents.smsc.library.SmsSet;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JButton;

import java.awt.GridLayout;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

import java.awt.Color;

import javax.swing.ListSelectionModel;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class CassandraToolForm {

    private JTextField tbHost;
    private JTextField tbPort;
    private JTextField tbUser;
    private JTextField tbPass;
    private JTextField tbKeyspace;
    private JTable tResult;
    protected JFrame frmSmppSimulator;
    private JButton btConnect;
    private JButton btDisconnect;
    private JButton btGetData;
    private JRadioButton rbLive;
    private JRadioButton rbArchive;
    private JRadioButton rbMsgId;
    private JRadioButton rbDlvMsgId;
    private DefaultTableModel model = new DefaultTableModel();

    private DBOperationsProxy dbOperations;
    private final ButtonGroup buttonGroup = new ButtonGroup();

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    CassandraToolForm window = new CassandraToolForm();
                    window.frmSmppSimulator.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void setupLog4j() {
        BasicConfigurator.configure();
    }

    public CassandraToolForm() {
        initialize();

        setupLog4j();
    }

    public JFrame getJFrame() {
        return this.frmSmppSimulator;
    }

    private void initialize() {
        frmSmppSimulator = new JFrame();
        frmSmppSimulator.setResizable(true);
        frmSmppSimulator.setTitle("Cassandra Tool");
        frmSmppSimulator.setBounds(100, 100, 895, 728);
        frmSmppSimulator.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        frmSmppSimulator.getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new GridLayout(0, 1, 0, 0));
        
        JPanel panel_1 = new JPanel();
        panel.add(panel_1);
        panel_1.setLayout(new GridLayout(1, 0, 0, 0));
        
        JScrollPane scrollPane = new JScrollPane();
        panel_1.add(scrollPane);

        tResult = new JTable();
        tResult.setModel(new DefaultTableModel(new Object[][] {}, new String[] { "TargetId", "Id", "DueSLot", "MessageId",
                "InSystem", "DeliveryCount", "SmStatus", "ValidityPeriod", "SchedulerDeliveryTime", "Text", }) {
            Class[] columnTypes = new Class[] { String.class, String.class, String.class, String.class, String.class, String.class,
                    String.class, String.class, String.class, String.class };

            public Class getColumnClass(int columnIndex) {
                return columnTypes[columnIndex];
            }

            boolean[] columnEditables = new boolean[] { false, false, false, false, false, false, false, false, false, false };

            public boolean isCellEditable(int row, int column) {
                return columnEditables[column];
            }
        });
        tResult.getColumnModel().getColumn(0).setPreferredWidth(80);
        tResult.getColumnModel().getColumn(1).setPreferredWidth(20);
        tResult.getColumnModel().getColumn(2).setPreferredWidth(140);
        tResult.getColumnModel().getColumn(3).setPreferredWidth(20);
        tResult.getColumnModel().getColumn(4).setPreferredWidth(20);
        tResult.getColumnModel().getColumn(5).setPreferredWidth(20);
        tResult.getColumnModel().getColumn(6).setPreferredWidth(80);
        tResult.getColumnModel().getColumn(7).setPreferredWidth(80);
        tResult.getColumnModel().getColumn(8).setPreferredWidth(80);
        tResult.getColumnModel().getColumn(9).setPreferredWidth(120);

        tResult.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tResult.setFillsViewportHeight(true);
        tResult.setBorder(new LineBorder(new Color(0, 0, 0)));
        scrollPane.setViewportView(tResult);

        model = (DefaultTableModel) tResult.getModel();

        JPanel panel_2 = new JPanel();
        panel.add(panel_2);
        panel_2.setLayout(null);
        
        JLabel lblCassandraHost = new JLabel("Cassandra host");
        lblCassandraHost.setBounds(10, 15, 125, 14);
        panel_2.add(lblCassandraHost);
        
        JLabel lblCassandraPort = new JLabel("Cassandra port");
        lblCassandraPort.setBounds(10, 43, 125, 14);
        panel_2.add(lblCassandraPort);
        
        JLabel lblCassandraKeyspace = new JLabel("Cassandra keyspace");
        lblCassandraKeyspace.setBounds(10, 70, 125, 14);
        panel_2.add(lblCassandraKeyspace);
        
        tbKeyspace = new JTextField();
        tbKeyspace.setBounds(145, 67, 125, 20);
        panel_2.add(tbKeyspace);
        tbKeyspace.setText("RestCommSMSC");
        tbKeyspace.setColumns(10);
        
        tbPort = new JTextField();
        tbPort.setBounds(145, 40, 125, 20);

        panel_2.add(tbPort);
        tbPort.setText("9042");
        tbPort.setColumns(10);
        
        tbHost = new JTextField();
        tbHost.setBounds(145, 12, 125, 20);
        panel_2.add(tbHost);
        tbHost.setText("127.0.0.1");
        tbHost.setColumns(10);

        tbUser = new JTextField();
        tbPort.setBounds(145, 40, 125, 20);
        panel_2.add(tbUser);
        tbUser.setText("cassandra");
        tbUser.setColumns(10);

        tbPass = new JTextField();
        tbPort.setBounds(145, 40, 125, 20);
        panel_2.add(tbPass);
        tbPass.setText("cassandra");
        tbPass.setColumns(10);

        btConnect = new JButton("Connect");
        btConnect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                connect();
            }
        });
        btConnect.setBounds(10, 135, 105, 23);
        panel_2.add(btConnect);
        
        btDisconnect = new JButton("Disconnect");
        btDisconnect.setEnabled(false);
        btDisconnect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                disConnect();
            }
        });
        btDisconnect.setBounds(125, 135, 105, 23);
        panel_2.add(btDisconnect);
        
        btGetData = new JButton("Get data");
        btGetData.setEnabled(false);
        btGetData.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getData();
            }
        });
        btGetData.setBounds(240, 135, 105, 23);
        panel_2.add(btGetData);

        rbLive = new JRadioButton("Live table");
        rbLive.setSelected(true);
        buttonGroup.add(rbLive);
        rbLive.setBounds(6, 91, 109, 23);
        panel_2.add(rbLive);

        rbArchive = new JRadioButton("Archive table");
        buttonGroup.add(rbArchive);
        rbArchive.setBounds(117, 91, 109, 23);
        panel_2.add(rbArchive);

        rbMsgId = new JRadioButton("MsgId table");
        buttonGroup.add(rbMsgId);
        rbMsgId.setBounds(228, 91, 109, 23);
        panel_2.add(rbMsgId);

        rbDlvMsgId = new JRadioButton("DlvMsgId table");
        buttonGroup.add(rbDlvMsgId);
        rbDlvMsgId.setBounds(339, 91, 109, 23);
        panel_2.add(rbDlvMsgId);
    }

    private void connect() {
        this.dbOperations = new DBOperationsProxy();
        String hosts = this.tbHost.getText();
        String strPort = this.tbPort.getText();
        String user = this.tbUser.getText();
        String pass = this.tbPass.getText();
        int port = 0;
        try {
            port = Integer.parseInt(strPort);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(getJFrame(), "Can not parse port value:\n" + e.getMessage());
            return;
        }
        String keyspace = this.tbKeyspace.getText();
        try {
            this.dbOperations.start(hosts, port, keyspace, user, pass, 60, 60, 60 * 10, 1, 10000000000L);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(getJFrame(), "Can not connect to cassandra database:\n" + e.getMessage());
            return;
        }

        this.btConnect.setEnabled(false);
        this.btDisconnect.setEnabled(true);
        this.btGetData.setEnabled(true);

        JOptionPane.showMessageDialog(getJFrame(),
                "Connected:\nDriverVersion =  " + this.dbOperations.getCluster().getDriverVersion() + "\nProtocolVersion = "
                        + this.dbOperations.getProtocolVersion(this.dbOperations.getCluster()));
    }

    private void disConnect() {
        try {
            this.dbOperations.stop();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.dbOperations = null;

        this.btConnect.setEnabled(true);
        this.btDisconnect.setEnabled(false);
        this.btGetData.setEnabled(false);

        JOptionPane.showMessageDialog(getJFrame(), "Disconnected");
    }

    private void getData() {
        try {
            String tName = this.dbOperations.getTableName(new Date());
            String tbFam;
            int option = 0;
            if (this.rbLive.isSelected()) {
                tbFam = Schema.FAMILY_SLOT_MESSAGES_TABLE;
            } else if (this.rbArchive.isSelected()) {
                tbFam = Schema.FAMILY_MESSAGES;
            } else if (this.rbMsgId.isSelected()) {
                tbFam = Schema.FAMILY_MES_ID;
                option = 1;
            } else {
                tbFam = Schema.FAMILY_DLV_MES_ID;
                option = 2;
            }
            String s1 = "SELECT * FROM \"" + tbFam + tName + "\";";
            Session session = this.dbOperations.getSession();
            PreparedStatement ps = session.prepare(s1);
            BoundStatement boundStatement = new BoundStatement(ps);
            ResultSet res = session.execute(boundStatement);

            model.getDataVector().clear();
            model.rowsRemoved(new TableModelEvent(model));

            DateFormat df = new SimpleDateFormat("MM.dd HH:mm:ss");
            if (option == 0) {
                SortedMap<Long, ArrayList<SmsSet>> result = new TreeMap<Long, ArrayList<SmsSet>>();
                for (Row row : res) {
                    SmsSet smsSet = this.dbOperations.createSms(row, null);
                    if (smsSet != null) {
                        ArrayList<SmsSet> al = result.get(smsSet.getSms(0).getMessageId());
                        if (al == null) {
                            al = new ArrayList<SmsSet>();
                            result.put(smsSet.getSms(0).getMessageId(), al);
                        }
                        al.add(smsSet);
                    }
                }

                for (ArrayList<SmsSet> al : result.values()) {
                    for (SmsSet smsSet : al) {
                        ListSelectionModel l = tResult.getSelectionModel();

                        Vector newRow = new Vector();
                        newRow.add(smsSet.getTargetId());
                        newRow.add(smsSet.getSms(0).getDbId());
                        String reportDate = df.format(this.dbOperations.c2_getTimeForDueSlot(smsSet.getSms(0).getDueSlot()));
                        newRow.add(smsSet.getSms(0).getDueSlot() + " - " + reportDate);
                        newRow.add(smsSet.getSms(0).getMessageId());
                        newRow.add(smsSet.getInSystem());
                        newRow.add(smsSet.getSms(0).getDeliveryCount());
                        newRow.add(smsSet.getStatus());
                        Date dt = smsSet.getSms(0).getValidityPeriod();
                        if (dt != null)
                            reportDate = df.format(dt);
                        else
                            reportDate = "";
                        newRow.add(reportDate);
                        dt = smsSet.getSms(0).getScheduleDeliveryTime();
                        if (dt != null)
                            reportDate = df.format(dt);
                        else
                            reportDate = "";
                        newRow.add(reportDate);
                        newRow.add(smsSet.getSms(0).getShortMessageText());
                        model.getDataVector().add(0, newRow);
                        model.newRowsAdded(new TableModelEvent(model));
                    }
                }
            } else if (option == 1) {
                for (Row row : res) {
                    ListSelectionModel l = tResult.getSelectionModel();

                    Vector newRow = new Vector();
                    newRow.add(row.getString(Schema.COLUMN_ADDR_DST_DIGITS));
                    newRow.add(row.getUUID(Schema.COLUMN_ID));
                    newRow.add("");
                    newRow.add(row.getLong(Schema.COLUMN_MESSAGE_ID));
                    newRow.add("");
                    newRow.add("");
                    newRow.add("");
                    newRow.add("");
                    newRow.add("");
                    newRow.add("");
                    model.getDataVector().add(0, newRow);
                    model.newRowsAdded(new TableModelEvent(model));
                }
            } else {
                for (Row row : res) {
                    ListSelectionModel l = tResult.getSelectionModel();

                    Vector newRow = new Vector();
                    newRow.add(row.getString(Schema.COLUMN_REMOTE_MESSAGE_ID));
                    newRow.add("");
                    newRow.add(row.getString(Schema.COLUMN_DEST_ID));
                    newRow.add(row.getLong(Schema.COLUMN_MESSAGE_ID));
                    newRow.add("");
                    newRow.add("");
                    newRow.add("");
                    newRow.add("");
                    newRow.add("");
                    newRow.add("");
                    model.getDataVector().add(0, newRow);
                    model.newRowsAdded(new TableModelEvent(model));
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(getJFrame(), "Can not get data from cassandra database:\n" + e.getMessage());
        }
    }

    public class DBOperationsProxy extends DBOperations {

        protected Cluster getCluster() {
            return super.getCluster();
        }

        protected Session getSession() {
            return super.getSession();
        }

        protected String getTableName(Date dt) {
            return super.getTableName(dt);
        }

        protected SmsSet createSms(final Row row, SmsSet smsSet) throws PersistenceException {
            return super.createSms(row, smsSet, true, true, true, true, true, true);
        }
    }
}
