package org.corfudb.tests;

import org.corfudb.runtime.CorfuDBRuntime;
import org.corfudb.runtime.OutOfSpaceException;
import org.corfudb.runtime.stream.ILog;
import org.corfudb.runtime.stream.ITimestamp;
import org.corfudb.runtime.view.IConfigurationMaster;
import org.corfudb.runtime.view.IStreamingSequencer;
import org.corfudb.runtime.view.IWriteOnceAddressSpace;
import org.corfudb.util.CorfuDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by dmalkhi on 1/16/15.
 */
public class CorfuHello {

    private static final Logger log = LoggerFactory.getLogger(CorfuHello.class);
    public static final String DEFAULT_ADDRESS_SPACE = "WriteOnceAddressSpace";

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
        CorfuDBFactory factory = new CorfuDBFactory(opts);
        CorfuDBRuntime client = factory.getRuntime();
        client.startViewManager();
        IWriteOnceAddressSpace addressSpace = factory.getWriteOnceAddressSpace(client);
        IStreamingSequencer sequencer = factory.getStreamingSequencer(client);
        IConfigurationMaster configMaster = factory.getConfigurationMaster(client);
        ILog sl = factory.getLog(sequencer, addressSpace);

        log.info("Appending hello world into log...");
        ITimestamp address = null;
        try {
            address = sl.append("hello world".getBytes());
        } catch (OutOfSpaceException oose) {
            log.error("Out of space during append!", oose);
            System.exit(1);
        }
        log.info("Successfully appended hello world into log position " + address);
        log.info("Reading back entry at address " + address);
        byte[] result = (byte[]) sl.read(address);
        log.info("Readback complete, result size=" + result.length);
        String sresult = new String(result, "UTF-8");
        log.info("Contents were: " + sresult);
        if (!sresult.toString().equals("hello world")) {
            log.error("ASSERT Failed: String did not match!");
            System.exit(1);
        }

        log.info("Successfully completed test!");
        System.exit(0);

    }
}

