package org.mobicents.protocols.smpp.load.smppp;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class RemoteControl extends Thread implements Closeable {

    private static final String COMMAND_SEPARATOR = " ";
    private static final String SUCCESS = "200";
    private static final String NOT_FOUND = "404";
    private final DatagramSocket socket;
    private final GlobalContext ctx;

    private final Map<String, RemoteCmd> commandMap = new HashMap();

    public RemoteControl(GlobalContext ctx) throws IOException {
        super("RemoteControlTH");
        this.ctx = ctx;
        socket = new DatagramSocket(ctx.getIntegerProp("smppp.remoteControlPort"));
        commandMap.put("stop", new StopCmd());
        commandMap.put("setRate", new SetRateCmd());
        
    }

    @Override
    public void close() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }

    interface RemoteCmd {

        public String execute(String[] cmdSplit);
    }

    class StopCmd implements RemoteCmd {
        public String execute(String[] cmdSplit) {
            ctx.fsm.fire(GlobalEvent.STOP, ctx);
            return SUCCESS;
        }
    }
    
    class SetRateCmd implements RemoteCmd {
        @Override
        public String execute(String[] cmdSplit) {
            ctx.setProperty("smppp.caps", cmdSplit[1]);
            ctx.fsm.fire(GlobalEvent.RATE_CHANGED, ctx);
            return SUCCESS;
        }
    }
    

    private String executeCommand(String remoteCommand) {
        ctx.logger.info("Received remote command:" + remoteCommand);        
        String response = NOT_FOUND;
        String[] cmdSplit = remoteCommand.split(COMMAND_SEPARATOR);
        if (cmdSplit != null && cmdSplit.length > 0
                && commandMap.containsKey(cmdSplit[0])) {
            RemoteCmd cmd = commandMap.get(cmdSplit[0]);
            ctx.logger.info("Command recognized:" + cmd.getClass().getSimpleName());            
            response = cmd.execute(cmdSplit);
        } else {
            ctx.logger.info("Command not recognized.");
        }
        ctx.logger.info("Remote command result:" + response);        
        return response;
    }

    @Override
    public void run() {
        while (!ctx.fsm.getCurrentState().equals(GlobalState.STOPPED)) {
            try {
                byte[] buf = new byte[256];

                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                
                String remoteCommand = new String(buf, "UTF-8");
                String dString = executeCommand(remoteCommand);
                buf = dString.getBytes();

                // send the response to the client at "address" and "port"
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);
            } catch (IOException e) {
                ctx.logger.error("while processing remote command", e);
            }
        }
    }
}
