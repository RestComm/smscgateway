package org.mobicents.smsc.mproc;

public enum HttpCode {
	LOCAL_RESPONSE_1(1), LOCAL_RESPONSE_2(2), LOCAL_RESPONSE_3(3), LOCAL_RESPONSE_4(4), LOCAL_RESPONSE_5(5);
	
	private final int value;

	private HttpCode(int value) {
        this.value = value;
    }
	
    public int getCode() {
        return value;
    }
}
