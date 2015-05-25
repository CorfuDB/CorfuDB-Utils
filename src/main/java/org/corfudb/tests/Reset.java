package org.corfudb.tests;

import org.corfudb.runtime.CorfuDBRuntime;
import org.corfudb.runtime.view.IConfigurationMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by dmalkhi on 1/16/15.
 */
public class Reset {

    private static final Logger log = LoggerFactory.getLogger(Reset.class);

    /**
     * /**
     *
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

        try (CorfuDBRuntime client = new CorfuDBRuntime(masteraddress)) {
            client.startViewManager();
            client.waitForViewReady();
            IConfigurationMaster cm = (IConfigurationMaster) client.getView().getConfigMasters().get(0);
            cm.resetAll();
        }
        log.info("Successfully reset the configuration!");
        System.exit(0);

    }
}

