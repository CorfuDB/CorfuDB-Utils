/**
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.corfudb.tests;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import gnu.getopt.Getopt;
import org.apache.zookeeper.CreateMode;
import org.corfudb.runtime.CorfuDBRuntime;
import org.corfudb.runtime.collections.*;
import org.corfudb.runtime.smr.*;
import org.corfudb.runtime.smr.legacy.*;
import org.corfudb.runtime.stream.IStream;
import org.corfudb.runtime.view.ISequencer;
import org.corfudb.runtime.view.IStreamingSequencer;
import org.corfudb.runtime.view.IWriteOnceAddressSpace;
import org.corfudb.util.CorfuDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Tester code for the CorfuDB runtime stack
*
*
*/

public class CorfuDBTester
{

    static Logger dbglog = LoggerFactory.getLogger(CorfuDBTester.class);

    static void print_usage()
    {
        System.out.println("usage: java CorfuDBTester");
        System.out.println("\t-m masternode");
        System.out.println("\t[-a testtype] (0==TXTest|1==LinMapTest|2==StreamTest|3==MultiClientTXTest|4==LinCounterTest)");
        System.out.println("\t[-A str-testtype] for those who find the numbers difficult to keep track of");
        System.out.println("\t[-Z serialiZe in memory log]");
        System.out.println("\t[-o cOmpress in memory log (implies serialize)]");
        System.out.println("\t[-t number of threads]");
        System.out.println("\t[-n number of ops]");
        System.out.println("\t[-k number of keys used in list tests]");
        System.out.println("\t[-l number of lists used in list tests]");
        System.out.println("\t[-p rpcport]");
        System.out.println("\t[-e expernum (for MultiClientTXTest)]");
        System.out.println("\t[-c numclients (for MultiClientTXTest)]");
        System.out.println("\t[-k numkeys (for TXTest)]");
        System.out.println("\t[-v verbose mode...]");
        System.out.println("\t[-x extreme debug mode (requires -v)]");
        System.out.println("\t[-r read write pct (double)]");
        System.out.println("\t[-T test case [functional|multifunctional|concurrent|tx]]\n");
        System.out.println("\t[-i init fs trace path]\n");
        System.out.println("\t[-w fs wkld trace path]\n");
        System.out.println("\t[-L crash output log to use in recovery (BTreeFS)]\n");
        System.out.println("\t[-C B-Tree class name]\n");
        System.out.println("\t[-z crash/recover op number]\n");
        System.out.println("\t[-S collect latency stats]\n");
        System.out.println("\t[-h synthesized directory structure target depth]\n");
        System.out.println("\t[-h synthesized directory structure fanout]\n");
        System.out.println("\t[-N non-transactional fs]\n");
        System.out.println("\t[-M B-tree M parameter (max entries per node--must be even!) fs]\n");

//        if(dbglog instanceof SimpleLogger)
//            System.out.println("using SimpleLogger: run with -Dorg.slf4j.simpleLogger.defaultLogLevel=debug to " +
//                    "enable debug printouts");
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
        final int TXTEST=0;
        final int LINTEST=1;
        final int STREAMTEST=2;
        final int MULTICLIENTTXTEST=3;
        final int LINCTRTEST=4;
        final int REMOBJTEST=5;
        final int TXLOGICALLIST=6;
        final int TXLINKEDLIST=7;
        final int TXDOUBLYLINKEDLIST=8;
        final int LINZK=9;
        final int TXLOGICALBTREE = 10;
        final int TXPHYSICALBTREE = 11;
        final int BTREEFS_BASIC  = 13;
        final int BTREEFS_RECORD = 14;
        final int BTREEFS_PLAYBACK = 15;
        final int BTREEFS_CRASH = 16;
        final int BTREEFS_RECOVER = 17;
        final String DEFAULT_ADDRESS_SPACE = "WriteOnceAddressSpace";
        final String DEFAULT_STREAM_IMPL = "SimpleStream";
        final String DUMMYSTREAMIMPL = "SimpleStream";
        final String HOPSTREAMIMPL = "HopStream";
        final String MEMORYSTREAM = "MemoryStream";
        String streamimpl = DEFAULT_STREAM_IMPL;

        int c;
        int numclients = 2;
        int expernum = 1; //used by the barrier code
        String strArg;
        int numthreads = 1;
        int numops = 1000;
        int numkeys = 100;
        int numlists = 2;
        int testnum = 0;
        int rpcport = 9090;
        String masternode = null;
        boolean verbose = false;
        double rwpct = 0.25;
        String testCase = "functional";
        String strInitPath = null;
        String strWkldPath = null;
        String strTestType = "";
        String strBTreeClass = "CDBLogicalBTree";
        int nCrashRecoverOp = 0;
        int nTargetFSDepth = 5;
        int nTargetFSFanout = 8;
        String strCrashLog = "out.txt";
        boolean transactionalFS = true;
        boolean serialize = false;
        boolean compress = false;


        if(args.length==0)
        {
            print_usage();
            return;
        }

        Getopt g = new Getopt("CorfuDBTester", args, "a:m:t:n:p:e:k:c:l:r:vxT:s:i:w:A:L:C:z:Sh:f:NM:Zo");
        while ((c = g.getopt()) != -1)
        {
            switch(c)
            {
                case 'M':
                    CDBAbstractBTree.M = Integer.parseInt(g.getOptarg());
                    break;
                case 'N':
                    transactionalFS = false;
                    break;
                case 'h':
                    nTargetFSDepth = Integer.parseInt(g.getOptarg());
                    break;
                case 'f':
                    nTargetFSFanout = Integer.parseInt(g.getOptarg());
                    break;
                case 'S':
                    CDBAbstractBTree.s_collectLatencyStats = true;
                    break;
                case 'z':
                    nCrashRecoverOp = Integer.parseInt(g.getOptarg());
                    break;
                case 'C':
                    strBTreeClass = g.getOptarg();
                    break;
                case 'L':
                    strCrashLog = g.getOptarg();
                    break;
                case 'A':
                    strTestType = g.getOptarg();
                    if(strTestType.compareToIgnoreCase("crash") == 0)
                        testnum = BTREEFS_CRASH;
                    else if(strTestType.compareToIgnoreCase("recover") == 0)
                        testnum = BTREEFS_RECOVER;
                    else if(strTestType.compareToIgnoreCase("record") == 0)
                        testnum = BTREEFS_RECORD;
                    else if(strTestType.compareToIgnoreCase("playback") == 0)
                        testnum = BTREEFS_PLAYBACK;
                    break;
                case 'i':
                    strInitPath = g.getOptarg();
                    break;
                case 'w':
                    strWkldPath = g.getOptarg();
                    break;
                case 's':
                    strArg = g.getOptarg();
                    System.out.println("streamimpl = "+ strArg);
                    streamimpl = strArg;
                case 'T':
                    testCase = g.getOptarg();
                    break;
                case 'x':
                    org.corfudb.tests.TXListTester.extremeDebug = true;
                    BTreeTester.extremeDebug = true;
                    BTreeTester.trackOps = true;
                    CDBPhysicalBTree.extremeDebug = true;
                    break;
                case 'v':
                    verbose = true;
                    break;
                case 'a':
                    strArg = g.getOptarg();
                    System.out.println("testtype = "+ strArg);
                    testnum = Integer.parseInt(strArg);
                    break;
                case 'r':
                    strArg = g.getOptarg();
                    System.out.println("rwpct = "+ strArg);
                    rwpct = Double.parseDouble(strArg);
                    break;
                case 'm':
                    masternode = g.getOptarg();
                    masternode = masternode.trim();
                    System.out.println("master = " + masternode);
                    break;
                case 't':
                    strArg = g.getOptarg();
                    System.out.println("numthreads = "+ strArg);
                    numthreads = Integer.parseInt(strArg);
                    if(numthreads == 1)
                        CDBAbstractBTree.s_singleThreadOptimization = true;
                    break;
                case 'n':
                    strArg = g.getOptarg();
                    System.out.println("numops = "+ strArg);
                    numops = Integer.parseInt(strArg);
                    break;
                case 'k':
                    strArg = g.getOptarg();
                    System.out.println("numkeys = "+ strArg);
                    numkeys = Integer.parseInt(strArg);
                    break;
                case 'l':
                    strArg = g.getOptarg();
                    System.out.println("numlists = "+ strArg);
                    numlists = Integer.parseInt(strArg);
                    break;
                case 'p':
                    strArg = g.getOptarg();
                    System.out.println("rpcport = "+ strArg);
                    rpcport = Integer.parseInt(strArg);
                    break;
                case 'c':
                    strArg = g.getOptarg();
                    System.out.println("numbarrier = " + strArg);
                    numclients = Integer.parseInt(strArg);
                    break;
                case 'e':
                    strArg = g.getOptarg();
                    System.out.println("expernum = " + strArg);
                    expernum = Integer.parseInt(strArg);
                    break;
                case 'Z':
                    System.out.println("Serialize in memory log");
                    serialize = true;
                    break;
                case 'o':
                    System.out.println("Compress in memory log (implies serialize)");
                    compress = true;
                    break;
                default:
                    System.out.print("getopt() returned " + c + "\n");
            }
        }

        if(masternode == null)
            throw new Exception("must provide master http address using -m flag");
        if(numthreads < 1)
            throw new Exception("need at least one thread!");
        if(numops < 1)
            throw new Exception("need at least one op!");



        String rpchostname;

        try
        {
            rpchostname = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
            throw new RuntimeException(e);
        }

        HashMap<String, Object> opts = new HashMap();
        opts.put("--master", masternode);
        opts.put("--address-space", DEFAULT_ADDRESS_SPACE);
        opts.put("--stream-impl", streamimpl);
        CorfuDBFactory factory = new CorfuDBFactory(opts);
        CorfuDBRuntime crfa = null;

        if (streamimpl != MEMORYSTREAM)
        {
            crfa = factory.getRuntime();
            crfa.startViewManager();
            crfa.waitForViewReady();
        }

        Thread[] threads = new Thread[numthreads];

        long starttime = System.currentTimeMillis();

        AbstractRuntime TR = null;
        DirectoryService DS = null;
        CorfuDBCounter barrier=null;
        IStreamingSequencer seq = factory.getStreamingSequencer(crfa);
        IWriteOnceAddressSpace woas = factory.getWriteOnceAddressSpace(crfa);
        IStreamFactory sf = factory.getStreamFactory(seq, woas);

        if(testnum==LINTEST)
        {
            Map<Integer, Integer> cob1 = null;
            int numpartitions = 0;
            if(numpartitions==0)
            {
                TR = new SimpleRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport);
                cob1 = new CorfuDBMap<Integer, Integer>(TR, DirectoryService.getUniqueID(sf));
            }
            else
            {
                TR = new TXRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport);
                CDBLinkedList<UUID> partitionlist = new CDBLinkedList<>(TR, sf, DirectoryService.getUniqueID(sf));
                while(true)
                {
                    TR.BeginTX();
                    if(partitionlist.size()!=numpartitions)
                    {
                        for (int i = 0; i < numpartitions; i++)
                            partitionlist.add(DirectoryService.getUniqueID(sf));
                    }
                    if(TR.EndTX()) break;
                }
                cob1 = new PartitionedMap<Integer, Integer>(partitionlist, TR);
            }
            for (int i = 0; i < numthreads; i++)
            {
                //linearizable tester
                threads[i] = new Thread(new MapTesterThread(cob1));
                threads[i].start();
            }
            for(int i=0;i<numthreads;i++)
                threads[i].join();
            System.out.println("Test succeeded!");
        }
        else if(testnum==LINCTRTEST)
        {
            TR = new SimpleRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport);
            CorfuDBCounter ctr1 = new CorfuDBCounter(TR, DirectoryService.getUniqueID(sf));
            for (int i = 0; i < numthreads; i++)
            {
                //linearizable tester
                threads[i] = new Thread(new CtrTesterThread(ctr1));
                threads[i].start();
            }
            for(int i=0;i<numthreads;i++)
                threads[i].join();
            System.out.println("Test succeeded!");
        }
        else if(testnum==TXTEST)
        {
            int numpartitions = 0;
            boolean perthreadstack = true;
            CorfuDBMap<Integer, Integer> cob1 = null;
            CorfuDBMap<Integer, Integer> cob2 = null;
            if(!perthreadstack)
            {
                TR = new TXRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport);

                DS = new DirectoryService(TR);
                cob1 = new CorfuDBMap(TR, DS.nameToStreamID("testmap1"));
                cob2 = new CorfuDBMap(TR, DS.nameToStreamID("testmap2"));
            }
            TXTesterThread firsttester = null;
            for (int i = 0; i < numthreads; i++)
            {
                TXTesterThread ttt = null;
                //transactional tester
                if(perthreadstack)
                    ttt = new TXTesterThread(numkeys, numops, sf, rpchostname, rpcport+i, numpartitions);
                else
                    ttt = new TXTesterThread(cob1, cob2, TR, numkeys, numops);
                if(i==0) firsttester = ttt;
                threads[i] = new Thread(ttt);
            }
            for(int i=0;i<numthreads;i++)
                threads[i].start();
            for(int i=0;i<numthreads;i++)
                threads[i].join();
            System.out.println("Test done! Checking consistency...");
            if(firsttester.check_consistency())
                System.out.println("Consistency check passed --- test successful!");
            else
            {
                System.out.println("Consistency check failed!");
                System.out.println(firsttester.map1);
                System.out.println(firsttester.map2);
            }
            System.out.println(TR);
        }
        else if(testnum==STREAMTEST)
        {
            throw new RuntimeException("STREAMTEST no longer implemented due to deprecation of previous Stream interface.");
            /*
            IStream sb = sf.newStream(new UUID(0, 1234));

            //trim the stream to get rid of entries from previous tests
            //sb.prefixTrim(sb.checkTail()); //todo: turning off, trim not yet implemented at log level
            for(int i=0;i<numthreads;i++)
            {
                threads[i] = new Thread(new StreamTester(sb, numops));
                threads[i].start();
            }
            for(int i=0;i<numthreads;i++)
                threads[i].join();
                */
        }
        else if(testnum==TXLOGICALLIST) {
            TR = new TXRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport, true);
            TXListTester.<Integer, CDBLogicalList<Integer>>runListTest(
                    TR, sf, numthreads, numlists, numops, numkeys, rwpct, "CDBLogicalList", verbose);
        }
        else if(testnum==TXLINKEDLIST) {
            TR = new TXRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport, true);
            TXListTester.<Integer, CDBLinkedList<Integer>>runListTest(
                    TR, sf, numthreads, numlists, numops, numkeys, rwpct, "CDBLinkedList", verbose);
        }
        else if(testnum==TXDOUBLYLINKEDLIST) {
            TR = new TXRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport, true);
            TXListTester.<Integer, CDBDoublyLinkedList<Integer>>runListTest(
                    TR, sf, numthreads, numlists, numops, numkeys, rwpct, "CDBDoublyLinkedList", verbose);
        }
        else if(testnum==TXLOGICALBTREE) {
            TR = new TXRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport, true);
            BTreeTester.<String, String, CDBLogicalBTree<String, String>>runTest(
                    TR, sf, numthreads, numlists, numops, numkeys, rwpct, "CDBLogicalBTree", testCase, verbose);
        }
        else if(testnum==TXPHYSICALBTREE) {
            TR = new TXRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport, true);
            BTreeTester.<String, String, CDBPhysicalBTree<String, String>>runTest(
                    TR, sf, numthreads, numlists, numops, numkeys, rwpct, "CDBPhysicalBTree", testCase, verbose);
        }
        else if(testnum==BTREEFS_BASIC) {
            TR = new TXRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport, true);
            BTreeFS.fstestBasic(TR, sf, strBTreeClass, transactionalFS);
        }
        else if(testnum== BTREEFS_RECORD) {
            TR = new TXRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport, true);
            BTreeFS.fstestRecord(TR, sf, strBTreeClass, transactionalFS, nTargetFSDepth, nTargetFSFanout, numops, strInitPath, strWkldPath);
        }
        else if(testnum==BTREEFS_PLAYBACK) {
            TR = new TXRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport, true);
            BTreeFS.fstestPlayback(TR, sf, strBTreeClass, strInitPath, strWkldPath,transactionalFS);
        }
        else if(testnum == BTREEFS_CRASH) {
            TR = new TXRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport, true);
            BTreeFS.fstestCrash(TR, sf, strBTreeClass, transactionalFS, strInitPath, strWkldPath, nCrashRecoverOp);
        }
        else if(testnum == BTREEFS_RECOVER) {
            TR = new TXRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport, true);
            BTreeFS.fstestRecover(TR, sf, strBTreeClass, transactionalFS, strInitPath, strWkldPath, strCrashLog, nCrashRecoverOp);
        }
        else if(testnum==REMOBJTEST)
        {
            //create two maps, one local, one remote
            TR = new TXRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport);

            DS = new DirectoryService(TR);
            CorfuDBMap<Integer, Integer> cob1 = new CorfuDBMap(TR, DS.nameToStreamID("testmap" + (rpcport%2)));
            CorfuDBMap<Integer, Integer> cob2 = new CorfuDBMap(TR, DS.nameToStreamID("testmap" + ((rpcport+1)%2)), true);
            System.out.println("local map = testmap" + (rpcport%2) + " " + cob1.getID());
            System.out.println("remote map = testmap" + ((rpcport+1)%2) + " " + cob2.getID());


            System.out.println("sleeping");
            Thread.sleep(10000);
            System.out.println("woke up");

            cob1.put(100, 55);
            System.out.println(cob1.size());
            System.out.println(cob2.size());
            Thread.sleep(5000);
            System.out.println("Test succeeded!");
        }
        else if(testnum==MULTICLIENTTXTEST)
        {
            //barrier code
            TR = new TXRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport);
            DS = new DirectoryService(TR);
            CorfuDBMap<Integer, Integer> cob1 = new CorfuDBMap(TR, DS.nameToStreamID("testmap" + (rpcport%2)));
            CorfuDBMap<Integer, Integer> cob2 = new CorfuDBMap(TR, DS.nameToStreamID("testmap" + ((rpcport+1)%2)), true);

            barrier = new CorfuDBCounter(TR, DS.nameToStreamID("barrier" + expernum));
            if(barrier.read()>numclients)
            {
                System.out.println("This experiment number has been used before! Use a new number for the -e flag, or check" +
                        " that the -c flag correctly specifies the number of clients.");
                System.exit(0);
            }
            barrier.increment();
            long lastprinttime = System.currentTimeMillis();
            int curnumclients = 0;
            while((curnumclients = barrier.read()) < numclients)
            {
                if(System.currentTimeMillis()-lastprinttime>3000)
                {
                    System.out.println("current number of clients in barrier " + expernum + " = " + curnumclients);
                    lastprinttime = System.currentTimeMillis();
                }
            }
            dbglog.debug("Barrier reached; starting test...");

            System.out.println("local map = testmap" + (rpcport%2) + " " + cob1.getID());
            System.out.println("remote map = testmap" + ((rpcport+1)%2)+ " " + cob2.getID());

            for (int i = 0; i < numthreads; i++)
            {
                //transactional tester
                threads[i] = new Thread(new TXTesterThread(cob1, cob2, TR, numkeys, numops));
                threads[i].start();
            }
            for(int i=0;i<numthreads;i++)
                threads[i].join();
            barrier.increment();
            while(barrier.read() < 2*numclients)
                cob1.size(); //this ensures that we're still processing txints and issuing partial decisions to help other nodes
                             //need a cleaner way to ensure this
            dbglog.debug("second barrier reached; checking consistency...");
            System.out.println("Checking consistency...");
            TXTesterThread tx = new TXTesterThread(cob1, cob2, TR, numkeys, numops);
            if(tx.check_consistency())
                System.out.println("Consistency check passed --- test successful!");
            else
                System.out.println("Consistency check failed!");
            System.out.println(TR);
            barrier.increment();
            while(barrier.read() < 3*numclients);
            dbglog.debug("third barrier reached; test done");
            System.out.println("Test done!");
        }
        else if(testnum==LINZK)
        {
            TR = new TXRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport);
            DS = new DirectoryService(TR);
            IZooKeeper zk = new CorfuDBZK(TR, DS.nameToStreamID("zookeeper"), false, null);
            if(zk.exists("/xyz",  true)==null)
                System.out.println(zk.create("/xyz", "ABCD".getBytes(), null, CreateMode.PERSISTENT));
            else
                System.out.println("already exists");
            System.out.println(zk.exists("/xyz",  true));
            zk.setData("/xyz", "AAA".getBytes(), -1);
            System.out.println(new String(zk.getData("/xyz", false, null)));
            zk.delete("/xyz", -1);
            System.out.println(zk.exists("/xyz",  true));
            System.out.println(zk.create("/xyz", "ABCD".getBytes(), null, CreateMode.PERSISTENT));
            Thread.sleep(1000);
            int numzops = 100;
            //synchronous testing
            for(int i=0;i<numzops;i++)
                zk.create("/xyz/" + i, "AAA".getBytes(), null, CreateMode.PERSISTENT);
            for(int i=0;i<numzops;i++)
                zk.setData("/xyz/" + i, "BBB".getBytes(), -1);
            for(int i=0;i<numzops;i++)
                zk.exists("/xyz/" + i, false);
            for(int i=0;i<numzops;i++)
                zk.getData("/xyz/" + i, false, null);
            for(int i=0;i<numzops;i++)
                zk.getChildren("/xyz/" + i, false);
            //atomic rename
            int txretries = 0;
            int moves = 0;
            for(int i=0;i<numzops;i++)
            {
                while(true)
                {
                    TR.BeginTX();
                    String src = "/xyz/" + (int)((Math.random()*(double)numzops));
                    String dest = "/xyz/" + (int)((Math.random()*(double)numzops*2));
                    if(zk.exists(src, false)!=null && zk.exists(dest, false)==null)
                    {
                        moves++;
                        byte[] data = zk.getData(src, false, null);
                        zk.delete(src, -1);
                        zk.create(dest, data, null, CreateMode.PERSISTENT); //take mode from old item?
                    }
                    if (TR.EndTX())
                    {
                        break;
                    }
                    else
                        txretries++;
                }
            }
            System.out.println("atomic renames: " + moves + " moves, " + txretries + " TX retries.");
            for(int i=0;i<numzops;i++)
                zk.delete("/xyz/" + i, -1);
            for(int i=0;i<numzops;i++)
                System.out.println("Sequential --- " + zk.create("/xyzaaa", "qwerty".getBytes(), null, CreateMode.PERSISTENT_SEQUENTIAL));
            List<String> childnodes = zk.getChildren("/", false);
            Iterator<String> it = childnodes.iterator();
            while(it.hasNext())
                zk.delete(it.next(), -1);
            zk.create("/abcd", "QWE".getBytes(), null, CreateMode.PERSISTENT);
        }

        System.out.println("Test done in " + (System.currentTimeMillis()-starttime));

        System.exit(0);

    }
}


