package org.mobicents.protocols.smpp.load.smppp;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class StatsPrinter implements Runnable, Closeable {

    private static final String[] COUNTER_HDRS = {
        "CreatedScenario",
        "FailedScenario",
        "CompletedScenario",
        "ResponseTimeSamples1",
        "ResponseTimeSum1",
        
        /*"onErrorComponent",
        "onRejectComponent",
        "onInvokeTimeout",
        "onProcessUnstructuredSSRequest",
        "onProcessUnstructuredSSResponse",
        "onUnstructuredSSRequest",
        "onUnstructuredSSResponse",
        "onUnstructuredSSNotifyRequest",
        "onUnstructuredSSNotifyResponseIndication",
        "onDialogDelimiter",
        "onDialogRequest",
        "onDialogRequestEricsson",
        "onDialogAccept",
        "onDialogReject",
        "onDialogUserAbort",
        "onDialogProviderAbort",
        "onDialogClose",
        "onDialogNotice",
        "onDialogRelease",
        "onDialogTimeout",
        "onRegisterSSRequest",
        "onRegisterSSResponse",
        "onEraseSSRequest",
        "onEraseSSResponse",
        "onActivateSSRequest",
        "onActivateSSResponse",
        "onDeactivateSSRequest",
        "onDeactivateSSResponse",
        "onInterrogateSSRequest",
        "onInterrogateSSResponse",
        "onGetPasswordRequest",
        "onGetPasswordResponse",
        "onRegisterPasswordRequest",
        "onRegisterPasswordResponse",
        "onUnstructuredSSNotifyResponse",
        "onMAPMessage"*/
    };

    private static final String CSV_SEPARATOR = ";";
    GlobalContext ctx;
    PrintWriter writer;

    public StatsPrinter(GlobalContext ctx) throws FileNotFoundException, UnsupportedEncodingException {
        this.ctx = ctx;
        String csvFilePath = ctx.getProperty("smppp.csvFilePath");
        if (csvFilePath == null) {
            csvFilePath = "smppp-" + System.currentTimeMillis() + ".csv";
        }
        writer = new PrintWriter(csvFilePath, "UTF-8");
        printRow(Arrays.asList(COUNTER_HDRS));
    }

    private void printRow(List columns) {
        StringBuilder csvLine = new StringBuilder();
        for (Object cAux : columns) {
            csvLine.append(cAux);
            csvLine.append(CSV_SEPARATOR);
        }
        writer.println(csvLine.toString());
        writer.flush();
    }

    @Override
    public void run() {
        List columns = new ArrayList(COUNTER_HDRS.length);
        Map<String, AtomicLong> retrieveAndResetCurrentCounters = ctx.retrieveAndResetCurrentCounters();
        for (String counterKey : COUNTER_HDRS) {
            AtomicLong counter = retrieveAndResetCurrentCounters.get(counterKey);
            if (counter != null) {
                columns.add(counter);
            } else {
                columns.add("0");
            }
        }
        printRow(columns);
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

}
