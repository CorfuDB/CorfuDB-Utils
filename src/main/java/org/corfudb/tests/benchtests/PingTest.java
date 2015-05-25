package org.corfudb.tests.benchtests;

import org.corfudb.runtime.view.CorfuDBViewSegment;
import org.corfudb.runtime.protocols.IServerProtocol;
import org.corfudb.runtime.CorfuDBRuntime;
import java.util.Map;
import java.util.List;
import com.codahale.metrics.*;


@SuppressWarnings({"rawtypes","unchecked"})
public class PingTest implements IBenchTest {

    public PingTest() {
    }

    CorfuDBRuntime c;

    @Override
    public void doSetup(Map<String, Object> args) {
        c = getRuntime(args);
        c.startViewManager();
        c.waitForViewReady();
    }

    @Override
    public void close() {
        c.close();
    }

    @Override
    public void doRun(Map<String, Object> args, long runNum, MetricRegistry m) {
        for (IServerProtocol s : c.getView().getSequencers()) {
            Timer t_sequencer = m.timer("ping sequencer: " + s.getFullString());
            Timer.Context c_sequencer = t_sequencer.time();
            s.ping();
            c_sequencer.stop();
        }
        for (CorfuDBViewSegment vs : c.getView().getSegments()) {
            for (List<IServerProtocol> lsp : vs.getGroups()) {
                for (IServerProtocol lu : lsp) {
                    Timer t_logunit = m.timer("ping logunit: " + lu.getFullString());
                    Timer.Context c_logunit = t_logunit.time();
                    lu.ping();
                    c_logunit.stop();
                }
            }
        }
    }
}

