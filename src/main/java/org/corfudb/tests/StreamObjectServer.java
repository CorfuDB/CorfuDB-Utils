package org.corfudb.tests;

import com.esotericsoftware.kryonet.Server;
import org.corfudb.runtime.CorfuDBRuntime;
import org.corfudb.runtime.entries.IBundleEntry;
import org.corfudb.runtime.entries.IStreamEntry;
import org.corfudb.runtime.stream.IStream;
import org.corfudb.runtime.view.IConfigurationMaster;
import org.corfudb.runtime.view.IStreamingSequencer;
import org.corfudb.runtime.view.IWriteOnceAddressSpace;
import org.corfudb.util.CorfuDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StreamObjectServer {

    private static final Logger log = LoggerFactory.getLogger(StreamObjectServer.class);
    public static final String DEFAULT_ADDRESS_SPACE = "WriteOnceAddressSpace";

    public static class StreamObjectWrapper implements Serializable {
        public int serverNum;

        public StreamObjectWrapper(int serverNum) {
            this.serverNum = serverNum;
        }
    }

    public static class StreamClientObjectWrapper implements Serializable {
        public int serverNum;

        public StreamClientObjectWrapper(int serverNum) {
            this.serverNum = serverNum;
        }
    }

    public static Server server;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        String masteraddress = "http://localhost:8002/corfu";
        Map<String, Object> opts = new HashMap();
        opts.put("--master", masteraddress);
        opts.put("--address-space", DEFAULT_ADDRESS_SPACE);
        CorfuDBFactory factory = new CorfuDBFactory(opts);
        CorfuDBRuntime client = factory.getRuntime();
        client.startViewManager();
        IWriteOnceAddressSpace addressSpace = factory.getWriteOnceAddressSpace(client);
        IStreamingSequencer sequencer = factory.getStreamingSequencer(client);

        final int serverNum = Integer.parseInt(args[0]);
        IStream s = factory.getStream(new UUID(0, serverNum), sequencer, addressSpace);
        while (true) {
            IStreamEntry cdse = s.readNextEntry();
            if (cdse instanceof IBundleEntry) {
                IBundleEntry b = (IBundleEntry) cdse;
                b.writeSlot(null);
            }
        }
    }
}

