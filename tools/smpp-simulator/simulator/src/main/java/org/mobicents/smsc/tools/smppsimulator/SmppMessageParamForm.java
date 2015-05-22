/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.smsc.tools.smppsimulator;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmppMessageParamForm extends JDialog {

	private static final long serialVersionUID = -7694148495845105185L;

	private SmppSimulatorParameters data;

	private JTextArea tbMessage;
	private JComboBox<SmppSimulatorParameters.EncodingType> cbEncodingType;
	private JComboBox<SmppSimulatorParameters.SplittingType> cbSplittingType;
	private JComboBox<SmppSimulatorParameters.TON> cbSrcTON;
	private JComboBox<SmppSimulatorParameters.NPI> cbSrcNPI;
	private JTextField tbSourceAddress;
	private JTextField tbDestAddress;
	private JComboBox<SmppSimulatorParameters.TON> cbDestTON;
	private JComboBox<SmppSimulatorParameters.NPI> cbDestNPI;
	private JComboBox<SmppSimulatorParameters.ValidityType> cbValidityType;
	private JTextField tbBulkDestAddressRangeStart;
	private JTextField tbBulkDestAddressRangeEnd;
	private JTextField tbBulkMessagePerSecond;
	private JCheckBox cbMessageClass;
	private JComboBox<SmppSimulatorParameters.SendingMessageType> cbSendingMessageType;
	private JComboBox<SmppSimulatorParameters.MCDeliveryReceipt> cbMcDeliveryReceipt;
	private JRadioButton rbUtf8;
	private JRadioButton rbUnicode;
	private JComboBox<SmppSimulatorParameters.MessagingMode> cbMessagingMode;
	private final ButtonGroup buttonGroup = new ButtonGroup();

	public SmppMessageParamForm(JDialog owner) {
		super(owner, true);

		setTitle("SMPP message parameters");
		setResizable(false);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 620, 772);

		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(null);
		
		JLabel lblTextEncodingType = new JLabel("Text encoding type");
		lblTextEncodingType.setBounds(10, 109, 329, 14);
		panel.add(lblTextEncodingType);
		
		cbEncodingType = new JComboBox<SmppSimulatorParameters.EncodingType>();
		cbEncodingType.setBounds(349, 106, 255, 20);
		panel.add(cbEncodingType);
		
		JLabel lblMessageText = new JLabel("Message text");
		lblMessageText.setBounds(10, 14, 401, 14);
		panel.add(lblMessageText);
		
		JButton button = new JButton("OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doOK();
			}
		});
		button.setBounds(327, 712, 136, 23);
		panel.add(button);
		
		JButton button_1 = new JButton("Cancel");
		button_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doCancel();
			}
		});
		button_1.setBounds(468, 712, 136, 23);
		panel.add(button_1);

		cbSplittingType = new JComboBox<SmppSimulatorParameters.SplittingType>();
		cbSplittingType.setBounds(349, 165, 255, 20);
		panel.add(cbSplittingType);

        JLabel lblMessageSplittingType = new JLabel("Message splitting type");
        lblMessageSplittingType.setBounds(10, 168, 329, 14);
        panel.add(lblMessageSplittingType);

        tbMessage = new JTextArea();
        tbMessage.setBounds(10, 39, 594, 56);
        // panel.add(tbMessage);

        JScrollPane scrollPane = new JScrollPane(tbMessage);
        scrollPane.setBounds(0, 40, 604, 58);
        panel.add(scrollPane);

        JLabel lblTypeOfNumber = new JLabel("Source address: Type of number");
        lblTypeOfNumber.setBounds(10, 199, 329, 14);
        panel.add(lblTypeOfNumber);

        cbSrcTON = new JComboBox<SmppSimulatorParameters.TON>();
        cbSrcTON.setBounds(349, 196, 255, 20);
        panel.add(cbSrcTON);

        JLabel lblNumberingPlanIndicator = new JLabel("Source address: Numbering plan indicator");
        lblNumberingPlanIndicator.setBounds(10, 227, 329, 14);
        panel.add(lblNumberingPlanIndicator);

        cbSrcNPI = new JComboBox<SmppSimulatorParameters.NPI>();
        cbSrcNPI.setBounds(349, 224, 255, 20);
        panel.add(cbSrcNPI);

        tbSourceAddress = new JTextField();
        tbSourceAddress.setBounds(349, 344, 255, 20);
        panel.add(tbSourceAddress);
        tbSourceAddress.setColumns(10);

        JLabel lblSourceAddress = new JLabel("Source address");
        lblSourceAddress.setBounds(10, 347, 329, 14);
        panel.add(lblSourceAddress);

        tbDestAddress = new JTextField();
        tbDestAddress.setColumns(10);
        tbDestAddress.setBounds(349, 374, 255, 20);
        panel.add(tbDestAddress);

        JLabel lblDestinationAddress = new JLabel("Destination address");
        lblDestinationAddress.setBounds(10, 377, 329, 14);
        panel.add(lblDestinationAddress);

        JLabel lblDestinationAddressType = new JLabel("Destination address: Type of number");
        lblDestinationAddressType.setBounds(10, 257, 329, 14);
        panel.add(lblDestinationAddressType);

        JLabel lblDestinationAddressNumbering = new JLabel("Destination address: Numbering plan indicator");
        lblDestinationAddressNumbering.setBounds(10, 285, 329, 14);
        panel.add(lblDestinationAddressNumbering);

        cbDestTON = new JComboBox<SmppSimulatorParameters.TON>();
        cbDestTON.setBounds(349, 254, 255, 20);
        panel.add(cbDestTON);

        cbDestNPI = new JComboBox<SmppSimulatorParameters.NPI>();
        cbDestNPI.setBounds(349, 282, 255, 20);
        panel.add(cbDestNPI);

        JLabel lblValidityPeriod = new JLabel("Validity period / schedule delivery time");
        lblValidityPeriod.setBounds(10, 409, 329, 14);
        panel.add(lblValidityPeriod);

        cbValidityType = new JComboBox<SmppSimulatorParameters.ValidityType>();
        cbValidityType.setBounds(349, 406, 255, 20);
        panel.add(cbValidityType);

        JPanel panel_1 = new JPanel();
        panel_1.setBounds(10, 547, 592, 152);
        panel.add(panel_1);
        panel_1.setLayout(null);

        JLabel lblBulkMessageSending = new JLabel("Bulk message sending options");
        lblBulkMessageSending.setBounds(12, 13, 329, 14);
        panel_1.add(lblBulkMessageSending);

        JLabel lblDestinationAddressRange = new JLabel("Destination address range start");
        lblDestinationAddressRange.setBounds(12, 43, 329, 14);
        panel_1.add(lblDestinationAddressRange);

        tbBulkDestAddressRangeStart = new JTextField();
        tbBulkDestAddressRangeStart.setColumns(10);
        tbBulkDestAddressRangeStart.setBounds(351, 40, 229, 20);
        panel_1.add(tbBulkDestAddressRangeStart);

        tbBulkDestAddressRangeEnd = new JTextField();
        tbBulkDestAddressRangeEnd.setColumns(10);
        tbBulkDestAddressRangeEnd.setBounds(351, 70, 229, 20);
        panel_1.add(tbBulkDestAddressRangeEnd);

        JLabel lblDestinationAddressRange_1 = new JLabel("Destination address range end");
        lblDestinationAddressRange_1.setBounds(12, 73, 329, 14);
        panel_1.add(lblDestinationAddressRange_1);

        JLabel lblBulkMessagesPer = new JLabel("Bulk messages per second");
        lblBulkMessagesPer.setBounds(12, 104, 329, 14);
        panel_1.add(lblBulkMessagesPer);

        tbBulkMessagePerSecond = new JTextField();
        tbBulkMessagePerSecond.setColumns(10);
        tbBulkMessagePerSecond.setBounds(351, 101, 229, 20);
        panel_1.add(tbBulkMessagePerSecond);

        cbMessageClass = new JCheckBox("Add message class 0");
        cbMessageClass.setBounds(349, 135, 255, 25);
        panel.add(cbMessageClass);

        JLabel lblSendingMessageType = new JLabel("Sending message type");
        lblSendingMessageType.setBounds(10, 439, 329, 14);
        panel.add(lblSendingMessageType);

        cbSendingMessageType = new JComboBox<SmppSimulatorParameters.SendingMessageType>();
