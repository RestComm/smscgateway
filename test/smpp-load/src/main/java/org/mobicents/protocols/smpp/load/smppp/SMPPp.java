package org.mobicents.protocols.smpp.load.smppp;

import com.cloudhopper.smpp.type.Address;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;


public class SMPPp {

    final GlobalContext ctx;

    public SMPPp(Properties props) {
        initLog4J(props);
        
        ctx = new GlobalContext(props);
        
        //init beanutils and converters
        ConvertUtilsBean cub = new ConvertUtilsBean();
        cub.register(new AddressConverter(ctx) , Address.class);

        
        
        BeanUtilsBean.setInstance(new BeanUtilsBean(cub, new PropertyUtilsBean()));        

        //add shutdown hook to ensure greaceful shutdown
        Runtime.getRuntime().addShutdownHook(new ReleaseOnShutdown(ctx));

        ctx.fsm.fire(GlobalEvent.START, ctx);

    }

    private void initLog4J(Properties props) {

        try {
            String log4jProps = props.getProperty("smppp.log4jPropsFilePath");
            if (log4jProps != null) {
                InputStream inStreamLog4j = new FileInputStream(new File(log4jProps));
                Properties propertiesLog4j = new Properties();
                propertiesLog4j.load(inStreamLog4j);
                PropertyConfigurator.configure(propertiesLog4j);
            } else {
                BasicConfigurator.configure();
            }
            String lf = props.getProperty("smppp.logFilePath");
            if (lf != null) {
                ctx.logger.addAppender(new FileAppender(new SimpleLayout(), lf));
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    //http://tldp.org/LDP/abs/html/exitcodes.html
    private static final int ERROR_STATUS = 64;

    public static void main(String[] args) {
        SMPPp smppp = null;
        int status = 0;
        try {
            Properties props = new Properties();
            FileInputStream iStream = new FileInputStream(new File(args[0]));
            props.load(iStream);
            smppp = new SMPPp(props);
            smppp.waitForTrafficToComplete();
        } catch (Exception ex) {
            ex.printStackTrace();
            status = ERROR_STATUS;
        }
        System.exit(status);
    }

    public void waitForTrafficToComplete() throws InterruptedException {
        //wait for all dialogs to complete
        ctx.executor.awaitTermination(ctx.getIntegerProp("smppp.awaitTermination"), TimeUnit.SECONDS);
        ctx.fsm.fire(GlobalEvent.STOP, ctx);
        ctx.executor.awaitTermination(ctx.getIntegerProp("smppp.trafficGrantPeriod"), TimeUnit.SECONDS);
    }
}
