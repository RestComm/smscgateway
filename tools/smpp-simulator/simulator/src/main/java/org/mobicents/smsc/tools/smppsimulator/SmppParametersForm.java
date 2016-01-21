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

package org.mobicents.smsc.tools.smppsimulator;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JComboBox;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;

import javax.swing.JCheckBox;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmppParametersForm extends JDialog {

	private static final long serialVersionUID = -8945615083883278369L;

	private SmppSimulatorParameters data;
	private JTextField tbWindowSize;
	private JComboBox<SmppBindType> cbBindType;
    private JComboBox<SmppSession.Type> cbSmppSessionType;
	private JTextField tbHost;
	private JTextField tbPort;
	private JTextField tbConnectTimeout;
	private JTextField tbSystemId;
	private JTextField tbPassword;
	private JTextField tbRequestExpiryTimeout;
	private JTextField tbWindowMonitorInterval;
	private JCheckBox cbRejectIncomingDeliveryMessage;
	private JTextField tbAddressRange;

	public SmppParametersForm(JFrame owner) {
		super(owner, true);

		setTitle("SMPP general parameters");
		setResizable(false);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 620, 451);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(null);
		
		JLabel lblSmppWindowSize = new JLabel("<html>SMPP window size. The maximum number of requests \r\n<br>permitted to be outstanding (unacknowledged) at a given time\r\n</html>");
		lblSmppWindowSize.setBounds(10, 225, 401, 33);
		panel.add(lblSmppWindowSize);
		
		tbWindowSize = new JTextField();
		tbWindowSize.setBounds(424, 224, 86, 20);
		panel.add(tbWindowSize);
		tbWindowSize.setColumns(10);
		
		JButton btCancel = new JButton("Cancel");
		btCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doCancel();
			}
		});
		btCancel.setBounds(466, 382, 136, 23);
		panel.add(btCancel);
		
		JButton btOK = new JButton("OK");
		btOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doOK();
			}
		});
		btOK.setBounds(325, 382, 136, 23);
		panel.add(btOK);
		
		JLabel lblSmppBindType = new JLabel("SMPP bind type");
		lblSmppBindType.setBounds(10, 129, 401, 14);
		panel.add(lblSmppBindType);
		
		cbBindType = new JComboBox();
		cbBindType.setBounds(424, 126, 180, 20);
		panel.add(cbBindType);
		
		JLabel lblSmscHost = new JLabel("SMSC host");
		lblSmscHost.setBounds(10, 70, 401, 14);
		panel.add(lblSmscHost);
		
		JLabel lblSmscPort = new JLabel("SMSC port (for client mode), local port (for server mode)");
		lblSmscPort.setBounds(10, 101, 401, 14);
		panel.add(lblSmscPort);
		
		tbHost = new JTextField();
		tbHost.setColumns(10);
		tbHost.setBounds(424, 67, 180, 20);
		panel.add(tbHost);
		
		tbPort = new JTextField();
		tbPort.setColumns(10);
		tbPort.setBounds(424, 98, 86, 20);
		panel.add(tbPort);
		
		JLabel lblConnecttimeoutmilliseconds = new JLabel("ConnectTimeout (milliseconds)");
		lblConnecttimeoutmilliseconds.setBounds(10, 265, 401, 14);
		panel.add(lblConnecttimeoutmilliseconds);
		
		tbConnectTimeout = new JTextField();
		tbConnectTimeout.setColumns(10);
		tbConnectTimeout.setBounds(424, 262, 86, 20);
		panel.add(tbConnectTimeout);
		
		JLabel lblSystemid = new JLabel("SystemId");
		lblSystemid.setBounds(10, 14, 401, 14);
		panel.add(lblSystemid);
		
		tbSystemId = new JTextField();
		tbSystemId.setColumns(10);
		tbSystemId.setBounds(424, 11, 180, 20);
		panel.add(tbSystemId);
		
		JLabel lblPassword = new JLabel("Password");
		lblPassword.setBounds(10, 42, 401, 14);
		panel.add(lblPassword);
		
		tbPassword = new JTextField();
		tbPassword.setColumns(10);
		tbPassword.setBounds(424, 39, 180, 20);
		panel.add(tbPassword);
		
		JLabel lblRequestexpirytimeoutmilliseconds = new JLabel("RequestExpiryTimeout (milliseconds)");
		lblRequestexpirytimeoutmilliseconds.setBounds(10, 293, 401, 14);
		panel.add(lblRequestexpirytimeoutmilliseconds);
		
		tbRequestExpiryTimeout = new JTextField();
		tbRequestExpiryTimeout.setColumns(10);
		tbRequestExpiryTimeout.setBounds(424, 290, 86, 20);
		panel.add(tbRequestExpiryTimeout);
		
		JLabel lblWindowmonitorintervalmilliseconds = new JLabel("WindowMonitorInterval (milliseconds)");
		lblWindowmonitorintervalmilliseconds.setBounds(10, 321, 401, 14);
		panel.add(lblWindowmonitorintervalmilliseconds);
		
		tbWindowMonitorInterval = new JTextField();
		tbWindowMonitorInterval.setColumns(10);
		tbWindowMonitorInterval.setBounds(424, 318, 86, 20);
		panel.add(tbWindowMonitorInterval);
		
		cbRejectIncomingDeliveryMessage = new JCheckBox("Rejecting of incoming SMPP delivery messages");
		cbRejectIncomingDeliveryMessage.setBounds(10, 344, 524, 25);
		panel.add(cbRejectIncomingDeliveryMessage);
		
		JLabel lblSmppRole = new JLabel("Smpp session type");
		lblSmppRole.setBounds(10, 155, 401, 14);
		panel.add(lblSmppRole);
		
		cbSmppSessionType = new JComboBox();
		cbSmppSessionType.setBounds(424, 152, 180, 20);
		panel.add(cbSmppSessionType);
		
		JLabel lblEsmeaddressrangeField = new JLabel("Esme \"address_range\" field");
		lblEsmeaddressrangeField.setBounds(10, 181, 401, 14);
		panel.add(lblEsmeaddressrangeField);
		
		tbAddressRange = new JTextField();
		tbAddressRange.setColumns(10);
		tbAddressRange.setBounds(424, 178, 180, 20);
		panel.add(tbAddressRange);

	}

	public void setData(SmppSimulatorParameters data) {
		this.data = data;

		this.tbWindowSize.setText(((Integer) data.getWindowSize()).toString());
		this.tbHost.setText(data.getHost());
		this.tbPort.setText(((Integer) data.getPort()).toString());
		this.tbSystemId.setText(data.getSystemId());
		this.tbPassword.setText(data.getPassword());
		this.tbConnectTimeout.setText(((Long) data.getConnectTimeout()).toString());
		this.tbRequestExpiryTimeout.setText(((Long) data.getRequestExpiryTimeout()).toString());
		this.tbWindowMonitorInterval.setText(((Long) data.getWindowMonitorInterval()).toString());
		this.tbAddressRange.setText(data.getAddressRange());

        this.cbBindType.removeAllItems();
        SmppBindType[] vall = SmppBindType.values();
        SmppBindType dv = null;
        for (SmppBindType v : vall) {
            this.cbBindType.addItem(v);
            if (v == data.getBindType())
                dv = v;
        }
        if (dv != null)
            this.cbBindType.setSelectedItem(dv);

        this.cbSmppSessionType.removeAllItems();
        SmppSession.Type[] vall2 = SmppSession.Type.values();
        SmppSession.Type dv2 = null;
        for (SmppSession.Type v : vall2) {
            this.cbSmppSessionType.addItem(v);
            if (v == data.getSmppSessionType())
                dv2 = v;
        }
        if (dv2 != null)
            this.cbSmppSessionType.setSelectedItem(dv2);
		
		this.cbRejectIncomingDeliveryMessage.setSelected(this.data.isRejectIncomingDeliveryMessage());
	}

	public SmppSimulatorParameters getData() {
		return this.data;
	}

	private void doOK() {
//		this.data = new SmppSimulatorParameters();

		this.data.setHost(this.tbHost.getText());
		this.data.setSystemId(this.tbSystemId.getText());
		this.data.setPassword(this.tbPassword.getText());

		int intVal = 0;
		try {
			intVal = Integer.parseInt(this.tbWindowSize.getText());
			this.data.setWindowSize(intVal);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Exception when parsing WindowSize value: " + e.toString());
			return;
		}
		try {
			intVal = Integer.parseInt(this.tbPort.getText());
			this.data.setPort(intVal);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Exception when parsing Port value: " + e.toString());
			return;
		}

		long longVal = 0;
		try {
			longVal = Long.parseLong(this.tbConnectTimeout.getText());
			this.data.setConnectTimeout(longVal);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Exception when parsing ConnectTimeout value: " + e.toString());
			return;
		}
		try {
			longVal = Long.parseLong(this.tbRequestExpiryTimeout.getText());
			this.data.setRequestExpiryTimeout(longVal);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Exception when parsing RequestExpiryTimeout value: " + e.toString());
			return;
		}
		try {
			longVal = Long.parseLong(this.tbWindowMonitorInterval.getText());
			this.data.setWindowMonitorInterval(longVal);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Exception when parsing WindowMonitorInterval value: " + e.toString());
			return;
		}

        this.data.setBindType((SmppBindType) cbBindType.getSelectedItem());
        this.data.setSmppSessionType((SmppSession.Type) cbSmppSessionType.getSelectedItem());

		this.data.setRejectIncomingDeliveryMessage(this.cbRejectIncomingDeliveryMessage.isSelected());

		this.data.setAddressRange(tbAddressRange.getText());
		
		this.dispose();
	}

	private void doCancel() {
		this.data = null;
		this.dispose();
	}
}
