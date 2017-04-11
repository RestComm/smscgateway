package org.mobicents.protocols.smpp.load.smppp;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class StatsPrinter implements Runnable, Closeable {

    private static final String[] COUNTER_HDRS = {
        "CreatedScenario",
        "FailedScenario",
        "CompletedScenario" /*"onErrorComponent",
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
        "onMAPMessage"*/};

    private static final String CSV_SEPARATOR = ";";
    GlobalContext ctx;
    PrintWriter writer;
    boolean headerPrinted = false;

    public StatsPrinter(GlobalContext ctx) throws FileNotFoundException, UnsupportedEncodingException {
        this.ctx = ctx;
        String csvFilePath = ctx.getProperty("smppp.csvFilePath");
        if (csvFilePath == null) {
            csvFilePath = "smppp-" + System.currentTimeMillis() + ".csv";
        }
        writer = new PrintWriter(csvFilePath, "UTF-8");
    }

    private void printHeader(Map<String, AtomicLong> retrieveAndResetCurrentCounters) {
        List columns = new ArrayList(COUNTER_HDRS.length);
        columns.addAll(Arrays.asList(COUNTER_HDRS));
        for (int i = 1; i < 5; i++) {
            String rSamplesKey = "ResponseTimeSamples" + i;
            if (retrieveAndResetCurrentCounters.containsKey(rSamplesKey)) {
                columns.add("ResponseTime" + i);
            }
        }
        printRow(columns);
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

        if (!headerPrinted) {
            printHeader(retrieveAndResetCurrentCounters);
            headerPrinted = true;
        }

        for (String counterKey : COUNTER_HDRS) {
            AtomicLong counter = retrieveAndResetCurrentCounters.get(counterKey);
            if (counter != null) {
                columns.add(counter);
            } else {
                columns.add("0");
            }
        }
        for (int i = 1; i < 5; i++) {
            String rSamplesKey = "ResponseTimeSamples" + i;
            if (retrieveAndResetCurrentCounters.containsKey(rSamplesKey)) {
                String rSumKey = "ResponseTimeSum" + i;
                AtomicLong samples = retrieveAndResetCurrentCounters.get(rSamplesKey);
                AtomicLong sum = retrieveAndResetCurrentCounters.get(rSumKey);
                double avg = sum.get() / samples.get();
                columns.add(String.format(Locale.US, "%.2f", avg));
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
