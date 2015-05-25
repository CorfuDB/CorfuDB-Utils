package org.corfudb.tests;

import org.corfudb.runtime.CorfuDBRuntime;
import org.corfudb.runtime.OutOfSpaceException;
import org.corfudb.runtime.entries.OldBundleEntry;
import org.corfudb.runtime.entries.IBundleEntry;
import org.corfudb.runtime.entries.IStreamEntry;
import org.corfudb.runtime.stream.OldBundle;
import org.corfudb.runtime.stream.IHopStream;
import org.corfudb.runtime.stream.ITimestamp;
import org.corfudb.runtime.view.IConfigurationMaster;
import org.corfudb.runtime.view.IStreamingSequencer;
import org.corfudb.runtime.view.IWriteOnceAddressSpace;
import org.corfudb.util.CorfuDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CorfuBundleHello {

    private static final Logger log = LoggerFactory.getLogger(CorfuBundleHello.class);
    public static final String DEFAULT_ADDRESS_SPACE = "WriteOnceAddressSpace";
    public static final String DEFAULT_STREAM_IMPL = "HopStream";

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
        opts.put("--stream-impl", DEFAULT_STREAM_IMPL);
        CorfuDBFactory factory = new CorfuDBFactory(opts);
        CorfuDBRuntime client = factory.getRuntime();
        client.startViewManager();
        IWriteOnceAddressSpace addressSpace = factory.getWriteOnceAddressSpace(client);
        IStreamingSequencer sequencer = factory.getStreamingSequencer(client);
        IConfigurationMaster configMaster = factory.getConfigurationMaster(client);

        UUID streamID = UUID.randomUUID();
        UUID streamID2 = UUID.randomUUID();
        UUID streamID3 = UUID.randomUUID();

        ITimestamp address = null;
        ITimestamp address2 = null;


        log.info("Stream 1={}", streamID);
        log.info("Stream 2={}", streamID2);
        log.info("Stream 3={}", streamID3);

        try (IHopStream s = (IHopStream) factory.getStream(streamID, sequencer, addressSpace)) {
            try (IHopStream s2 = (IHopStream) factory.getStream(streamID2, sequencer, addressSpace)) {
                try (IHopStream s3 = (IHopStream) factory.getStream(streamID3, sequencer, addressSpace)) {

                    log.info("Appending hello world into all logs...");

                    try {
                        address = s.append("hello world from stream " + streamID.toString());
                        log.info("Successfully appended hello world into log position " + address + ", stream " + streamID.toString());
                        address = s2.append("hello world from stream " + streamID2.toString());
                        log.info("Successfully appended hello world into log position " + address + ", stream " + streamID2.toString());
                        address2 = s3.append("hello world from stream " + streamID3.toString());
                        log.info("Successfully appended hello world into log position " + address + ", stream " + streamID3.toString());
                    } catch (OutOfSpaceException oose) {
                        log.error("Out of space during append!", oose);
                        System.exit(1);
                    }

                    log.info("Reading back initial entries");
                    String sresult = (String) s.readNextObject();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("hello world from stream " + streamID.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }

                    sresult = (String) s2.readNextObject();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("hello world from stream " + streamID2.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }

                    sresult = (String) s3.readNextObject();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("hello world from stream " + streamID3.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }

                    log.info("Creating new bundle entry on stream " + streamID.toString());

                    ArrayList<UUID> remoteStreams = new ArrayList<UUID>();
                    remoteStreams.add(streamID2);
                    remoteStreams.add(streamID3);
                    OldBundle b = new OldBundle(s, remoteStreams, "Bundled Hello World!", true);
                    ITimestamp timestampBundle = b.apply();

                    ITimestamp s1_bundle;
                    ITimestamp s2_bundle;
                    ITimestamp s3_bundle;

                    IBundleEntry b2;
                    IBundleEntry b3;

                    IStreamEntry cdse;
                    log.debug("Reading back bundled append.");
                    cdse = s.readNextEntry();
                    s1_bundle = cdse.getTimestamp();
                    sresult = (String) cdse.getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("Bundled Hello World!")) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }

                    log.error("XXXX: CJR: exiting CorfuBundleHello early because previous code no longer supports the requisite interfaces.");
                    log.error("XXXX: CJR: specifically, IBundleEntry does not appear to extend IStreamEntry. Consequently, the original code.");
                    log.error("XXXX: CJR: does not build, and the fixes to force it to build require casting IBundleEntry objects to IStreamEntry... ");
                    System.exit(1);

                    IBundleEntry be = (IBundleEntry) s2.readNextEntry();
                    b2 = (IBundleEntry) s2.readNextEntry();
                    IStreamEntry se2 = (IStreamEntry) b2;
                    s2_bundle = ((IStreamEntry) b2).getTimestamp();
                    sresult = (String) ((IStreamEntry) b2).getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("Bundled Hello World!")) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }
                    b3 = (IBundleEntry) s3.readNextEntry();
                    s3_bundle = ((IStreamEntry) b3).getTimestamp();
                    sresult = (String) ((IStreamEntry) b3).getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("Bundled Hello World!")) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }

                    log.debug("s1=" + s1_bundle.toString() + ", s2=" + s2_bundle.toString());
                    log.info("Making sure addresses are all equal");
                    checkAddressEquality(s1_bundle, s2_bundle);
                    checkAddressEquality(s2_bundle, s3_bundle);

                    /* Now we append to the bundles, and see if we can read back the same entry on all streams. */
                    log.info("Appending to each bundle");
                    ITimestamp s2_bundleAppend = b2.writeSlot("Hello from bundle " + streamID2.toString());
                    ITimestamp s3_bundleAppend = b3.writeSlot("Hello from bundle " + streamID3.toString());

                    ITimestamp s1_firstAddress = null;
                    ITimestamp s1_secondAddress = null;
                    ITimestamp s2_firstAddress = null;
                    ITimestamp s2_secondAddress = null;
                    ITimestamp s3_firstAddress = null;
                    ITimestamp s3_secondAddress = null;

                    log.debug("Reading back 2 results from outer stream " + streamID.toString());
                    cdse = s.readNextEntry();
                    s1_firstAddress = cdse.getTimestamp();
                    sresult = (String) cdse.getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("Hello from bundle " + streamID2.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }
                    cdse = s.readNextEntry();
                    s1_secondAddress = cdse.getTimestamp();
                    sresult = (String) cdse.getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("Hello from bundle " + streamID3.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }
                    checkAddresses(s1_bundle, s1_firstAddress, s1_secondAddress);

                    log.debug("Reading back 2 results from inner stream " + streamID2.toString());
                    cdse = s2.readNextEntry();
                    s2_firstAddress = cdse.getTimestamp();
                    sresult = (String) cdse.getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("Hello from bundle " + streamID2.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }
                    cdse = s2.readNextEntry();
                    s2_secondAddress = cdse.getTimestamp();
                    sresult = (String) cdse.getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("Hello from bundle " + streamID3.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }
                    checkAddresses(s2_bundle, s2_firstAddress, s2_secondAddress);

                    log.debug("Reading back 2 results from inner stream " + streamID2.toString());
                    cdse = s3.readNextEntry();
                    s3_firstAddress = cdse.getTimestamp();
                    sresult = (String) cdse.getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("Hello from bundle " + streamID2.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }
                    cdse = s3.readNextEntry();
                    s3_secondAddress = cdse.getTimestamp();
                    sresult = (String) cdse.getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("Hello from bundle " + streamID3.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }
                    checkAddresses(s3_bundle, s3_firstAddress, s3_secondAddress);

                    log.info("Checking for address equality on bundle");
                    checkAddressEquality(s1_firstAddress, s2_firstAddress);
                    checkAddressEquality(s2_firstAddress, s3_firstAddress);
                    checkAddressEquality(s1_secondAddress, s2_secondAddress);
                    checkAddressEquality(s2_secondAddress, s3_secondAddress);

                    try {
                        log.info("Appending to detached stream " + streamID.toString());
                        address = s.append("hello world from detached stream " + streamID.toString());
                        log.info("Successfully appended hello world into log position " + address + ", stream " + streamID.toString());
                        log.info("Appending to detached stream " + streamID2.toString());
                        address = s2.append("hello world from detached stream " + streamID2.toString());
                        log.info("Successfully appended hello world into log position " + address + ", stream " + streamID2.toString());
                        log.info("Appending to detached stream " + streamID3.toString());
                        address = s3.append("hello world from detached stream " + streamID3.toString());
                        log.info("Successfully appended hello world into log position " + address + ", stream " + streamID3.toString());

                    } catch (OutOfSpaceException oose) {
                        log.error("Out of space during append!", oose);
                        System.exit(1);
                    }

                    log.info("Reading back detached entries");
                    sresult = (String) s.readNextObject();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("hello world from detached stream " + streamID.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }
                    sresult = (String) s2.readNextObject();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("hello world from detached stream " + streamID2.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }
                    sresult = (String) s3.readNextObject();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("hello world from detached stream " + streamID3.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }

                    try (IHopStream s2_1 = (IHopStream) factory.getStream(streamID2, sequencer, addressSpace)) {
                        IStreamEntry cdbse;
                        log.info("Re-reading stream 2");
                        log.info("Contents were: " + sresult);
                        sresult = (String) s2_1.readNextObject();
                        if (!sresult.toString().equals("hello world from stream " + streamID2.toString())) {
                            log.error("ASSERT Failed: String did not match!");
                            System.exit(1);
                        }
                        cdbse = s2_1.readNextEntry();
                        s2_bundle = cdbse.getTimestamp();
                        sresult = (String) cdbse.getPayload();
                        log.info("Contents were: " + sresult);
                        if (!sresult.toString().equals("Bundled Hello World!")) {
                            log.error("ASSERT Failed: String did not match!");
                            System.exit(1);
                        }
                        log.debug("Reading back 2 results from inner stream " + streamID2.toString());
                        cdse = s2_1.readNextEntry();
                        s2_firstAddress = cdse.getTimestamp();
                        sresult = (String) cdse.getPayload();
                        log.info("Contents were: " + sresult);
                        if (!sresult.toString().equals("Hello from bundle " + streamID2.toString())) {
                            log.error("ASSERT Failed: String did not match!");
                            System.exit(1);
                        }
                        cdse = s2_1.readNextEntry();
                        s2_secondAddress = cdse.getTimestamp();
                        sresult = (String) cdse.getPayload();
                        log.info("Contents were: " + sresult);
                        if (!sresult.toString().equals("Hello from bundle " + streamID3.toString())) {
                            log.error("ASSERT Failed: String did not match!");
                            System.exit(1);
                        }
                        checkAddresses(s2_bundle, s2_firstAddress, s2_secondAddress);
                    }

                    try (IHopStream s3_1 = (IHopStream) factory.getStream(streamID3, sequencer, addressSpace)) {
                        IStreamEntry cdbse;
                        log.info("Re-reading stream 3");
                        log.info("Contents were: " + sresult);
                        sresult = (String) s3_1.readNextObject();
                        if (!sresult.toString().equals("hello world from stream " + streamID3.toString())) {
                            log.error("ASSERT Failed: String did not match!");
                            System.exit(1);
                        }
                        cdbse = s3_1.readNextEntry();
                        s3_bundle = cdbse.getTimestamp();
                        sresult = (String) cdbse.getPayload();
                        log.info("Contents were: " + sresult);
                        if (!sresult.toString().equals("Bundled Hello World!")) {
                            log.error("ASSERT Failed: String did not match!");
                            System.exit(1);
                        }
                        log.debug("Reading back 2 results from inner stream " + streamID3.toString());
                        cdse = s3_1.readNextEntry();
                        s3_firstAddress = cdse.getTimestamp();
                        sresult = (String) cdse.getPayload();
                        log.info("Contents were: " + sresult);
                        if (!sresult.toString().equals("Hello from bundle " + streamID2.toString())) {
                            log.error("ASSERT Failed: String did not match!");
                            System.exit(1);
                        }
                        cdse = s3_1.readNextEntry();
                        s3_secondAddress = cdse.getTimestamp();
                        sresult = (String) cdse.getPayload();
                        log.info("Contents were: " + sresult);
                        if (!sresult.toString().equals("Hello from bundle " + streamID3.toString())) {
                            log.error("ASSERT Failed: String did not match!");
                            System.exit(1);
                        }
                        checkAddresses(s3_bundle, s3_firstAddress, s3_secondAddress);
                    }

                    System.exit(0);
                }
            }
        }
    }

    static void checkAddressEquality(ITimestamp a1, ITimestamp a2) {
        log.info("Checking that addresses are equal");
        log.info("Address 1 == Address 2");
        if (!(a1.equals(a2))) {
            log.error("ASSERT failed: Address 1 should EQUAL Address 2");
            System.exit(1);
        }
        log.info("Address 2 == Address 1");
        if (!(a2.equals(a1))) {
            log.error("ASSERT failed: Address 2 should EQUAL Address 1");
            System.exit(1);
        }
    }

    static void checkAddresses(ITimestamp firstAddress, ITimestamp secondAddress, ITimestamp thirdAddress) {
        try {
            log.info("Checking that addresses are comparable");
            log.info("Address 1 == Address 1");
            if (!(firstAddress.compareTo(firstAddress) == 0)) {
                log.error("ASSERT failed: Address 1 should EQUAL Address 1, got {}", firstAddress.compareTo(firstAddress));
                System.exit(1);
            }
            log.info("Address 2 == Address 2");
            if (!(secondAddress.compareTo(secondAddress) == 0)) {
                log.error("ASSERT failed: Address 2 should EQUAL Address 2, got {}", secondAddress.compareTo(secondAddress));
                System.exit(1);
            }
            log.info("Address 3 == Address 3");
            if (!(thirdAddress.compareTo(thirdAddress) == 0)) {
                log.error("ASSERT failed: Address 3 should EQUAL Address 3, got {}", thirdAddress.compareTo(thirdAddress));
                System.exit(1);
            }

            log.info("Address 1 < Address 2");
            if (!(firstAddress.compareTo(secondAddress) < 0)) {
                log.error("ASSERT failed: Address 1 should come BEFORE Address 2, got {}", firstAddress.compareTo(secondAddress));
                System.exit(1);
            }
            log.info("Address 1 < Address 3");
            if (!(firstAddress.compareTo(thirdAddress) < 0)) {
                log.error("ASSERT failed: Address 1 should come BEFORE Address 3, got {}", firstAddress.compareTo(thirdAddress));
                System.exit(1);
            }
            log.info("Address 2 > Address 1");
            if (!(secondAddress.compareTo(firstAddress) > 0)) {
                log.error("ASSERT failed: Address 2 should come AFTER Address 1, got {}", secondAddress.compareTo(firstAddress));
                System.exit(1);
            }
            log.info("Address 2 < Address 3");
            if (!(secondAddress.compareTo(thirdAddress) < 0)) {
                log.error("ASSERT failed: Address 2 should come BEFORE Address 3, got {}", secondAddress.compareTo(thirdAddress));
                System.exit(1);
            }
            log.info("Address 3 > Address 1");
            if (!(thirdAddress.compareTo(firstAddress) > 0)) {
                log.error("ASSERT failed: Address 3 should come AFTER Address 1, got {}", thirdAddress.compareTo(firstAddress));
                System.exit(1);
            }
            log.info("Address 3 > Address 2");
            if (!(thirdAddress.compareTo(secondAddress) > 0)) {
                log.error("ASSERT failed: Address 3 should come AFTER Address 2, got {}", thirdAddress.compareTo(secondAddress));
                System.exit(1);
            }
        } catch (ClassCastException cce) {
            log.error("ASSERT failed: all addresses of attached stream SHOULD be comparable", cce);
            System.exit(1);
        }
    }
}

