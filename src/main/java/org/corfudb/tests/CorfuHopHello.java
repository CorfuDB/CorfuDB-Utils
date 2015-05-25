package org.corfudb.tests;

import org.corfudb.runtime.CorfuDBRuntime;
import org.corfudb.runtime.OutOfSpaceException;
import org.corfudb.runtime.stream.*;
import org.corfudb.runtime.view.IConfigurationMaster;
import org.corfudb.runtime.view.IStreamingSequencer;
import org.corfudb.runtime.view.IWriteOnceAddressSpace;
import org.corfudb.util.CorfuDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by dmalkhi on 1/16/15.
 */
public class CorfuHopHello {

    private static final Logger log = LoggerFactory.getLogger(CorfuHello.class);
    public static final String DEFAULT_ADDRESS_SPACE = "WriteOnceAddressSpace";
    public static final String DEFAULT_STREAM_IMPL = "HopStream";

    /**
     * /**
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {

        String masteraddress = null;

        if (args.length >= 1) {
            masteraddress = args[0]; // TODO check arg.length
        } else {
            // throw new Exception("must provide master http address"); // TODO
            masteraddress = "http://localhost:8002/corfu";
        }

        final int numthreads = 1;
        Map<String, Object> opts = new HashMap();
        opts.put("--master", masteraddress);
        opts.put("--address-space", DEFAULT_ADDRESS_SPACE);
        opts.put("--stream-impl", DEFAULT_STREAM_IMPL);
        CorfuDBFactory factory = new CorfuDBFactory(opts);
        CorfuDBRuntime client = factory.getRuntime();
        client.startViewManager();
        IWriteOnceAddressSpace addressSpace = factory.getWriteOnceAddressSpace(client);
        IStreamingSequencer sequencer = factory.getStreamingSequencer(client);
        IConfigurationMaster configMaster = factory.getConfigurationMaster(client);

        UUID streamID = UUID.randomUUID();
        try (IHopStream s = (IHopStream) factory.getStream(streamID, sequencer, addressSpace)) {
            log.info("Appending hello world into local log...");
            log.info("Local log address = " + client.getView().getUUID().toString());
            UUID remoteLog = null;

            System.out.println("Exiting: configMaster.getAllLogs currently unimplemented!");
            System.exit(-1);

            /*
        for (UUID log : configMaster.getAllLogs().keySet())
        {
            if (!log.equals(client.getView().getUUID()))
            {
                remoteLog = log;
                break;
            }
        }
        */

            if (remoteLog == null) {
                log.error("No remote log could be found!");
                return;
            }
            log.info("Remote log address = " + remoteLog.toString());
            ITimestamp address = null;
            try {
                address = s.append("hello world from stream " + streamID.toString());
            } catch (OutOfSpaceException oose) {
                log.error("Out of space during append!", oose);
                System.exit(1);
            }
            log.info("Successfully appended hello world into log position " + address + ", stream " + streamID.toString());
            log.info("Reading back entry at address " + address);
            String sresult = (String) s.readNextObject();
            log.info("Contents were: " + sresult);
            if (!sresult.toString().equals("hello world from stream " + streamID.toString())) {
                log.error("ASSERT Failed: String did not match!");
                System.exit(1);
            }

            log.info("Hopping to remote log " + remoteLog.toString());
            s.hopLog(remoteLog);

            log.info("Waiting for hop to complete...");
            while (true) {
                //try{
                System.out.println("Exiting: IHopStream.waitForEpochChange currently unimplemented!");
                System.exit(-1);

                // s.waitForEpochChange();
                break;
                // } catch (InterruptedException ie) {}
            }
            log.info("Hop complete.");

            log.info("Appending to remote log");
            try {
                address = s.append("hello world remote from stream " + streamID.toString());
            } catch (OutOfSpaceException oose) {
                log.error("Out of space during append!", oose);
                System.exit(1);
            }
            log.info("Successfully appended hello world remote into log position " + address + ", stream " + streamID.toString());

            sresult = (String) s.readNextObject();
            log.info("Contents were: " + sresult);
            if (!sresult.toString().equals("hello world remote from stream " + streamID.toString())) {
                log.error("ASSERT Failed: String did not match!");
                System.exit(1);
            }

            log.info("Hopping back to home log " + client.getView().getUUID().toString());
            s.hopLog(client.getView().getUUID());

            log.info("Waiting for hop to complete...");
            while (true) {
                //try{
                System.out.println("Exiting: IHopStream.waitForEpochChange currently unimplemented!");
                System.exit(-1);
                //s.waitForEpochChange();
                break;
                //} catch (InterruptedException ie) {}
            }
            log.info("Hop complete.");

            log.info("Appending to home log");
            try {
                address = s.append("hello world back home from stream " + streamID.toString());
            } catch (OutOfSpaceException oose) {
                log.error("Out of space during append!", oose);
                System.exit(1);
            }
            log.info("Successfully appended hello world back home into log position " + address + ", stream " + streamID.toString());

            sresult = (String) s.readNextObject();
            log.info("Contents were: " + sresult);
            if (!sresult.toString().equals("hello world back home from stream " + streamID.toString())) {
                log.error("ASSERT Failed: String did not match!");
                System.exit(1);
            }

            System.exit(0);
        }
    }
}