/**
 * This tester implements a bipartite graph over two maps, where an edge exists between integers map1:X and map2:Y
 * iff map1:X == Y and map2:Y==X. The code uses transactions across the maps to add and remove edges,
 * ensuring that the graph is always in a consistent state.
 */
class TXTesterThread implements Runnable
{
    private static Logger dbglog = LoggerFactory.getLogger(TXTesterThread.class);

    AbstractRuntime cr;
    Map<Integer, Integer> map1;
    Map<Integer, Integer> map2;
    int numkeys;
    int numops;

    //creates thread-specific stack
    public TXTesterThread(int tnumkeys, int tnumops, IStreamFactory sf, String rpchostname, int rpcport, int numpartitions)
    {
        TXRuntime TR = new TXRuntime(sf, DirectoryService.getUniqueID(sf), rpchostname, rpcport);

        DirectoryService DS = new DirectoryService(TR);
        if(numpartitions==0)
        {
            map1 = new CorfuDBMap(TR, DS.nameToStreamID("testmap1"));
            map2 = new CorfuDBMap(TR, DS.nameToStreamID("testmap2"));
        }
        else
        {
            throw new RuntimeException("unimplemented");
            /*
            Map<Integer, Integer> partmaparray1[] = new Map[numpartitions];
            for (int i = 0; i < partmaparray1.length; i++)
                partmaparray1[i] = new CorfuDBMap<Integer, Integer>(TR, DS.nameToStreamID("testmap1-part" + i));
            map1 = new PartitionedMap<>(partmaparray1);

            Map<Integer, Integer> partmaparray2[] = new Map[numpartitions];
            for (int i = 0; i < partmaparray2.length; i++)
                partmaparray2[i] = new CorfuDBMap<Integer, Integer>(TR, DS.nameToStreamID("testmap2-part" + i));
            map2 = new PartitionedMap<>(partmaparray2);*/
        }

        cr = TR;
        numkeys = tnumkeys;
        numops = tnumops;
    }

