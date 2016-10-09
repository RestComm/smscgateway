package org.mobicents.smsc.slee.services.http.server.tx;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.testng.annotations.Test;

public class SendPostRequest {

    @Test(groups = { "SendPostRequest" })
    public void testSendPostRequest() throws Exception {
        this.send();
    }

    private void send2() throws Exception {
        int destAddress = 6666;
        int addrCount = 2;

        String urlParameters = createBody(destAddress, addrCount);
        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;
        String request = "http://localhost:8080/restcomm/sendSms";
        URL url = new URL(request);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        conn.setUseCaches(false);
        
        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        // try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
        // wr.write(postData);
        // }

        int responseCode = conn.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Post parameters : " + urlParameters);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result
        System.out.println(response.toString());

    }

    private void send() throws Exception {
        String host = "localhost";
        int port = 8080;
        int destAddress = 7000;
        int addrCount = 50000;

        InetAddress ipAddress = InetAddress.getByName(host);
        Socket socket = new Socket(ipAddress, port);

        InputStream sin = socket.getInputStream();
        OutputStream sout = socket.getOutputStream();

        String s1 = createBody(destAddress, addrCount);
        Charset utf8 = Charset.forName("utf-8");
        byte[] bufx = s1.getBytes(utf8);

        StringBuilder sb = new StringBuilder();
        sb.append("POST /restcomm/sendSms HTTP/1.1\r\n");
        sb.append("Content-Length: ");
        sb.append(bufx.length);
        sb.append("\r\n");
        sb.append("Content-Type: application/x-www-form-urlencoded\r\n");
        sb.append("Content-Encoding: utf-8\r\n");
        sb.append("Host: ");
        sb.append(host);
        sb.append(":");
        sb.append(port);
        sb.append("\r\n");

        // if (cookie != null) {
        // sb.append("Cookie: JSESSIONID=");
        // sb.append(cookie);
        // sb.append("\n");
        // }
        // sb.append("Connection: Keep-Alive\n");

        sb.append("\r\n");
        sb.append(s1);

        byte[] buf = sb.toString().getBytes(utf8);
        sout.write(buf);
        sout.flush();

        StringBuilder resp = new StringBuilder();
        for (int i0 = 0; i0 < 50; i0++) {
            Thread.sleep(100);
            if (sin.available() > 0) {
                int i1 = sin.read(buf);
                byte[] buf2 = new byte[i1];
                System.arraycopy(buf, 0, buf2, 0, i1);
                String s = new String(buf2, utf8);
                resp.append(s);
            }
        }
        
        socket.close();

        int i2 = 0;
        i2++;
    }

    private String createBody(int destAddress, int addrCount) {
        StringBuilder sb = new StringBuilder();

        sb.append("userid=user1&password=password&msg=Thisisamessage012&sender=1234&to=");
        boolean firstMsg = true;
        for (int i1 = 0; i1 < addrCount; i1++) {
            if (firstMsg)
                firstMsg = false;
            else
                sb.append(",");
            sb.append(destAddress + i1);
        }

        String s1 = sb.toString();

        return s1;

        // Charset utf8 = Charset.forName("utf-8");
        // byte[] bufx = s1.getBytes(utf8);

    }
}