//        cbSendingMessageType.addItemListener(new ItemListener() {
//            public void itemStateChanged(ItemEvent arg0) {
//                if (cbSendingMessageType.getSelectedItem().toString().equals(SendingMessageType.SubmitMulti.toString())) {
//                    tbSubmitMultiMessageCnt.setEnabled(true);
//                } else {
//                    tbSubmitMultiMessageCnt.setEnabled(false);
//                }
//            }
//        });
        cbSendingMessageType.setBounds(349, 436, 255, 20);
        panel.add(cbSendingMessageType);

        JLabel lblMcdeliveryreceiptRequestin = new JLabel("MCDeliveryReceipt request (in registered_delivery)");
        lblMcdeliveryreceiptRequestin.setBounds(10, 495, 329, 14);
        panel.add(lblMcdeliveryreceiptRequestin);

        cbMcDeliveryReceipt = new JComboBox<SmppSimulatorParameters.MCDeliveryReceipt>();
        cbMcDeliveryReceipt.setBounds(349, 492, 255, 20);
        panel.add(cbMcDeliveryReceipt);

        JLabel lblEncodingTypeAt = new JLabel("Encoding type at SMPP part for (GSM7/UCS2)");
        lblEncodingTypeAt.setBounds(10, 312, 401, 14);
        panel.add(lblEncodingTypeAt);

        rbUtf8 = new JRadioButton("Utf8");
        buttonGroup.add(rbUtf8);
        rbUtf8.setBounds(418, 307, 73, 25);
        panel.add(rbUtf8);

        rbUnicode = new JRadioButton("Unicode");
        buttonGroup.add(rbUnicode);
        rbUnicode.setBounds(495, 307, 109, 25);
        panel.add(rbUnicode);

        JLabel lblMessagingMode = new JLabel("Messaging mode");
        lblMessagingMode.setBounds(10, 523, 329, 14);
        panel.add(lblMessagingMode);

        cbMessagingMode = new JComboBox<SmppSimulatorParameters.MessagingMode>();
        cbMessagingMode.setBounds(349, 520, 255, 20);
        panel.add(cbMessagingMode);
	}

	public void setData(SmppSimulatorParameters data) {
		this.data = data;

		this.tbMessage.setText(data.getMessageText());
		this.tbSourceAddress.setText(data.getSourceAddress());
		this.tbDestAddress.setText(data.getDestAddress());

		this.tbBulkDestAddressRangeStart.setText(((Integer)data.getBulkDestAddressRangeStart()).toString());
		this.tbBulkDestAddressRangeEnd.setText(((Integer)data.getBulkDestAddressRangeEnd()).toString());
		this.tbBulkMessagePerSecond.setText(((Integer)data.getBulkMessagePerSecond()).toString());

		this.cbEncodingType.removeAllItems();
		SmppSimulatorParameters.EncodingType[] vallET = SmppSimulatorParameters.EncodingType.values();
		SmppSimulatorParameters.EncodingType dv = null;
		for (SmppSimulatorParameters.EncodingType v : vallET) {
			this.cbEncodingType.addItem(v);
			if (v == data.getEncodingType())
				dv = v;
		}
		if (dv != null)
			this.cbEncodingType.setSelectedItem(dv);

		this.cbSplittingType.removeAllItems();
		SmppSimulatorParameters.SplittingType[] vallST = SmppSimulatorParameters.SplittingType.values();
		SmppSimulatorParameters.SplittingType dvST = null;
		for (SmppSimulatorParameters.SplittingType v : vallST) {
			this.cbSplittingType.addItem(v);
			if (v == data.getSplittingType())
				dvST = v;
		}
		if (dvST != null)
			this.cbSplittingType.setSelectedItem(dvST);

		this.cbSrcTON.removeAllItems();
		SmppSimulatorParameters.TON[] vallTON = SmppSimulatorParameters.TON.values();
		SmppSimulatorParameters.TON dvTON = null;
		for (SmppSimulatorParameters.TON v : vallTON) {
			this.cbSrcTON.addItem(v);
			if (v == data.getSourceTON())
				dvTON = v;
		}
		if (dvTON != null)
			this.cbSrcTON.setSelectedItem(dvTON);

		this.cbDestTON.removeAllItems();
		vallTON = SmppSimulatorParameters.TON.values();
		dvTON = null;
		for (SmppSimulatorParameters.TON v : vallTON) {
			this.cbDestTON.addItem(v);
			if (v == data.getDestTON())
				dvTON = v;
		}
		if (dvTON != null)
			this.cbDestTON.setSelectedItem(dvTON);

		this.cbSrcNPI.removeAllItems();
		SmppSimulatorParameters.NPI[] vallNPI = SmppSimulatorParameters.NPI.values();
		SmppSimulatorParameters.NPI dvNPI = null;
		for (SmppSimulatorParameters.NPI v : vallNPI) {
			this.cbSrcNPI.addItem(v);
			if (v == data.getSourceNPI())
				dvNPI = v;
		}
		if (dvNPI != null)
			this.cbSrcNPI.setSelectedItem(dvNPI);

		this.cbDestNPI.removeAllItems();
		vallNPI = SmppSimulatorParameters.NPI.values();
		dvNPI = null;
		for (SmppSimulatorParameters.NPI v : vallNPI) {
			this.cbDestNPI.addItem(v);
			if (v == data.getDestNPI())
				dvNPI = v;
		}
		if (dvNPI != null)
			this.cbDestNPI.setSelectedItem(dvNPI);

        this.cbValidityType.removeAllItems();
        SmppSimulatorParameters.ValidityType[] vallValType = SmppSimulatorParameters.ValidityType.values();
        SmppSimulatorParameters.ValidityType dvValType = null;
        for (SmppSimulatorParameters.ValidityType v : vallValType) {
            this.cbValidityType.addItem(v);
            if (v == data.getValidityType())
                dvValType = v;
        }
        if (dvValType != null)
            this.cbValidityType.setSelectedItem(dvValType);

        this.cbSendingMessageType.removeAllItems();
        SmppSimulatorParameters.SendingMessageType[] vallSendingMessageType = SmppSimulatorParameters.SendingMessageType.values();
        SmppSimulatorParameters.SendingMessageType dvSendingMessageType = null;
        for (SmppSimulatorParameters.SendingMessageType v : vallSendingMessageType) {
            this.cbSendingMessageType.addItem(v);
            if (v == data.getSendingMessageType())
                dvSendingMessageType = v;
        }
        if (dvSendingMessageType != null)
            this.cbSendingMessageType.setSelectedItem(dvSendingMessageType);

        this.cbMessagingMode.removeAllItems();
        SmppSimulatorParameters.MessagingMode[] vallMessagingMode = SmppSimulatorParameters.MessagingMode.values();
        SmppSimulatorParameters.MessagingMode dvMessagingMode = null;
        for (SmppSimulatorParameters.MessagingMode v : vallMessagingMode) {
            this.cbMessagingMode.addItem(v);
            if (v == data.getMessagingMode())
                dvMessagingMode = v;
        }
        if (dvMessagingMode != null)
            this.cbMessagingMode.setSelectedItem(dvMessagingMode);

        this.cbMcDeliveryReceipt.removeAllItems();
        SmppSimulatorParameters.MCDeliveryReceipt[] vallMcDeliveryReceipt = SmppSimulatorParameters.MCDeliveryReceipt.values();
        SmppSimulatorParameters.MCDeliveryReceipt dvMcDeliveryReceipt = null;
        for (SmppSimulatorParameters.MCDeliveryReceipt v : vallMcDeliveryReceipt) {
            this.cbMcDeliveryReceipt.addItem(v);
            if (v == data.getMcDeliveryReceipt())
                dvMcDeliveryReceipt = v;
        }
        if (dvMcDeliveryReceipt != null)
            this.cbMcDeliveryReceipt.setSelectedItem(dvMcDeliveryReceipt);


        this.cbMessageClass.setSelected(data.isMessageClass());

        if (data.getSmppEncoding() == 0)
            this.rbUtf8.setSelected(true);
        else
            this.rbUnicode.setSelected(true);
	}

	public SmppSimulatorParameters getData() {
		return this.data;
	}

	private void doOK() {
//		this.data = new SmppSimulatorParameters();

		this.data.setMessageText(this.tbMessage.getText());
		this.data.setSourceAddress(this.tbSourceAddress.getText());
		this.data.setDestAddress(this.tbDestAddress.getText());

		try {
			int val = Integer.parseInt(this.tbBulkDestAddressRangeStart.getText());
			if (val < 0)
				throw new NumberFormatException();
			data.setBulkDestAddressRangeStart(val);
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Error in BulkDestAddressRangeStart field - it must be digital and positive");
			return;
		}
		try {
			int val = Integer.parseInt(this.tbBulkDestAddressRangeEnd.getText());
			if (val < 0)
				throw new NumberFormatException();
			data.setBulkDestAddressRangeEnd(val);
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Error in BulkDestAddressRangeEnd field - it must be digital and positive");
			return;
		}
		try {
			int val = Integer.parseInt(this.tbBulkMessagePerSecond.getText());
			if (val < 0)
				throw new NumberFormatException();
			data.setBulkMessagePerSecond(val);
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Error in BulkMessagePerSecond field - it must be digital and positive");
			return;
		}

		this.data.setEncodingType((SmppSimulatorParameters.EncodingType) cbEncodingType.getSelectedItem());
		this.data.setSplittingType((SmppSimulatorParameters.SplittingType) cbSplittingType.getSelectedItem());
		this.data.setSourceTON((SmppSimulatorParameters.TON) cbSrcTON.getSelectedItem());
		this.data.setSourceNPI((SmppSimulatorParameters.NPI) cbSrcNPI.getSelectedItem());
		this.data.setDestTON((SmppSimulatorParameters.TON) cbDestTON.getSelectedItem());
		this.data.setDestNPI((SmppSimulatorParameters.NPI) cbDestNPI.getSelectedItem());
        this.data.setValidityType((SmppSimulatorParameters.ValidityType) cbValidityType.getSelectedItem());
        this.data.setSendingMessageType((SmppSimulatorParameters.SendingMessageType) cbSendingMessageType.getSelectedItem());
        this.data.setMcDeliveryReceipt((SmppSimulatorParameters.MCDeliveryReceipt) cbMcDeliveryReceipt.getSelectedItem());
        this.data.setMessagingMode((SmppSimulatorParameters.MessagingMode) cbMessagingMode.getSelectedItem());

        this.data.setMessageClass(this.cbMessageClass.isSelected());

        if (this.rbUtf8.isSelected())
            this.data.setSmppEncoding(0);
        else
            this.data.setSmppEncoding(1);

		this.dispose();
	}

	private void doCancel() {
		this.data = null;
		this.dispose();
	}
}