    public TXTesterThread(CorfuDBMap<Integer, Integer> tmap1, CorfuDBMap<Integer, Integer> tmap2, AbstractRuntime tcr)
    {
        this(tmap1, tmap2, tcr, 10, 100);
    }

    public TXTesterThread(CorfuDBMap<Integer, Integer> tmap1, CorfuDBMap<Integer, Integer> tmap2, AbstractRuntime tcr, int tnumkeys, int tnumops)
    {
        map1 = tmap1;
        map2 = tmap2;
        cr = tcr;
        numkeys = tnumkeys;
        numops = tnumops;
    }

    public boolean check_consistency()
    {
        boolean consistent = true;
        int numretries = 10;
        int j = 0;
        for(j=0;j<numretries;j++)
        {
            cr.BeginTX();
            for (int i = 0; i < numkeys; i++)
            {
                if (map1.containsKey(i))
                {
                    if (!map2.containsKey(map1.get(i)) || map2.get(map1.get(i)) != i)
                    {
                        consistent = false;
                        System.out.println("inconsistency in map1 on " + i);
                        System.out.println("map1 contains " + i);
                        System.out.println("map1.get(" + i + ") is " + map1.get(i));
                        System.out.println("map2 contains " + map1.get(i) + " = " + map2.containsKey(map1.get(i)));
                        System.out.println("map2.get(" + map1.get(i) + ") is " + map2.get(map1.get(i)));
                        break;
                    }
                }
                if (map2.containsKey(i))
                {
                    if (!map1.containsKey(map2.get(i)) || map1.get(map2.get(i)) != i)
                    {
                        consistent = false;
                        System.out.println("inconsistency in map2 on " + i);
                        System.out.println("map2 contains " + i);
                        System.out.println("map2.get(" + i + ") is " + map2.get(i));
                        System.out.println("map1 contains " + map2.get(i) + " = " + map1.containsKey(map2.get(i)));
                        System.out.println("map1.get(" + map2.get(i) + ") is " + map1.get(map2.get(i)));
                        break;
                    }
                }
            }
            if (cr.EndTX())
            {
                break;
            }
            else
            {
                consistent = true;
                System.out.println("abort! retrying consistency check...");
            }
        }
        if(j==numretries) throw new RuntimeException("too many aborts on consistency check");
        return consistent;
    }

