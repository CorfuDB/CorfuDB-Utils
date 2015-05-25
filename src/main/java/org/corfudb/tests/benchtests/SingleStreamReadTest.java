 package org.corfudb.tests.benchtests;

 import org.corfudb.runtime.CorfuDBRuntime;
 import java.util.Map;
 import java.util.UUID;
 import java.util.concurrent.atomic.AtomicLong;
 import com.codahale.metrics.*;
 import org.corfudb.runtime.stream.Stream;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

@SuppressWarnings({"rawtypes","unchecked"})
public class SingleStreamReadTest implements IBenchTest {

    static final Logger log = LoggerFactory.getLogger(SingleStreamReadTest.class);
    CorfuDBRuntime c;
    Stream s;
    AtomicLong l;
    byte[] data;

    public SingleStreamReadTest() {
    }

    @Override
    public void doSetup(Map<String, Object> args) {
        c = getRuntime(args);
        c.startViewManager();
        s = new Stream(c, UUID.randomUUID(), 1000, getSingleStreamAllocationSize(args), false);
        l = new AtomicLong();
        data = new byte[getPayloadSize(args)];
        for (long i = 0; i < getNumOperations(args) * 2; i++) {
            try {
                s.append(data);
            } catch (Exception e) {
                log.debug("Error appending log entry", e);
            }
        }
        c.waitForViewReady();
    }

    @Override
    public void close() {
        s.close();
        c.close();
    }

    @Override
    public void doRun(Map<String, Object> args, long runNum, MetricRegistry m) {
        Timer t_logunit = m.timer("read data from stream");
        Timer.Context c_logunit = t_logunit.time();
        try {
            s.readNext();
        } catch (Exception e) {
        }
        c_logunit.stop();
    }
}

