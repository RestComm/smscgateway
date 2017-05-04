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

package org.mobicents.smsc.tools.smppsimulator;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JScrollPane;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;

import java.awt.Color;

import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.JButton;

import org.mobicents.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharset;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharsetEncoder;
import org.mobicents.protocols.ss7.map.datacoding.GSMCharsetEncodingData;
import org.mobicents.protocols.ss7.map.datacoding.Gsm7EncodingStyle;
import org.mobicents.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import org.mobicents.smsc.library.MessageUtil;
import org.mobicents.smsc.tools.smppsimulator.SmppSimulatorParameters.EncodingType;
import org.mobicents.smsc.tools.smppsimulator.SmppSimulatorParameters.SendingMessageType;
import org.mobicents.smsc.tools.smppsimulator.SmppSimulatorParameters.SplittingType;
import org.mobicents.smsc.tools.smppsimulator.SmppSimulatorParameters.ValidityType;
import org.mobicents.smsc.tools.smppsimulator.testsmpp.TestSmppClient;
import org.mobicents.smsc.tools.smppsimulator.testsmpp.TestSmppSession;

import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseSm;
import com.cloudhopper.smpp.pdu.DataSm;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitMulti;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.SmppChannelException;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.ButtonGroup;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmppTestingForm extends JDialog implements SmppAccepter {

	private static final long serialVersionUID = 4969830723671541575L;

	private DefaultTableModel model = new DefaultTableModel();
	private EventForm eventForm;
	private SmppSimulatorForm mainForm;
	private SmppSimulatorParameters param;
	private JTable tNotif;
	private JButton btStart;
	private JButton btStop;
	private JButton btStartBulk;
	private JButton btStopBulk;
	private javax.swing.Timer tm;
	private JLabel lbState;
	private JRadioButton rbRandomBulkMessages;
    private JRadioButton rbBulkMessagesFrom;
    private JButton btPcapFileName;

	private ThreadPoolExecutor executor;
	private ScheduledThreadPoolExecutor monitorExecutor;
	private TestSmppClient clientBootstrap;
	private SmppSession session0;
	private DefaultSmppServer defaultSmppServer;

	protected Timer[] timer;
	protected AtomicInteger messagesSent = new AtomicInteger();
	protected AtomicInteger segmentsSent = new AtomicInteger();
    protected AtomicInteger responsesRcvd = new AtomicInteger();
    protected AtomicInteger messagesRcvd = new AtomicInteger();

	private static Charset utf8Charset = Charset.forName("UTF-8");
    private static Charset ucs2Charset = Charset.forName("UTF-16BE");
    private static Charset isoCharset = Charset.forName("ISO-8859-1");
    private static Charset gsm7Charset = new GSMCharset("GSM", new String[] {});

    private AtomicLong msgIdGenerator;

	public SmppTestingForm(JFrame owner) {
		super(owner, true);
		setModal(false);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (getDefaultCloseOperation() == JDialog.DO_NOTHING_ON_CLOSE) {
					JOptionPane.showMessageDialog(getJDialog(), "Before exiting you must Stop the testing process");
				} else {
					closingWindow();
				}
			}
		});
		setBounds(100, 100, 772, 677);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(new GridLayout(0, 1, 0, 0));
		
		JPanel panel_1 = new JPanel();
		panel.add(panel_1);
		panel_1.setLayout(new GridLayout(1, 0, 0, 0));
		
		JScrollPane scrollPane = new JScrollPane((Component) null);
		panel_1.add(scrollPane);
		
		tNotif = new JTable();
		tNotif.setFillsViewportHeight(true);
		tNotif.setBorder(new LineBorder(new Color(0, 0, 0)));
		tNotif.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tNotif.setModel(new DefaultTableModel(new Object[][] {}, new String[] { "TimeStamp", "Message", "UserData" }) {
			Class[] columnTypes = new Class[] { String.class, String.class, String.class };

			public Class getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}

			boolean[] columnEditables = new boolean[] { false, false, false };

			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		});
		tNotif.getColumnModel().getColumn(0).setPreferredWidth(46);
		tNotif.getColumnModel().getColumn(1).setPreferredWidth(221);
		tNotif.getColumnModel().getColumn(2).setPreferredWidth(254);

		scrollPane.setViewportView(tNotif);

		model = (DefaultTableModel) tNotif.getModel();

		tNotif.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {

				if (e.getValueIsAdjusting())
					return;
				if (eventForm == null)
					return;

				// Номер текущей строки таблицы
				setEventMsg();
			}
		});

		JPanel panel_2 = new JPanel();
		panel.add(panel_2);
		panel_2.setLayout(null);
		
		btStart = new JButton("Start a session");
		btStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				start();
			}
		});
		btStart.setBounds(10, 11, 141, 23);
		panel_2.add(btStart);
		
		btStop = new JButton("Stop a session");
		btStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stop();
			}
		});
		btStop.setEnabled(false);
		btStop.setBounds(158, 11, 122, 23);
		panel_2.add(btStop);
		
		JButton btRefreshState = new JButton("Refresh state");
		btRefreshState.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refreshState();
			}
		});
		btRefreshState.setBounds(286, 11, 148, 23);
		panel_2.add(btRefreshState);
		
		JButton btOpeEventWindow = new JButton("Open event window");
		btOpeEventWindow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openEventWindow();
			}
		});
		btOpeEventWindow.setBounds(439, 11, 159, 23);
		panel_2.add(btOpeEventWindow);
		
		JButton btConfigSubmitData = new JButton("Configure data for a message submitting");
		btConfigSubmitData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SmppMessageParamForm frame = new SmppMessageParamForm(getJDialog());
				frame.setData(param);
				frame.setVisible(true);

				SmppSimulatorParameters newPar = frame.getData();
				if (newPar != null) {
					param = newPar;

					try {
						BufferedOutputStream bis = new BufferedOutputStream(new FileOutputStream("SmppSimulatorParameters.xml"));
						XMLEncoder d = new XMLEncoder(bis);
						d.writeObject(newPar);
						d.close();
					} catch (Exception ee) {
						ee.printStackTrace();
						JOptionPane.showMessageDialog(null, "Failed when saving the parameter file SmppSimulatorParameters.xml: " + ee.getMessage());
					}
				}
			}
		});
		btConfigSubmitData.setBounds(11, 46, 341, 23);
		panel_2.add(btConfigSubmitData);
		
		JButton btSendMessage = new JButton("Submit a message");
		btSendMessage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
                submitMessage(param.getEncodingType(), param.betMessageClass(), param.getMessageText(),
                        param.getSplittingType(), param.getValidityType(), param.getDestAddress(), param.getMessagingMode(),
                        param.getSpecifiedSegmentLength());
			}
		});
		btSendMessage.setBounds(11, 80, 341, 23);
		panel_2.add(btSendMessage);

		btStartBulk = new JButton("Start bulk sending");
		btStartBulk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				startBulkSending();
			}
		});
		btStartBulk.setBounds(10, 172, 201, 23);
		panel_2.add(btStartBulk);
		
		btStopBulk = new JButton("Stop bulk sending");
		btStopBulk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stopBulkSending();
			}
		});
		btStopBulk.setEnabled(false);
		btStopBulk.setBounds(223, 172, 211, 23);
		panel_2.add(btStopBulk);
		
		lbState = new JLabel("-");
		lbState.setBounds(14, 206, 732, 16);
		panel_2.add(lbState);
		
		rbRandomBulkMessages = new JRadioButton("Random bulk messages");
		rbRandomBulkMessages.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent arg0) {
                tbPcapFileName.setEnabled(false);
                btPcapFileName.setEnabled(false);
                tbPcapPort.setEnabled(false);
		    }
		});
		buttonGroup.add(rbRandomBulkMessages);
		rbRandomBulkMessages.setSelected(true);
		rbRandomBulkMessages.setBounds(10, 127, 197, 23);
		panel_2.add(rbRandomBulkMessages);
		
		rbBulkMessagesFrom = new JRadioButton("Bulk messages from pcap file");
		rbBulkMessagesFrom.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent arg0) {
                tbPcapFileName.setEnabled(true);
                btPcapFileName.setEnabled(true);
                tbPcapPort.setEnabled(true);
		    }
		});
		buttonGroup.add(rbBulkMessagesFrom);
		rbBulkMessagesFrom.setBounds(10, 149, 204, 23);
		panel_2.add(rbBulkMessagesFrom);
		
		tbPcapFileName = new JTextField();
		tbPcapFileName.setEnabled(false);
		tbPcapFileName.setBounds(251, 128, 439, 20);
		panel_2.add(tbPcapFileName);
		tbPcapFileName.setColumns(10);
		
		btPcapFileName = new JButton(". . .");
		btPcapFileName.setEnabled(false);
		btPcapFileName.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent arg0) {
                JFileChooser chooser = new JFileChooser();
                String filterName = null;
                filterName = "Pcap";
                TraceFileFilter filter = new TraceFileFilter(filterName);
                chooser.setFileFilter(filter);
                chooser.addChoosableFileFilter(filter);
                File f = new File(tbPcapFileName.getText());
                chooser.setSelectedFile(f);
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                int returnVal = chooser.showOpenDialog(getJDialog());
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File f2 = chooser.getSelectedFile();
                    if (f2 != null && f2.exists())
                        tbPcapFileName.setText(f2.getPath());
                    else
                        JOptionPane.showMessageDialog(null, "File does not exists - try again");
                }
		    }
		});
		btPcapFileName.setBounds(693, 127, 53, 23);
		panel_2.add(btPcapFileName);
		
		JLabel lblTcpPortFor = new JLabel("TCP Port for pcap parsing");
		lblTcpPortFor.setBounds(251, 154, 240, 14);
		panel_2.add(lblTcpPortFor);
		
		tbPcapPort = new JTextField();
		tbPcapPort.setText("2775");
		tbPcapPort.setEnabled(false);
		tbPcapPort.setBounds(501, 150, 86, 20);
		panel_2.add(tbPcapPort);
		tbPcapPort.setColumns(10);
		
		JButton btSendBadPacket = new JButton("Send Bad packet");
		btSendBadPacket.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
                doSendBadPacket();
		    }
		});
		btSendBadPacket.setBounds(509, 45, 129, 23);
		panel_2.add(btSendBadPacket);

		this.tm = new javax.swing.Timer(5000, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refreshState();
			}
		});
		this.tm.start();

        Random rn = new Random();
        msgIdGenerator = new AtomicLong(rn.nextInt(100000000));
	}

    public ScheduledThreadPoolExecutor getExecutor() {
        return this.monitorExecutor;
    }

    public SmppSession getSession() {
        return session0;
    }

    public AtomicLong getMsgIdGenerator() {
        return msgIdGenerator;
    }

    private void doSendBadPacket() {
        // TODO: ..............................
        SubmitSm submitSm = new SubmitSm();
        try {
            ((TestSmppSession)this.session0).setMalformedPacket();
            this.session0.submit(submitSm, 1000);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

	private int msgRef = 0;

	public SmppSimulatorParameters getSmppSimulatorParameters() {
		return this.param;
	}

    public void setSmppSession(SmppSession smppSession) {
        this.session0 = smppSession;
    }

    public SmppSession getSmppSession() {
        return this.session0;
    }

	private int getNextMsgRef() {
		msgRef++;
		if (msgRef > 255)
			msgRef = 1;
		return msgRef;
	}

    private byte[] encodeSegment(String msg, EncodingType encodingType) {
        if (encodingType == EncodingType.GSM8_DCS_4) {
            return msg.getBytes(isoCharset);
        } else {
            if (this.param.getSmppEncoding() == 0) {
                return msg.getBytes(utf8Charset);
            } else if (this.param.getSmppEncoding() == 1) {
                return msg.getBytes(ucs2Charset);
            } else {
                GSMCharsetEncoder encoder = (GSMCharsetEncoder) gsm7Charset.newEncoder();
                encoder.setGSMCharsetEncodingData(new GSMCharsetEncodingData(Gsm7EncodingStyle.bit8_smpp_style, null));
                ByteBuffer bb = null;
                try {
                    bb = encoder.encode(CharBuffer.wrap(msg));
                } catch (CharacterCodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                byte[] data = new byte[bb.limit()];
                bb.get(data);
                return data;
            }
        }
    }

    private void submitMessage(EncodingType encodingType, int messageClass, String messageText, SplittingType splittingType, ValidityType validityType,
            String destAddr, SmppSimulatorParameters.MessagingMode messagingMode, int specifiedSegmentLength) {
        if (session0 == null)
            return;

        try {
        	int dcs = 0;
			ArrayList<byte[]> msgLst = new ArrayList<byte[]>();
        	int msgRef = 0;

            switch (encodingType) {
            case GSM7_DCS_0:
                dcs = 0;
                break;
            case GSM8_DCS_4:
                dcs = 4;
                break;
            case UCS2_DCS_8:
                dcs = 8;
                break;
            }
            // if (messageClass) {
            // dcs += 16;
            // }
            int messageClassVal = 0;
            if (messageClass > 0) {
                messageClassVal = messageClass;
            }

            DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(dcs);
            int maxLen = MessageUtil.getMaxSolidMessageCharsLength(dataCodingScheme);
            int maxSplLen = MessageUtil.getMaxSegmentedMessageCharsLength(dataCodingScheme);
            if (splittingType == SplittingType.SplitWithParameters_SpecifiedSegmentLength
                    || splittingType == SplittingType.SplitWithUdh_SpecifiedSegmentLength) {
                maxLen = specifiedSegmentLength;
                maxSplLen = specifiedSegmentLength;
            }

            int segmCnt = 0;
            int esmClass = 0;
            boolean addSegmTlv = false;
            if (messageText.length() > maxLen) { // may be message splitting
                SplittingType st = splittingType;
                switch (st) {
                case DoNotSplit:
                    // we do not split
                    byte[] buf1 = encodeSegment(messageText, encodingType);
                    byte[] buf2;
                    if (encodingType == EncodingType.GSM8_DCS_4) {
                        // 4-bytes length
                        byte[] bf3 = new byte[7];
                        bf3[0] = 6; // total UDH length
                        bf3[1] = 5; // UDH id
                        bf3[2] = 4; // UDH length
                        bf3[3] = 0x3E;
                        bf3[4] = (byte) 0x94;
                        bf3[5] = 0;
                        bf3[6] = 0;

                        // 0-bytes length
//                        bf3 = new byte[3];
//                        bf3[0] = 2; // total UDH length
//                        bf3[1] = 112; // UDH id
//                        bf3[2] = 0; // UDH length
                        
                        buf2 = new byte[bf3.length + buf1.length];
                        System.arraycopy(bf3, 0, buf2, 0, bf3.length);
                        System.arraycopy(buf1, 0, buf2, bf3.length, buf1.length);
                        esmClass = 0x40;
                    } else {
                        buf2 = buf1;
                    }
                    msgLst.add(buf2);
                    ArrayList<String> r1 = this.splitStr(messageText, maxSplLen);
                    segmCnt = r1.size();
                    break;
                case SplitWithParameters_DefaultSegmentLength:
                case SplitWithParameters_SpecifiedSegmentLength:
                    msgRef = getNextMsgRef();
                    r1 = this.splitStr(messageText, maxSplLen);
                    for (String bf : r1) {
                        msgLst.add(encodeSegment(bf, encodingType));
                    }
                    segmCnt = msgLst.size();
                    addSegmTlv = true;
                    break;
                case SplitWithUdh_DefaultSegmentLength:
                case SplitWithUdh_SpecifiedSegmentLength:
                    msgRef = getNextMsgRef();
                    r1 = this.splitStr(messageText, maxSplLen);
                    byte[] bf1 = new byte[6];
                    bf1[0] = 5; // total UDH length
                    bf1[1] = 0; // UDH id
                    bf1[2] = 3; // UDH length
                    bf1[3] = (byte) msgRef; // refNum
                    bf1[4] = (byte) r1.size(); // segmCnt
                    int i1 = 0;
                    for (String bfStr : r1) {
                        byte[] bf = encodeSegment(bfStr, encodingType);
                        i1++;
                        bf1[5] = (byte) i1; // segmNum
                        byte[] bf2 = new byte[bf1.length + bf.length];
                        System.arraycopy(bf1, 0, bf2, 0, bf1.length);
                        System.arraycopy(bf, 0, bf2, bf1.length, bf.length);
                        msgLst.add(bf2);
                    }
                    segmCnt = msgLst.size();
                    esmClass = 0x40;
                    break;
                }
            } else {
                byte[] buf = encodeSegment(messageText, encodingType);
                if (encodingType == EncodingType.GSM8_DCS_4) {
                    byte[] bf1 = new byte[7];
                    bf1[0] = 6; // total UDH length
                    bf1[1] = 5; // UDH id
                    bf1[2] = 4; // UDH length
                    bf1[3] = 0x3e;
                    bf1[4] = (byte) 0x94;
                    bf1[5] = 0;
                    bf1[6] = 0;

                    // 0-bytes length
//                    bf1 = new byte[3];
//                    bf1[0] = 2; // total UDH length
//                    bf1[1] = 112; // UDH id
//                    bf1[2] = 0; // UDH length

                    byte[] bf2 = new byte[bf1.length + buf.length];
                    System.arraycopy(bf1, 0, bf2, 0, bf1.length);
                    System.arraycopy(buf, 0, bf2, bf1.length, buf.length);
                    msgLst.add(bf2);
                    esmClass = 0x40;
                } else {
                    msgLst.add(buf);
                }
                segmCnt = 1;
            }
            esmClass |= messagingMode.getCode();

            this.doSubmitMessage(dcs, msgLst, msgRef, addSegmTlv, esmClass, validityType, segmCnt, destAddr, messageClassVal);
		} catch (Exception e) {
			this.addMessage("Failure to submit message", e.toString());
			return;
		}
	}

	private ArrayList<String> splitStr(String buf, int maxLen) {
		ArrayList<String> res = new ArrayList<String>();

		String prevBuf = buf;

		while (true) {
			if (prevBuf.length() <= maxLen) {
				res.add(prevBuf);
				break;
			}

            String segm = prevBuf.substring(0, maxLen);
            String newBuf = prevBuf.substring(maxLen, prevBuf.length());

//			String segm = new byte[maxLen];
//			String newBuf = new byte[prevBuf.length - maxLen];
//
//			System.arraycopy(prevBuf, 0, segm, 0, maxLen);
//			System.arraycopy(prevBuf, maxLen, newBuf, 0, prevBuf.length - maxLen);
			
			res.add(segm);
			prevBuf = newBuf;
		}

		return res;
	}

    private void doSubmitMessage(int dcs, ArrayList<byte[]> msgLst, int msgRef, boolean addSegmTlv, int esmClass,
            SmppSimulatorParameters.ValidityType validityType, int segmentCnt, String destAddr, int messageClassVal)
            throws Exception {
        int i1 = 0;
		for (byte[] buf : msgLst) {
			i1++;

            BaseSm pdu;
			switch(this.param.getSendingMessageType()){
            case SubmitSm:
                SubmitSm submitPdu = new SubmitSm();
                pdu = submitPdu;
                break;
            case DataSm:
                DataSm dataPdu = new DataSm();
                pdu = dataPdu;
                break;
            case DeliverSm:
                DeliverSm deliverPdu = new DeliverSm();
                pdu = deliverPdu;
                break;
            case SubmitMulti:
                SubmitMulti submitMulti = new SubmitMulti();
                pdu = submitMulti;
                break;
            default:
                return;
			}

            pdu.setSourceAddress(new Address((byte)this.param.getSourceTON().getCode(), (byte)this.param.getSourceNPI().getCode(), this.param.getSourceAddress()));

            if (this.param.getSendingMessageType() == SendingMessageType.SubmitMulti) {
                long daOrig = 1;
                try {
                    daOrig = Long.parseLong(destAddr);
                } catch (Exception e) {

                }
                for (int i2 = 0; i2 < this.param.getSubmitMultiMessageCnt(); i2++) {
                    // this code can be used for testing of address rejections
                    // if(i2 == 0){
                    // ((SubmitMulti) pdu).addDestAddresses(new Address((byte)
                    // 8, (byte) this.param.getDestNPI().getCode(), String
                    // .valueOf(daOrig + i2)));
                    // }else {
                    // ((SubmitMulti) pdu).addDestAddresses(new Address((byte)
                    // this.param.getDestTON().getCode(), (byte)
                    // this.param.getDestNPI().getCode(), String
                    // .valueOf(daOrig + i2)));
                    // }
                    
                    
                    ((SubmitMulti) pdu).addDestAddresses(new Address((byte) this.param.getDestTON().getCode(), (byte) this.param.getDestNPI().getCode(), String
                            .valueOf(daOrig + i2)));
                }
            } else {
                pdu.setDestAddress(new Address((byte) this.param.getDestTON().getCode(), (byte) this.param.getDestNPI().getCode(), destAddr));
            }

            pdu.setEsmClass((byte) esmClass);

			switch (validityType) {
            case ValidityPeriod_5min:
                pdu.setValidityPeriod(MessageUtil.printSmppRelativeDate(0, 0, 0, 0, 5, 0));
                break;
            case ValidityPeriod_2hours:
                pdu.setValidityPeriod(MessageUtil.printSmppRelativeDate(0, 0, 0, 2, 0, 0));
                break;
			case ScheduleDeliveryTime_5min:
			    pdu.setScheduleDeliveryTime(MessageUtil.printSmppRelativeDate(0, 0, 0, 0, 5, 0));
				break;
			}

            pdu.setDataCoding((byte) dcs);
            pdu.setRegisteredDelivery((byte) this.param.getMcDeliveryReceipt().getCode());

            if (buf.length < 250 && this.param.getSendingMessageType() != SmppSimulatorParameters.SendingMessageType.DataSm)
                pdu.setShortMessage(buf);
            else {
                Tlv tlv = new Tlv(SmppConstants.TAG_MESSAGE_PAYLOAD, buf);
                pdu.addOptionalParameter(tlv);
            }

			if (addSegmTlv) {
				byte[] buf1 = new byte[2];
				buf1[0] = 0;
				buf1[1] = (byte)msgRef;
				Tlv tlv = new Tlv(SmppConstants.TAG_SAR_MSG_REF_NUM, buf1);
				pdu.addOptionalParameter(tlv);
				buf1 = new byte[1];
				buf1[0] = (byte) msgLst.size();
				tlv = new Tlv(SmppConstants.TAG_SAR_TOTAL_SEGMENTS, buf1);
				pdu.addOptionalParameter(tlv);
				buf1 = new byte[1];
				buf1[0] = (byte)i1;
				tlv = new Tlv(SmppConstants.TAG_SAR_SEGMENT_SEQNUM, buf1);
				pdu.addOptionalParameter(tlv);
			}
            if (messageClassVal > 0) {
                byte[] buf1 = new byte[1];
                buf1[0] = (byte) messageClassVal;
                Tlv tlv = new Tlv(SmppConstants.TAG_DEST_ADDR_SUBUNIT, buf1);
                pdu.addOptionalParameter(tlv);
            }

            if(this.param.isSendOptionalParameter()){
                for(Tlv tlv: this.param.getTlvSet().getOptionalParameters()){
                    pdu.addOptionalParameter(tlv);
                }
            }

	        WindowFuture<Integer,PduRequest,PduResponse> future0 = session0.sendRequestPdu(pdu, 10000, false);

			this.messagesSent.incrementAndGet();
			if (this.timer == null) {
				this.addMessage("Request=" + pdu.getName(), pdu.toString());
			}
		}

		this.segmentsSent.addAndGet(segmentCnt);
	}

	private void setEventMsg() {
		ListSelectionModel l = tNotif.getSelectionModel();
		if (!l.isSelectionEmpty()) {
			int index = l.getMinSelectionIndex();
			String s1 = (String) model.getValueAt(index, 0);
			String s2 = (String) model.getValueAt(index, 1);
			String s3 = (String) model.getValueAt(index, 2);
			eventForm.setData(s1, s2, s3);
		}
	}

	private void start() {
		this.messagesSent = new AtomicInteger();
		this.segmentsSent = new AtomicInteger();
        this.responsesRcvd = new AtomicInteger();
        this.messagesRcvd = new AtomicInteger();

        this.addMessage("Trying to start a new " + this.param.getSmppSessionType() + " session", "");

        this.executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        this.monitorExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1, new ThreadFactory() {
            private AtomicInteger sequence = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("SmppClientSessionWindowMonitorPool-" + sequence.getAndIncrement());
                return t;
            }
        });

        if (this.param.getSmppSessionType() == SmppSession.Type.CLIENT) {
            clientBootstrap = new TestSmppClient(Executors.newCachedThreadPool(), 1, monitorExecutor);

            DefaultSmppSessionHandler sessionHandler = new ClientSmppSessionHandler(this);

            SmppSessionConfiguration config0 = new SmppSessionConfiguration();
            config0.setWindowSize(this.param.getWindowSize());
            config0.setName("Tester.Session.0");
            config0.setType(this.param.getBindType());
            config0.setHost(this.param.getHost());
            config0.setPort(this.param.getPort());
            config0.setConnectTimeout(this.param.getConnectTimeout());
            config0.setSystemId(this.param.getSystemId());
            config0.setPassword(this.param.getPassword());
            config0.setAddressRange(new Address((byte) 1, (byte) 1, this.param.getAddressRange()));
            config0.getLoggingOptions().setLogBytes(true);
            // to enable monitoring (request expiration)
            config0.setRequestExpiryTimeout(this.param.getRequestExpiryTimeout());
            config0.setWindowMonitorInterval(this.param.getWindowMonitorInterval());
            config0.setCountersEnabled(true);

            try {
                session0 = clientBootstrap.bind(config0, sessionHandler);
            } catch (Exception e) {
                this.addMessage("Failure to start a new session", e.toString());
                return;
            }

            enableStart(false);
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

            this.addMessage("Session has been successfully started", "");
        } else {

            SmppServerConfiguration configuration = new SmppServerConfiguration();
            configuration.setName("Test SMPP server");
            configuration.setPort(this.param.getPort());
            configuration.setBindTimeout(5000);
            configuration.setSystemId(this.param.getSystemId());
            configuration.setAutoNegotiateInterfaceVersion(true);
            configuration.setInterfaceVersion(SmppConstants.VERSION_3_4);
            configuration.setMaxConnectionSize(SmppConstants.DEFAULT_SERVER_MAX_CONNECTION_SIZE);
            configuration.setNonBlockingSocketsEnabled(true);

            configuration.setDefaultRequestExpiryTimeout(SmppConstants.DEFAULT_REQUEST_EXPIRY_TIMEOUT);
            configuration.setDefaultWindowMonitorInterval(SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL);

            configuration.setDefaultWindowSize(SmppConstants.DEFAULT_WINDOW_SIZE);

            configuration.setDefaultWindowWaitTimeout(SmppConstants.DEFAULT_WINDOW_WAIT_TIMEOUT);
            configuration.setDefaultSessionCountersEnabled(true);

            configuration.setJmxEnabled(false);

            this.defaultSmppServer = new DefaultSmppServer(configuration, new DefaultSmppServerHandler(this), executor, monitorExecutor);
            try {
                this.defaultSmppServer.start();
            } catch (SmppChannelException e1) {
                this.addMessage("Failure to start a defaultSmppServer", e1.toString());
                return;
            }

            enableStart(false);
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

            this.addMessage("SMPP Server has been successfully started", "");

        }
	}

	public void stop() {
		this.addMessage("Trying to stop a session", "");

		this.doStop();
	}

	public void doStop() {
        if (this.session0 != null) {
            this.session0.unbind(5000);
            this.session0.destroy();
            this.session0 = null;
        }
        if (this.defaultSmppServer != null) {
            this.defaultSmppServer.stop();
            this.defaultSmppServer.destroy();
            this.defaultSmppServer = null;
        }

		if (clientBootstrap != null) {
			try {
				clientBootstrap.destroy();
				executor.shutdownNow();
				monitorExecutor.shutdownNow();
			} catch (Exception e) {

			}

			clientBootstrap = null;
			executor = null;
			monitorExecutor = null;
		}

		enableStart(true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		this.addMessage("Session has been stopped", "");
	}

	public void enableStart(boolean enabled) {
		this.btStart.setEnabled(enabled);
		this.btStop.setEnabled(!enabled);
	}

	private void refreshState() {
        this.lbState.setText("messageSegmentsSent=" + this.segmentsSent.get() + ", submitMessagesSent="
                + this.messagesSent.get() + ", submitResponsesRcvd=" + this.responsesRcvd.get() + ", messagesRcvd="
                + this.messagesRcvd.get());
	}

	public void setData(SmppSimulatorForm mainForm, SmppSimulatorParameters param) {
		this.param = param;
		this.mainForm = mainForm;

//		this.tm = new javax.swing.Timer(5000, new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				refreshState();
//			}
//		});
//		this.tm.start();
	}

	private JDialog getJDialog() {
		return this;
	}

	private void closingWindow() {
		this.mainForm.testingFormClose();
	}

	public void eventFormClose() {
		this.eventForm = null;
	}
	
	private void openEventWindow() {
		if (this.eventForm != null)
			return;

		this.eventForm = new EventForm(this);
		this.eventForm.setVisible(true);
		setEventMsg();
	}

	private void doStopTimer() {
        if (this.timer != null) {
            for (Timer tm : this.timer) {
                tm.cancel();
            }
            this.timer = null;
        }
	}

	private int threadCount = 10;

	private void startBulkSending() {
        if (this.rbRandomBulkMessages.isSelected()) {
            this.doStopTimer();

            this.timer = new Timer[threadCount];
            for (int i1 = 0; i1 < threadCount; i1++) {
                this.timer[i1] = new Timer();
                this.timer[i1].scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        doSendSmppMessages();
                    }
                }, 1 * 1000, 1 * 1000);
            }

            this.btStartBulk.setEnabled(false);
            this.btStopBulk.setEnabled(true);
        } else {
            this.doStopTimer();
            this.btStartBulk.setEnabled(false);
            this.btStopBulk.setEnabled(true);
            
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    doParsePcapFile();
                }} );
            t.start();
        }
	}

	private void stopBulkSending() {
		this.doStopTimer();

		this.btStartBulk.setEnabled(true);
		this.btStopBulk.setEnabled(false);
	}

	private String bigMessage = "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
	private JTextField tbPcapFileName;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	private JTextField tbPcapPort;

    private AtomicInteger messagesNum = new AtomicInteger();

	private void doSendSmppMessages() {

        Random rand = new Random();

        for (int i1 = 0; i1 < this.param.getBulkMessagePerSecond() / threadCount; i1++) {
            int n = this.param.getBulkDestAddressRangeEnd() - this.param.getBulkDestAddressRangeStart() + 1;
            if (n < 1)
                n = 1;
            int j1 = rand.nextInt(n);
            Integer destAddr = this.param.getBulkDestAddressRangeStart() + j1;
            String destAddrS = destAddr.toString();

            int j2 = rand.nextInt(2);
            int j3 = rand.nextInt(3);
            EncodingType encodingType;
            if (j2 == 0)
                encodingType = EncodingType.GSM7_DCS_0;
            else
                encodingType = EncodingType.UCS2_DCS_8;
            SplittingType splittingType;
            switch (j3) {
            case 0:
                splittingType = SplittingType.DoNotSplit;
                break;
            case 1:
                splittingType = SplittingType.SplitWithParameters_DefaultSegmentLength;
                break;
            default:
                splittingType = SplittingType.SplitWithUdh_DefaultSegmentLength;
                break;
            }

            int j4 = rand.nextInt(5);
            String msg = this.param.getMessageText();
            if (j4 == 0)
                msg = bigMessage;
            msg += " " + ((Integer) messagesNum.incrementAndGet()).toString();

            this.submitMessage(encodingType, 0, msg, splittingType, param.getValidityType(), destAddrS,
                    param.getMessagingMode(), param.getSpecifiedSegmentLength());
        }
	}

    private void doParsePcapFile() {
        try {
            int port = Integer.parseInt(this.tbPcapPort.getText());
            SmppPcapParser smppPcapParser = new SmppPcapParser(this, this.tbPcapFileName.getText(), port);

            smppPcapParser.parse();

        } catch (Throwable e) {
            JOptionPane.showMessageDialog(getJDialog(), "General exception when pcap parsing: " + e.toString());
            e.printStackTrace();
        } finally {
            this.btStartBulk.setEnabled(true);
            this.btStopBulk.setEnabled(false);
        }
    }

    @Override
    public void onNewSmppRequest(BaseSm pdu) throws Exception {
        if (session0 != null) {
            WindowFuture<Integer, PduRequest, PduResponse> future0 = session0.sendRequestPdu(pdu, 10000, false);

            this.messagesSent.incrementAndGet();
        }
    }

    @Override
    public boolean needContinue() {
        if (!this.btStartBulk.isEnabled())
            return true;
        else
            return false;
    }

	public synchronized void addMessage(String msg, String info) {
		
		Date d1 = new Date();
		String s1 = d1.toLocaleString();

		Vector newRow = new Vector();
		newRow.add(s1);
		newRow.add(msg);
		newRow.add(info);
		model.getDataVector().add(0,newRow);

		model.newRowsAdded(new TableModelEvent(model));
	}

}
