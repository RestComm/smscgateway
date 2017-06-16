package org.mobicents.smsc.mproc;

public enum RejectType {
	 NONE, DEFAULT, UNEXPECTED_DATA_VALUE, SYSTEM_FAILURE, THROTTLING, FACILITY_NOT_SUPPORTED;
	 
	 public static RejectType parse(String s) {
		 RejectType res = null;
		 if (s != null)
			 res = RejectType.valueOf(s);
		 		 
		 return res;
	}
}

