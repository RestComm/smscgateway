package org.mobicents.smsc.extension;

import org.jboss.as.controller.AbstractRemoveStepHandler;

class SmscMbeanRemove extends AbstractRemoveStepHandler {

    static final SmscMbeanRemove INSTANCE = new SmscMbeanRemove();

    private SmscMbeanRemove() {
    }
}