    public void run()
    {
        int numcommits = 0;
        System.out.println("starting thread");
        if(numkeys<2) throw new RuntimeException("minimum number of keys for test is 2");
        for(int i=0;i<numops;i++)
        {
            long curtime = System.currentTimeMillis();
            dbglog.debug("Tx starting...");
            int x = (int) (Math.random() * numkeys);
            int y = x;
            while(y==x)
                y = (int) (Math.random() * numkeys);
            cr.BeginTX();
            if(map1.containsKey(x)) //if x is occupied, delete the edge from x
            {
                int t = map1.get(x);
                System.out.println("Deleting " + x + " from map1; deleting " + t + " from map2;");
                map2.remove(t);
                map1.remove(x);
            }
            else if(map2.containsKey(y)) //if y is occupied, delete the edge from y
            {
                int t = map2.get(y);
                System.out.println("Deleting " + y + " from map2; deleting " + t + " from map1;");
                map1.remove(t);
                map2.remove(y);
            }
            else
            {
                System.out.println("Creating an edge between " + x + " and " + y);
                map1.put(x, y);
                map2.put(y, x);
            }
            if(cr.EndTX())
                numcommits++;
//            else
//                dbglog.warn("aborting...");
            dbglog.debug("Tx took {}", (System.currentTimeMillis()-curtime));
/*            try
            {
                Thread.sleep((int)(Math.random()*1000.0));
            }
            catch(Exception e)
            {
                throw new RuntimeException(e);
            }*/
        }
        System.out.println("Tester thread is done: " + numcommits + " commits out of " + numops);
    }

}


