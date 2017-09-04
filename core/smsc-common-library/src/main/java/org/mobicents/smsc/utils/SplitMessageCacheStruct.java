package org.mobicents.smsc.utils;

import java.sql.Timestamp;

/**
 * Created by Stanis?aw Leja on 31.08.17.
 */
public class SplitMessageCacheStruct {
    private Timestamp additionTimestamp;
    private String reference_number;


    public SplitMessageCacheStruct(String reference_number){
        setAdditionTimestamp();
        setReference_number(reference_number);
    }

    private void setAdditionTimestamp() {
        this.additionTimestamp = new Timestamp(System.currentTimeMillis());
    }

    public Timestamp getAdditionTimestamp() {
        return additionTimestamp;
    }

    public long getAdditionDate() {
        return additionTimestamp.getTime();
    }

    public String getReference_number() {
        return reference_number;
    }

    private void setReference_number(String reference_number) {
        this.reference_number = reference_number;
    }


}
