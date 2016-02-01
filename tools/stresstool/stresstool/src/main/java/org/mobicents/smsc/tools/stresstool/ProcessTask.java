package org.mobicents.smsc.tools.stresstool;

public interface ProcessTask {

    boolean isReady();

    String getResults();

    void terminate();

}