class CtrTesterThread implements Runnable
{
    CorfuDBCounter ctr;
    public CtrTesterThread(CorfuDBCounter tcob)
    {
        ctr = tcob;
    }

    public void run()
    {
        System.out.println("starting thread");
        for(int i=0;i<100;i++)
        {
            ctr.increment();
            System.out.println("counter value = " + ctr.read());
        }
    }
}


class MapTesterThread implements Runnable
{

    Map cmap;
    public MapTesterThread(Map tcob)
    {
        cmap = tcob;
    }

    public void run()
    {
        System.out.println("starting thread");
        for(int i=0;i<100;i++)
        {
            int x = (int) (Math.random() * 1000.0);
            System.out.println("changing key " + x + " from " + cmap.put(x, "ABCD") + " to " + cmap.get(x));
        }
    }
}


/*
class StreamTester implements Runnable
{
    IStream sb;
    int numops = 10000;
    public StreamTester(IStream tsb, int nops)
    {
        sb = tsb;
        numops = nops;
    }
    public void run()
    {
        System.out.println("starting sb tester thread");
        for(int i=0;i<numops;i++)
        {
            byte x[] = new byte[5];
            Set<Long> T = new HashSet<Long>();
            T.add(new Long(5));
            sb.append(new BufferStack(x), T);
        }
    }
}
*/





