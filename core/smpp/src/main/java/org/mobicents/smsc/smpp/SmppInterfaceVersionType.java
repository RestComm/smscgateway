package org.mobicents.smsc.smpp;

public enum SmppInterfaceVersionType {
			SMPP33("3.3"), SMPP34("3.4"), SMPP50("5.0");

		private static final String TYPE_SMPP33 = "3.3";
		private static final String TYPE_SMPP34 = "3.4";
		private static final String TYPE_SMPP50 = "5.0";

		private String type = null;

		private SmppInterfaceVersionType(String type) {
			this.type = type;
		}

		public static SmppInterfaceVersionType getInterfaceVersionType(String type) {
			if (TYPE_SMPP33.equals(type)) {
				return SMPP33;
			} else if (TYPE_SMPP34.equals(type)) {
				return SMPP34;
			} else if (TYPE_SMPP50.equals(type)) {
				return SMPP50;
			} else {
				return null;
			}
		}
		
		public String getType() {
			return this.type;
		}
}
