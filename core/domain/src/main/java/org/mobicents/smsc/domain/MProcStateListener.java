package org.mobicents.smsc.domain;

public interface MProcStateListener {
    void mprocCreated(int mprocId);
    void mprocDestroyed(int mprocId);
    void mprocModified(int mprocId);

}