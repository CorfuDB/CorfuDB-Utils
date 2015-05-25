package org.corfudb.tests.benchtests;

import org.corfudb.runtime.CorfuDBRuntime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import com.codahale.metrics.*;
import org.corfudb.runtime.stream.Stream;

@SuppressWarnings({"rawtypes","unchecked"})
public class MultipleStreamReadTest implements IBenchTest {
    CorfuDBRuntime c;
    ArrayList<Stream> sl;
    AtomicLong l;
    byte[] data;

    public MultipleStreamReadTest() {
    }

    ExecutorService exec;

    @Override
    public void doSetup(Map<String, Object> args) {
        c = getRuntime(args);
        c.startViewManager();
        exec = Executors.newFixedThreadPool(getNumThreads(args));
        sl = new ArrayList<Stream>();
        data = new byte[getPayloadSize(args)];
        for (int i = 0; i < getNumStreams(args); i++) {
            sl.add(new Stream(c, UUID.randomUUID(), getNumThreads(args), getStreamAllocationSize(args), exec, false));
        }

        for (long i = 0; i < getNumOperations(args) * 2; i++) {
            try {
                sl.get((int) i % getNumStreams(args)).append(data);
            } catch (Exception e) {
            }
        }
        l = new AtomicLong();
        c.waitForViewReady();
    }

    @Override
    public void close() {
        for (Stream s : sl) {
            s.close();
        }
        exec.shutdownNow();
        c.close();
    }

    @Override
    public void doRun(Map<String, Object> args, long runNum, MetricRegistry m) {
        int streamNum = (int) (runNum % getNumStreams(args));
        Timer t_logunit = m.timer("read data from stream " + streamNum);
        Timer.Context c_logunit = t_logunit.time();
        try {
            sl.get(streamNum).readNext();
            ;
        } catch (Exception e) {
        }
        c_logunit.stop();
    }
}

