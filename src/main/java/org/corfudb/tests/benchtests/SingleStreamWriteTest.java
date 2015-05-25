package org.corfudb.tests.benchtests;

import org.corfudb.runtime.CorfuDBRuntime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import com.codahale.metrics.*;
import org.corfudb.runtime.OutOfSpaceException;
import org.corfudb.runtime.stream.Stream;

@SuppressWarnings({"rawtypes","unchecked"})
public class SingleStreamWriteTest implements IBenchTest {

    CorfuDBRuntime c;
    Stream s;
    AtomicLong l;
    byte[] data;

    public SingleStreamWriteTest() {
    }

    @Override
    public void doSetup(Map<String, Object> args) {
        c = getRuntime(args);
        c.startViewManager();
        s = new Stream(c, UUID.randomUUID(), 1, getSingleStreamAllocationSize(args), false);
        l = new AtomicLong();
        data = new byte[getPayloadSize(args)];
        c.waitForViewReady();
    }

    @Override
    public void close() {
        s.close();
        c.close();
    }

    @Override
    public void doRun(Map<String, Object> args, long runNum, MetricRegistry m) {
        Timer t_logunit = m.timer("append data to stream");
        Timer.Context c_logunit = t_logunit.time();
        try {
            s.append(data);
        } catch (OutOfSpaceException oos) {
        }
        c_logunit.stop();
    }
}

