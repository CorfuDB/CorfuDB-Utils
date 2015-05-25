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
public class CorfuHopStreamHello {

    private static final Logger log = LoggerFactory.getLogger(CorfuHopStreamHello.class);
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
        UUID streamID2 = UUID.randomUUID();
        ITimestamp address = null;


        log.info("Stream 1={}", streamID);
        log.info("Stream 2={}", streamID2);
        try (IHopStream s = (IHopStream) factory.getStream(streamID, sequencer, addressSpace)) {
            try (IHopStream s2 = (IHopStream) factory.getStream(streamID2, sequencer, addressSpace)) {
                log.info("Appending hello world into both logs...");
                try {
                    address = s.append("hello world from stream " + streamID.toString());
                    log.info("Successfully appended hello world into log position " + address + ", stream " + streamID.toString());
                    address = s2.append("hello world from stream " + streamID2.toString());
                    log.info("Successfully appended hello world into log position " + address + ", stream " + streamID2.toString());
                } catch (OutOfSpaceException oose) {
                    log.error("Out of space during append!", oose);
                    System.exit(1);
                }

                log.info("Reading back initial entries");
                String sresult = (String) s.readNextObject();
                log.info("Contents were: " + sresult);
                if (!sresult.toString().equals("hello world from stream " + streamID.toString())) {
                    log.error("ASSERT Failed: String did not match!");
                    System.exit(1);
                }
                sresult = (String) s2.readNextObject();
                log.info("Contents were: " + sresult);
                if (!sresult.toString().equals("hello world from stream " + streamID2.toString())) {
                    log.error("ASSERT Failed: String did not match!");
                    System.exit(1);
                }

                log.info("Pulling stream  " + streamID2.toString() + " to stream " + streamID.toString());
                s.pullStream(streamID2);

                log.info("NOT Waiting for pull to complete...(NOT IMPLEMENTED!)");
                System.exit(-1);
                while (true) {
                    //try{
                    //    s2.waitForEpochChange();
                    break;
                    //} catch (InterruptedException ie) {}
                }
                log.info("Pull complete.");

                try {
                    log.info("Appending to outer stream " + streamID.toString());
                    address = s.append("hello world remote from outer stream " + streamID.toString());
                    log.info("Successfully appended hello world into log position " + address + ", stream " + streamID.toString());
                    log.info("Appending to inner stream " + streamID2.toString());
                    address = s2.append("hello world remote from inner stream " + streamID2.toString());
                    log.info("Successfully appended hello world into log position " + address + ", stream " + streamID2.toString());
                } catch (OutOfSpaceException oose) {
                    log.error("Out of space during append!", oose);
                    System.exit(1);
                }

                log.debug("Reading back 2 results from outer stream " + streamID.toString());
                sresult = (String) s.readNextObject();
                log.info("Contents were: " + sresult);
                if (!sresult.toString().equals("hello world remote from outer stream " + streamID.toString())) {
                    log.error("ASSERT Failed: String did not match!");
                    System.exit(1);
                }
                sresult = (String) s.readNextObject();
                log.info("Contents were: " + sresult);
                if (!sresult.toString().equals("hello world remote from inner stream " + streamID2.toString())) {
                    log.error("ASSERT Failed: String did not match!");
                    System.exit(1);
                }

                log.debug("Reading back 2 results from inner stream " + streamID2.toString());
                sresult = (String) s2.readNextObject();
                log.info("Contents were: " + sresult);
                if (!sresult.toString().equals("hello world remote from outer stream " + streamID.toString())) {
                    log.error("ASSERT Failed: String did not match!");
                    System.exit(1);
                }
                sresult = (String) s2.readNextObject();
                log.info("Contents were: " + sresult);
                if (!sresult.toString().equals("hello world remote from inner stream " + streamID2.toString())) {
                    log.error("ASSERT Failed: String did not match!");
                    System.exit(1);
                }

                System.exit(0);
            }
        }
    }
}