/*class CorfuDBRegister implements CorfuDBObject
{
	ByteBuffer converter;
	int registervalue;
	CorfuDBRuntime TR;
	long oid;
	public long getID()
	{
		return oid;
	}

	public CorfuDBRegister(CorfuDBRuntime tTR, long toid)
	{
		registervalue = 0;
		TR = tTR;
		converter = ByteBuffer.wrap(new byte[minbufsize]); //hardcoded
		oid = toid;
		TR.registerObject(this);
	}
	public void upcall(BufferStack update)
	{
//		System.out.println("dummyupcall");
		converter.put(update.pop());
		converter.rewind();
		registervalue = converter.getInt();
		converter.rewind();
	}
	public void write(int newvalue)
	{
//		System.out.println("dummywrite");
		converter.putInt(newvalue);
		byte b[] = new byte[minbufsize]; //hardcoded
		converter.rewind();
		converter.get(b);
		converter.rewind();
		TR.updatehelper(new BufferStack(b), oid);
	}
	public int read()
	{
//		System.out.println("dummyread");
		TR.queryhelper(oid);
		return registervalue;
	}
	public int readStale()
	{
		return registervalue;
	}
}
*/

/*class PerfCounter
{
    String description;
    AtomicLong sum;
    AtomicInteger num;
    public PerfCounter(String desc)
    {
        sum = new AtomicLong();
        num = new AtomicInteger();
        description = desc;
    }
    public long incrementAndGet()
    {
        //it's okay for this to be non-atomic, since these are just perf counters
        //but we do need to get exact numbers eventually, hence the use of atomiclong/integer
        num.incrementAndGet();
        return sum.incrementAndGet();
    }
    public long addAndGet(long val)
    {
        num.incrementAndGet();
        return sum.addAndGet(val);
    }
}*/
