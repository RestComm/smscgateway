package org.mobicents.smsc.extension;

import static org.mobicents.smsc.extension.SmscMbeanPropertyDefinition.PROPERTY_ATTRIBUTES;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;

class SmscMbeanPropertyAdd extends AbstractAddStepHandler {

    public static final SmscMbeanPropertyAdd INSTANCE = new SmscMbeanPropertyAdd();

    private SmscMbeanPropertyAdd() {
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        SmscMbeanPropertyDefinition.NAME_ATTR.validateAndSet(operation, model);
        for (SimpleAttributeDefinition def : PROPERTY_ATTRIBUTES) {
            def.validateAndSet(operation, model);
        }
    }
}