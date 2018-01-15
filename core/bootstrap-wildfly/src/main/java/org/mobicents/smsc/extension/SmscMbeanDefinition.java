package org.mobicents.smsc.extension;

import org.jboss.as.controller.*;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author sergey.povarnin@telestax.com
 */
public class SmscMbeanDefinition extends SimpleResourceDefinition {

    public enum Element {
        // must be first
        UNKNOWN(null),
        NAME("name"),
        TYPE("type");

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
                if (name != null)
                    map.put(name, element);
            }
            MAP = map;
        }

        public static Element of(final String localName) {
            final Element element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }

    }

    protected static final SimpleAttributeDefinition NAME_ATTR =
            new SimpleAttributeDefinitionBuilder(Element.NAME.localName(), ModelType.STRING)
                    .setXmlName(Element.NAME.localName())
                    .setAllowNull(true) // todo should be false, but 'add' won't validate then
                    .build();

    protected static final SimpleAttributeDefinition TYPE_ATTR =
            new SimpleAttributeDefinitionBuilder(Element.TYPE.localName(), ModelType.STRING)
                    .setXmlName(Element.TYPE.localName())
                    .setAllowNull(true) // todo should be false, but 'add' won't validate then
                    .build();

    public static final String MBEAN = "mbean";
    public static final PathElement MBEAN_PATH = PathElement.pathElement(MBEAN);
    public static final SmscMbeanDefinition INSTANCE = new SmscMbeanDefinition();

    protected static final SimpleAttributeDefinition[] MBEAN_ATTRIBUTES = {
            //NAME, // name is read-only
            TYPE_ATTR,
    };

    private SmscMbeanDefinition() {
        super(MBEAN_PATH,
                SmscExtension.getResourceDescriptionResolver(MBEAN),
                SmscMbeanAdd.INSTANCE,
                SmscMbeanRemove.INSTANCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(SmscMbeanPropertyDefinition.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration mbeans) {
        mbeans.registerReadOnlyAttribute(NAME_ATTR, null);
        for (SimpleAttributeDefinition def : MBEAN_ATTRIBUTES) {
            mbeans.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
        }
    }

}
