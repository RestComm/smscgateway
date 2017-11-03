package org.mobicents.smsc.extension;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

import java.util.HashMap;
import java.util.Map;

public class SmscMbeanPropertyDefinition extends SimpleResourceDefinition {

    public enum Element {
        UNKNOWN(null),
        NAME("name"),
        TYPE("type"),
        VALUE("value");

        private final String name;

        Element(final String name) {
            this.name = name;
        }

        public String localName() {
            return name;
        }

        private static final Map<String, Element> MAP;

        static {
            final Map<String, Element> map = new HashMap<String, Element>();
            for (Element element : values()) {
                final String name = element.localName();
                if (name != null) map.put(name, element);
            }
            MAP = map;
        }

        public static Element of(final String localName) {
            final Element element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }

    }

    public static final SimpleAttributeDefinition NAME_ATTR = new SimpleAttributeDefinition(
            Element.NAME.localName(), ModelType.STRING, true);
    public static final SimpleAttributeDefinition TYPE_ATTR = new SimpleAttributeDefinition(
            Element.TYPE.localName(), ModelType.STRING, true);
    public static final SimpleAttributeDefinition VALUE_ATTR = new SimpleAttributeDefinition(
            Element.VALUE.localName(), ModelType.STRING, true);

    public static final String PROPERTY = "property";
    public static final PathElement PROPERTY_PATH = PathElement.pathElement(PROPERTY);
    public static final SmscMbeanPropertyDefinition INSTANCE = new SmscMbeanPropertyDefinition();

    protected static final SimpleAttributeDefinition[] PROPERTY_ATTRIBUTES = {
            //NAME_ATTR, // name is read-only
            TYPE_ATTR,
            VALUE_ATTR
    };

    private SmscMbeanPropertyDefinition() {
        super(PROPERTY_PATH,
                SmscExtension.getResourceDescriptionResolver(SmscMbeanDefinition.MBEAN + "." + PROPERTY),
                SmscMbeanPropertyAdd.INSTANCE,
                SmscMbeanPropertyRemove.INSTANCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration properties) {
        //super.registerAttributes(resourceRegistration);
        properties.registerReadOnlyAttribute(NAME_ATTR, null);
        for (SimpleAttributeDefinition def : PROPERTY_ATTRIBUTES) {
            properties.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
        }
    }

}