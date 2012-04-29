
package org.mobicents.smsc.smpp;

public enum AddressTONType {
			UNKNOWN("0"), INTERNATIONAL("1"), NATIONAL("2"), NETWORKSPECIFIC("3"), SUBSCRIBERNUMBER("4"),
			ALPHANUMBERIC("5"), ABBREVIATED("6");

		private static final int TYPE_UNKNOWN = 0;
		private static final int TYPE_INTERNATIONAL = 1;
		private static final int TYPE_NATIONAL = 2;
		private static final int TYPE_NETWORKSPECIFIC = 3;
		private static final int TYPE_SUBSCRIBERNUMBER = 4;
		private static final int TYPE_ALPHANUMBERIC = 5;
		private static final int TYPE_ABBREVIATED = 6;
		
		private String type = null;

		private AddressTONType(String type) {
			this.type = type;
		}

		public static AddressTONType getAddressTONType(String type) {
			try {
				int intTon = Integer.parseInt(type);
				if (TYPE_UNKNOWN == intTon) {
					return UNKNOWN;
				} else if (TYPE_INTERNATIONAL == intTon) {
					return INTERNATIONAL;
				} else if (TYPE_NATIONAL == intTon) {
					return NATIONAL;
				} else if (TYPE_NETWORKSPECIFIC == intTon) {
					return NETWORKSPECIFIC;
				} else if (TYPE_SUBSCRIBERNUMBER == intTon) {
					return SUBSCRIBERNUMBER;
				}else if (TYPE_ALPHANUMBERIC == intTon) {
					return ALPHANUMBERIC;
				}else if (TYPE_ABBREVIATED == intTon) {
					return ABBREVIATED;
				} else {
					return null;
				}
			} catch (NumberFormatException e) {
				return null;
			}
		}
		
		public String getType() {
			return this.type;
		}
}
