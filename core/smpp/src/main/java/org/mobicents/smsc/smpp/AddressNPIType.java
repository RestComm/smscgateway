package org.mobicents.smsc.smpp;

public enum AddressNPIType {
			UNKNOWN("0"), ISDN("1"), DATA("3"), TELEX("4"), LANDMOBILE("6"),
			NATIONAL("8"), PRIVATE("9"), ERMES("10"), IP("14"), WAPCLIENTID("18");

		private static final String TYPE_UNKNOWN = "0";
		private static final String TYPE_ISDN = "1";
		private static final String TYPE_TELEX = "3";
		private static final String TYPE_LANDMOBILE = "4";
		private static final String TYPE_NATIONAL = "6";
		private static final String TYPE_PRIVATE = "8";
		private static final String TYPE_ERMES = "9";
		private static final String TYPE_IP = "14";
		private static final String TYPE_WAPCLIENTID = "18";
		
		private String type = null;

		private AddressNPIType(String type) {
			this.type = type;
		}

		public static AddressNPIType getAddressNPIType(String type) {
			if (TYPE_UNKNOWN.equals(type)) {
				return UNKNOWN;
			} else if (TYPE_ISDN.equals(type)) {
				return ISDN;
			} else if (TYPE_TELEX.equals(type)) {
				return TELEX;
			} else if (TYPE_LANDMOBILE.equals(type)) {
				return LANDMOBILE;
			} else if (TYPE_NATIONAL.equals(type)) {
				return NATIONAL;
			} else if (TYPE_PRIVATE.equals(type)) {
				return PRIVATE;
			} else if (TYPE_ERMES.equals(type)) {
				return ERMES;
			} else if (TYPE_IP.equals(type)) {
				return IP;
			} else if (TYPE_WAPCLIENTID.equals(type)) {
				return WAPCLIENTID;
			} else {
				return null;
			}
		}
		
		public String getType() {
			return this.type;
		}
}
