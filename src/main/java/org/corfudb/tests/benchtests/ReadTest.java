package org.corfudb.tests.benchtests;

import org.corfudb.runtime.TrimmedException;
import org.corfudb.runtime.UnwrittenException;
import org.corfudb.runtime.CorfuDBRuntime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import com.codahale.metrics.*;
import org.corfudb.runtime.view.IStreamingSequencer;
import org.corfudb.runtime.view.IWriteOnceAddressSpace;

@SuppressWarnings({"rawtypes","unchecked"})
public class ReadTest implements IBenchTest {

    CorfuDBRuntime c;
    IStreamingSequencer s;
    IWriteOnceAddressSpace woas;
    AtomicLong l;

    public ReadTest() {
    }

    public void doSetup(Map<String, Object> args) {
        c = getRuntime(args);
        c.startViewManager();
        s = getSequencer();
        woas = getAddressSpace();
        l = new AtomicLong();
        c.waitForViewReady();

        byte[] data = new byte[getPayloadSize(args)];

        for (long i = 0; i < getNumOperations(args); i++) {
            try {
                long token = s.getNext();
                woas.write(token, data);
            } catch (Exception ex) {
                log.error("Error writing to location for setup", ex);
            }
        }
    }

    public void close() {
        c.close();
    }

    @Override
    public void doRun(Map<String, Object> args, long runNum, MetricRegistry m) {
        Timer t_logunit = m.timer("Read data");
        Timer.Context c_logunit = t_logunit.time();
        try {
            woas.read(l.getAndIncrement());
        } catch (UnwrittenException oe) {
        } catch (TrimmedException te) {
        }
        c_logunit.stop();
    }
}

