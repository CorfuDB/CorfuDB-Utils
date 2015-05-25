package org.corfudb.tests.benchtests;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.corfudb.runtime.CorfuDBRuntime;
import org.corfudb.runtime.OverwriteException;
import org.corfudb.runtime.TrimmedException;
import org.corfudb.runtime.view.IStreamingSequencer;
import org.corfudb.runtime.view.IWriteOnceAddressSpace;
import org.corfudb.util.CorfuDBFactory;
import java.util.Map;

@SuppressWarnings({"rawtypes","unchecked"})
public class AppendTest implements IBenchTest {

     CorfuDBRuntime c;
     IStreamingSequencer s;
     IWriteOnceAddressSpace woas;
     byte[] data;

     public AppendTest() {
     }

     @Override
     public void doSetup(Map<String, Object> args) {
         CorfuDBFactory factory = new CorfuDBFactory(args);
         CorfuDBRuntime c = factory.getRuntime();
         c.startViewManager();
         data = new byte[getPayloadSize(args)];
         s = factory.getStreamingSequencer(c);
         woas = factory.getWriteOnceAddressSpace(c);
         c.waitForViewReady();
     }

     @Override
     public void close() {
         c.close();
     }

     @Override
     public void doRun(Map<String, Object> args, long runNum, MetricRegistry m) {
         Timer t_sequencer = m.timer("Acquire token");
         Timer.Context c_sequencer = t_sequencer.time();
         long token = s.getNext();
         c_sequencer.stop();
         Timer t_logunit = m.timer("Append data");
         Timer.Context c_logunit = t_logunit.time();
         try {
             woas.write(token, data);
         } catch (OverwriteException oe) {
         } catch (TrimmedException te) {
         }
         c_logunit.stop();
     }
 }

