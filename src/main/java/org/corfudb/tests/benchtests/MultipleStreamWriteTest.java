package org.corfudb.tests.benchtests;

import org.corfudb.runtime.CorfuDBRuntime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import com.codahale.metrics.*;
import org.corfudb.runtime.OutOfSpaceException;
import org.corfudb.runtime.stream.Stream;

@SuppressWarnings({"rawtypes","unchecked"})
public class MultipleStreamWriteTest implements IBenchTest {

    CorfuDBRuntime c;
    ArrayList<Stream> sl;
    AtomicLong l;
    byte[] data;
    ExecutorService exec;

    public MultipleStreamWriteTest() {
    }

    @Override
    public void doSetup(Map<String, Object> args) {
        c = getRuntime(args);
        c.startViewManager();
        exec = Executors.newFixedThreadPool(getNumThreads(args));
        sl = new ArrayList<Stream>();
        for (int i = 0; i < getNumStreams(args); i++) {
            sl.add(new Stream(c, UUID.randomUUID(), 1, getStreamAllocationSize(args), exec, false));
        }
        l = new AtomicLong();
        data = new byte[getPayloadSize(args)];
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
        Timer t_logunit = m.timer("append data to stream " + streamNum);
        Timer.Context c_logunit = t_logunit.time();
        try {
            sl.get(streamNum).append(data);
        } catch (OutOfSpaceException oos) {
        }
        c_logunit.stop();
    }
}

