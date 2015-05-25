package org.corfudb.tests;

import org.corfudb.runtime.CorfuDBRuntime;
import org.corfudb.runtime.OutOfSpaceException;
import org.corfudb.runtime.entries.IStreamEntry;
import org.corfudb.runtime.stream.*;
import org.corfudb.runtime.view.IConfigurationMaster;
import org.corfudb.runtime.view.IStreamingSequencer;
import org.corfudb.runtime.view.IWriteOnceAddressSpace;
import org.corfudb.util.CorfuDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import org.corfudb.runtime.LinearizationException;

/**
 * Created by dmalkhi on 1/16/15.
 */
public class CorfuHopStreamTemporaryMultiHello {

    private static final Logger log = LoggerFactory.getLogger(CorfuHopStreamTemporaryMultiHello.class);
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

                    log.info("Pulling stream  " + streamID3.toString() + ", " + streamID2.toString() + " to stream " + streamID.toString() + " for 3 entries");
                    List<UUID> streams = new ArrayList<UUID>();
                    streams.add(streamID2);
                    streams.add(streamID3);
                    s.pullStream(streams, 3);
/*
                log.info("Waiting for pull to complete on all streams...");
                while (true)
                {
                    try {
                    s2.waitForEpochChange(address);
                    s3.waitForEpochChange(address2);
                    break;
                    } catch (InterruptedException ie) {}
                }
                log.info("Pull complete on all streams");
*/

                    ITimestamp s1_firstAddress = null;
                    ITimestamp s1_secondAddress = null;
                    ITimestamp s1_thirdAddress = null;

                    ITimestamp s2_firstAddress = null;
                    ITimestamp s2_secondAddress = null;
                    ITimestamp s2_thirdAddress = null;

                    ITimestamp s3_firstAddress = null;
                    ITimestamp s3_secondAddress = null;
                    ITimestamp s3_thirdAddress = null;

                    try {
                        log.info("Appending to outer stream " + streamID.toString());
                        s1_firstAddress = s.append("hello world remote from outer stream " + streamID.toString());
                        log.info("Successfully appended hello world into log position " + address + ", stream " + streamID.toString());

                        log.info("Syncing inner streams to outer stream entry.");
                        while (true) {
                            //try {
                            log.info("Actually, NOT Syncing inner streams to outer stream entry...NOT IMPLEMENTED!");
                            // s2.sync(s1_firstAddress);
                            //s3.sync(s1_firstAddress);
                            break;
                            //} catch (InterruptedException ie) {}
                            //catch (LinearizationException le) {}
                        }
                        log.info("Finished syncing inner streams.");

                        log.info("Appending to inner stream " + streamID2.toString());
                        s1_secondAddress = s2.append("hello world remote from inner stream " + streamID2.toString());
                        log.info("Successfully appended hello world into log position " + address + ", stream " + streamID2.toString());
                        log.info("Appending to inner stream " + streamID3.toString());
                        s1_thirdAddress = s3.append("hello world remote from inner stream " + streamID3.toString());
                        log.info("Successfully appended hello world into log position " + address + ", stream " + streamID3.toString());
                    } catch (OutOfSpaceException oose) {
                        log.error("Out of space during append!", oose);
                        System.exit(1);
                    }

                    checkAddresses(s1_firstAddress, s1_secondAddress, s1_thirdAddress);

                    IStreamEntry cdse;
                    log.debug("Reading back 3 results from outer stream " + streamID.toString());
                    cdse = s.readNextEntry();
                    s1_firstAddress = cdse.getTimestamp();
                    sresult = (String) cdse.getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("hello world remote from outer stream " + streamID.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }

                    cdse = s.readNextEntry();
                    s1_secondAddress = cdse.getTimestamp();
                    sresult = (String) cdse.getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("hello world remote from inner stream " + streamID2.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }

                    cdse = s.readNextEntry();
                    s1_thirdAddress = cdse.getTimestamp();
                    sresult = (String) cdse.getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("hello world remote from inner stream " + streamID3.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }
                    checkAddresses(s1_firstAddress, s1_secondAddress, s1_thirdAddress);


                    log.debug("Reading back 3 results from inner stream " + streamID2.toString());
                    cdse = s2.readNextEntry();
                    s2_firstAddress = cdse.getTimestamp();
                    sresult = (String) cdse.getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("hello world remote from outer stream " + streamID.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }

                    cdse = s2.readNextEntry();
                    s2_secondAddress = cdse.getTimestamp();
                    sresult = (String) cdse.getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("hello world remote from inner stream " + streamID2.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }

                    cdse = s2.readNextEntry();
                    s2_thirdAddress = cdse.getTimestamp();
                    sresult = (String) cdse.getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("hello world remote from inner stream " + streamID3.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }
                    checkAddresses(s2_firstAddress, s2_secondAddress, s2_thirdAddress);

                    log.debug("Reading back 3 results from inner stream " + streamID3.toString());
                    cdse = s3.readNextEntry();
                    s3_firstAddress = cdse.getTimestamp();
                    sresult = (String) cdse.getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("hello world remote from outer stream " + streamID.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }

                    cdse = s3.readNextEntry();
                    s3_secondAddress = cdse.getTimestamp();
                    sresult = (String) cdse.getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("hello world remote from inner stream " + streamID2.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }

                    cdse = s3.readNextEntry();
                    s3_thirdAddress = cdse.getTimestamp();
                    sresult = (String) cdse.getPayload();
                    log.info("Contents were: " + sresult);
                    if (!sresult.toString().equals("hello world remote from inner stream " + streamID3.toString())) {
                        log.error("ASSERT Failed: String did not match!");
                        System.exit(1);
                    }
                    checkAddresses(s3_firstAddress, s3_secondAddress, s3_thirdAddress);

                    log.debug("Checking permutations of address comparisons");
                    checkAddresses(s1_firstAddress, s1_secondAddress, s2_thirdAddress);
                    checkAddresses(s1_firstAddress, s1_secondAddress, s3_thirdAddress);
                    checkAddresses(s1_firstAddress, s2_secondAddress, s1_thirdAddress);
                    checkAddresses(s1_firstAddress, s2_secondAddress, s2_thirdAddress);
                    checkAddresses(s1_firstAddress, s2_secondAddress, s3_thirdAddress);
                    checkAddresses(s1_firstAddress, s3_secondAddress, s1_thirdAddress);
                    checkAddresses(s1_firstAddress, s3_secondAddress, s2_thirdAddress);
                    checkAddresses(s1_firstAddress, s3_secondAddress, s3_thirdAddress);

                    log.debug("Checking address equality");
                    checkAddressEquality(s1_firstAddress, s2_firstAddress);
                    checkAddressEquality(s1_firstAddress, s3_firstAddress);
                    checkAddressEquality(s1_secondAddress, s2_secondAddress);
                    checkAddressEquality(s1_secondAddress, s3_secondAddress);
                    checkAddressEquality(s1_thirdAddress, s2_thirdAddress);
                    checkAddressEquality(s1_thirdAddress, s3_thirdAddress);

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

