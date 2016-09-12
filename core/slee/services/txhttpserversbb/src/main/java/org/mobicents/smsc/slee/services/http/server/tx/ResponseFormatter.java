package org.mobicents.smsc.slee.services.http.server.tx;

/**
 * Created by tpalucki on 08.09.16.
 */
public class ResponseFormatter {

    public static String format(SubmitMultiResponse response, String format){
        // TODO format String according to the Project descirption at https://docs.google.com/document/d/1MxDKpAkKexUxH5gVKzCtpE9TGny8ir-0JbDN9F1BmT0/edit#
        // ust for now returnign dumb response
        return "{\"error\":6,\"Formatting of the response is not implemented yet.\"}";
    }

    private static String formatString(String response){
        return null;
    }

    private static String formatJson(String response) {
        return null;
    }
}
