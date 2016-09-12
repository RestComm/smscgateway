package org.mobicents.smsc.slee.services.http.server.tx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by tpalucki on 08.09.16.
 * @author Tomasz Pa?ucki
 */
public class HttpIncomingData {

    private String userId;
    private String msg;

    /**
     * Response format for the request
     * Possible values: String, json
     * Default is String
     */
    private String format;

    /**
     * Optional parameter
     * possible: english, arabic
     * values: UCS-2, UTF-8
     * Default is english
     * */
    private RequestMessageBodyEncoding encoding;
    private String senderId;

//    private int numberOfDest;
//    private int dataCoding;

    private List<String> destAddresses = new ArrayList<String>();

    private String scheduleDeliveryTime;
//    private List<String> destDistributionList = new ArrayList<String>();



    public HttpIncomingData(String userId, String msg, String format, String encodingStr, String senderId, String[] to) throws HttpApiException {
    // checking if mandatory fields are present
        if(isEmptyOrNull(userId) || isEmptyOrNull(msg) || isEmptyOrNull(senderId) || to == null || to.length < 1 || !RequestMessageBodyEncoding.isValid(encodingStr)){
            throw new HttpApiException("Some of the mandatory parameters are not set properly or not valid in the Http Request.");
        }

        this.userId = userId;
        this.msg = msg;
        this.format = format;
        this.senderId = senderId;
        this.destAddresses.addAll(Arrays.asList(to));
        this.encoding = RequestMessageBodyEncoding.fromString(encodingStr);
    }

    private boolean isEmptyOrNull(String toCheck) {
        if(toCheck == null) {
            return true;
        }
        if("".equals(toCheck)){
            return true;
        }
        return false;
    }

    public List<String> getDestAddresses() {
        return destAddresses;
    }

//    public SubmitMultiResponse createResponse() {
//        return null;
//    }

    public String getUserId() {
        return userId;
    }

    public String getMsg() {
        return msg;
    }

    public String getFormat() {
        return format;
    }

    public RequestMessageBodyEncoding getEncoding() {
        return encoding;
    }

    public String getSenderId() {
        return senderId;
    }

    public int getDefaultMsgId() {
//        return defaultMsgId;
        // TODO implement defaultMessageId
        return -1;
    }

//    public int getDataCoding() {
//        return Integer.parseInt(this.encoding);
//    }

    public String getScheduleDeliveryTime() {
        return scheduleDeliveryTime;
    }

    public byte[] getShortMessage() {
        return this.msg.getBytes();
    }
}
