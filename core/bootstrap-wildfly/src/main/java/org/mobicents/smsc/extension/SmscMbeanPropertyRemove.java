package org.mobicents.smsc.extension;

import org.jboss.as.controller.AbstractRemoveStepHandler;

class SmscMbeanPropertyRemove extends AbstractRemoveStepHandler {

    public static final SmscMbeanPropertyRemove INSTANCE = new SmscMbeanPropertyRemove();

    private SmscMbeanPropertyRemove() {
    }
}