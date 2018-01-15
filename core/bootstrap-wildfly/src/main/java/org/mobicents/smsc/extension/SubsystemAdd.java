package org.mobicents.smsc.extension;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.mobicents.smsc.service.SmscService;
import org.mobicents.ss7.service.SS7ExtensionService;
import org.mobicents.ss7.service.SS7ServiceInterface;
import org.restcomm.smpp.SmppInterfaceVersionType;
import org.restcomm.smpp.service.SmppService;
import org.restcomm.smpp.service.SmppServiceInterface;

import javax.management.MBeanServer;
import java.util.List;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class SubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final SubsystemAdd INSTANCE = new SubsystemAdd();

    private final Logger log = Logger.getLogger(SubsystemAdd.class);

    private SubsystemAdd() {
    }

    /** {@inheritDoc} */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        log.info("Populating the model");
        model.setEmptyObject();
    }

    /** {@inheritDoc} */
    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        SmscService service = SmscService.INSTANCE;
        service.setModel(fullModel);

        ServiceName name = SmscService.getServiceName();
        ServiceController<SmscService> controller = context.getServiceTarget()
                .addService(name, service)
                .addDependency(PathManagerService.SERVICE_NAME, PathManager.class, service.getPathManagerInjector())
                .addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, service.getMbeanServer())
                .addDependency(SS7ExtensionService.getServiceName(), SS7ServiceInterface.class, service.getSS7Service())
                .addDependency(SmppService.getServiceName(), SmppServiceInterface.class, service.getSmppService())
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
        newControllers.add(controller);

    }
}
