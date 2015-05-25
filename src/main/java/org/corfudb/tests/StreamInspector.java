package org.corfudb.tests;

import org.corfudb.runtime.CorfuDBRuntime;
import org.corfudb.runtime.entries.IStreamEntry;
import org.corfudb.runtime.stream.IStream;
import org.corfudb.runtime.stream.ITimestamp;
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
public class StreamInspector {

    private static final Logger log = LoggerFactory.getLogger(CorfuHello.class);
    public static final String DEFAULT_ADDRESS_SPACE = "WriteOnceAddressSpace";

    /**

    /**
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
        client.startViewManager();

        try (IStream s = factory.getStream(UUID.fromString(args[1]), sequencer, addressSpace))
        {
            while (true)
            {
                IStreamEntry cdse = s.readNextEntry();
                ITimestamp t = cdse.getTimestamp();
                log.info("{} \t {}", t, cdse.getPayload().getClass().toString());
            }
        }

    }
}

