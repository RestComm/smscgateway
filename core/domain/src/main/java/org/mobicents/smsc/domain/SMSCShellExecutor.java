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

package org.mobicents.smsc.domain;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.mobicents.smsc.smpp.SmppEncoding;
import org.mobicents.smsc.smpp.SmppOamMessages;
import org.mobicents.ss7.management.console.ShellExecutor;

/**
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public class SMSCShellExecutor implements ShellExecutor {

	private static final Logger logger = Logger.getLogger(SMSCShellExecutor.class);

	private SmscManagement smscManagement;

    private static SmscPropertiesManagement smscPropertiesManagement;

	public SMSCShellExecutor() {

	}

	public void start() throws Exception {
		smscPropertiesManagement = SmscPropertiesManagement.getInstance(this.getSmscManagement().getName());
		if (logger.isInfoEnabled()) {
			logger.info("Started SMSCShellExecutor " + this.getSmscManagement().getName());
		}
	}

	/**
	 * @return the m3uaManagement
	 */
	public SmscManagement getSmscManagement() {
		return smscManagement;
	}

	/**
	 * @param m3uaManagement
	 *            the m3uaManagement to set
	 */
	public void setSmscManagement(SmscManagement smscManagement) {
		this.smscManagement = smscManagement;
	}

	private String showSip() {
		SipManagement sipManagement = SipManagement.getInstance();
		List<Sip> sips = sipManagement.getSips();
		if (sips.size() == 0) {
			return SMSCOAMMessages.NO_SIP_DEFINED_YET;
		}

		StringBuffer sb = new StringBuffer();
		for (Sip sip : sips) {
			sb.append(SMSCOAMMessages.NEW_LINE);
			sip.show(sb);
		}
		return sb.toString();
	}

	/**
	 * Command is smsc sip modify name cluster-name <clusterName> host <ip> port
	 * <port> routing-ton <routing address ton> routing-npi <routing address
	 * npi> routing-range <routing address range> 
	 * 
	 * @param args
	 * @return
	 * @throws Exception
	 */
	private String modifySip(String[] args) throws Exception {
		if (args.length < 6 || args.length > 22) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		// modify existing SIP
		String name = args[3];
		if (name == null) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		SipManagement sipManagement = SipManagement.getInstance();
		Sip sip = sipManagement.getSipByName(name);
		if (sip == null) {
			return String.format(SMSCOAMMessages.SIP_NOT_FOUND, name);
		}

		int count = 4;
		String command;

		boolean success = false;
		while (count < (args.length - 1) && ((command = args[count++]) != null)) {
			String value = args[count++];
			if (command.equals("cluster-name")) {
				sip.setClusterName(value);
				success = true;
			} else if (command.equals("host")) {
				sip.setHost(value);
				success = true;
            } else if (command.equals("port")) {
                sip.setPort(Integer.parseInt(value));
                success = true;
			} else if (command.equals("routing-ton")) {
				sip.setRoutingTon(Integer.parseInt(value));
				success = true;
			} else if (command.equals("routing-npi")) {
				sip.setRoutingNpi(Integer.parseInt(value));
				success = true;
			} else if (command.equals("routing-range")) {
				sip.setRoutingAddressRange(value);
				success = true;
			}
		}// while

		if (!success) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		return String.format(SMSCOAMMessages.SIP_MODIFY_SUCCESS, name);
	}

	private String executeSmsc(String[] args) {
		try {
			if (args.length < 2 || args.length > 50) {
				// any command will have atleast 3 args
				return SMSCOAMMessages.INVALID_COMMAND;
			}

			if (args[1] == null) {
				return SMSCOAMMessages.INVALID_COMMAND;
			}

            if (args[1].equals("sip")) {
                String rasCmd = args[2];
				if (rasCmd == null) {
					return SMSCOAMMessages.INVALID_COMMAND;
				}

				if (rasCmd.equals("modify")) {
					return this.modifySip(args);
				} else if (rasCmd.equals("show")) {
					return this.showSip();
				}

				return SMSCOAMMessages.INVALID_COMMAND;

			} else if (args[1].equals("set")) {
				return this.manageSet(args);
			} else if (args[1].equals("get")) {
				return this.manageGet(args);
			} else if (args[1].equals("remove")) {
				return this.manageRemove(args);
			}

            return SmppOamMessages.INVALID_COMMAND;
		} catch (Throwable e) {
			logger.error(String.format("Error while executing comand %s", Arrays.toString(args)), e);
			return e.toString();
		}
	}

	private String manageSet(String[] options) throws Exception {
		if (options.length < 4) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String parName = options[2].toLowerCase();
        try {
            if (parName.equals("defaultton")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setDefaultTon(val);
			} else if (parName.equals("defaultnpi")) {
				int val = Integer.parseInt(options[3]);
				smscPropertiesManagement.setDefaultNpi(val);
            } else if (parName.equals("smppencodingforgsm7")) {
                String s1 = options[3].toLowerCase();
                if (s1.equals("utf8")) {
                    smscPropertiesManagement.setSmppEncodingForGsm7(SmppEncoding.Utf8);
                } else if (s1.equals("unicode")) {
                    smscPropertiesManagement.setSmppEncodingForGsm7(SmppEncoding.Unicode);
                } else {
                    return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, "SmppEncodingForGsm7 value",
                            "UTF8 or UNICODE are possible");
                }
            } else if (parName.equals("smppencodingforucs2")) {
                String s1 = options[3].toLowerCase();
                if (s1.equals("utf8")) {
                    smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Utf8);
                } else if (s1.equals("unicode")) {
                    smscPropertiesManagement.setSmppEncodingForUCS2(SmppEncoding.Unicode);
                } else {
                    return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, "SmppEncodingForUCS2 value",
                            "UTF8 or UNICODE are possible");
                }
			} else if (parName.equals("esmedefaultcluster")) {
				smscPropertiesManagement.setEsmeDefaultClusterName(options[3]);
            } else if (parName.equals("generatereceiptcdr")) {
                smscPropertiesManagement.setGenerateReceiptCdr(Boolean.parseBoolean(options[3]));
            } else if (parName.equals("receiptsdisabling")) {
                smscPropertiesManagement.setReceiptsDisabling(Boolean.parseBoolean(options[3]));
            } else if (parName.equals("generatecdr")) {
                int val = Integer.parseInt(options[3]);
                smscPropertiesManagement.setGenerateCdrInt(val);

            } else if (parName.equals("deliverypause")) {
                boolean val = Boolean.parseBoolean(options[3]);
                smscPropertiesManagement.setDeliveryPause(val);
			} else {
				return SMSCOAMMessages.INVALID_COMMAND;
			}
		} catch (IllegalArgumentException e) {
			return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, parName, e.getMessage());
		}

		return SMSCOAMMessages.PARAMETER_SUCCESSFULLY_SET;
	}

	private String manageRemove(String[] options) throws Exception {
		if (options.length < 3) {
			return SMSCOAMMessages.INVALID_COMMAND;
		}

		String parName = options[2].toLowerCase();
		try {
			if (parName.equals("esmedefaultcluster")) {
				smscPropertiesManagement.setEsmeDefaultClusterName(null);

			} else {
				return SMSCOAMMessages.INVALID_COMMAND;
			}
		} catch (IllegalArgumentException e) {
			return String.format(SMSCOAMMessages.ILLEGAL_ARGUMENT, parName, e.getMessage());
		}

		return SMSCOAMMessages.PARAMETER_SUCCESSFULLY_REMOVED;
	}

	private String manageGet(String[] options) throws Exception {
		if (options.length == 3) {
			String parName = options[2].toLowerCase();

			StringBuilder sb = new StringBuilder();
			sb.append(options[2]);
			sb.append(" = ");
			if (parName.equals("defaultton")) {
				sb.append(smscPropertiesManagement.getDefaultTon());
			} else if (parName.equals("defaultnpi")) {
				sb.append(smscPropertiesManagement.getDefaultNpi());
            } else if (parName.equals("smppencodingforgsm7")) {
                sb.append(smscPropertiesManagement.getSmppEncodingForGsm7());
            } else if (parName.equals("smppencodingforucs2")) {
                sb.append(smscPropertiesManagement.getSmppEncodingForUCS2());
			} else if (parName.equals("esmedefaultcluster")) {
				sb.append(smscPropertiesManagement.getEsmeDefaultClusterName());
            } else if (parName.equals("generatereceiptcdr")) {
                sb.append(smscPropertiesManagement.getGenerateReceiptCdr());
            } else if (parName.equals("receiptsdisabling")) {
                sb.append(smscPropertiesManagement.getReceiptsDisabling());
            } else if (parName.equals("receiptsdisabling")) {
                sb.append(smscPropertiesManagement.getReceiptsDisabling());

            } else if (parName.equals("deliverypause")) {
                sb.append(smscPropertiesManagement.isDeliveryPause());
			} else {
				return SMSCOAMMessages.INVALID_COMMAND;
			}

			return sb.toString();
		} else {
			StringBuilder sb = new StringBuilder();

			sb.append("defaultton = ");
			sb.append(smscPropertiesManagement.getDefaultTon());
			sb.append("\n");

			sb.append("defaultnpi = ");
			sb.append(smscPropertiesManagement.getDefaultNpi());
			sb.append("\n");

            sb.append("smppencodingforgsm7 = ");
            sb.append(smscPropertiesManagement.getSmppEncodingForGsm7());
            sb.append("\n");

            sb.append("smppencodingforucs2 = ");
            sb.append(smscPropertiesManagement.getSmppEncodingForUCS2());
            sb.append("\n");

			sb.append("esmedefaultcluster = ");
			sb.append(smscPropertiesManagement.getEsmeDefaultClusterName());
			sb.append("\n");

            sb.append("generatereceiptcdr = ");
            sb.append(smscPropertiesManagement.getGenerateReceiptCdr());
            sb.append("\n");

            sb.append("receiptsdisabling = ");
            sb.append(smscPropertiesManagement.getReceiptsDisabling());
            sb.append("\n");

            sb.append("generatecdr = ");
            sb.append(smscPropertiesManagement.getGenerateCdr().getValue());
            sb.append("\n");

            sb.append("deliverypause = ");
            sb.append(smscPropertiesManagement.isDeliveryPause());
            sb.append("\n");

			return sb.toString();
		}
	}

	public String execute(String[] args) {
		if (args[0].equals("smsc")) {
			return this.executeSmsc(args);
		}
		return SMSCOAMMessages.INVALID_COMMAND;
	}

	@Override
	public boolean handles(String command) {
		return "smsc".equals(command);
	}

//	public static void main(String[] args) throws Exception {
//		String command = "smsc mapcache get 1234567";
//		SMSCShellExecutor exec = new SMSCShellExecutor();
//		exec.getMapVersionCache(command.split(" "));
//	}

}

