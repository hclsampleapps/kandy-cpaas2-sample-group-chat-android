package com.hcl.kandy.cpass;

import com.rbbn.cpaas.mobile.utilities.Configuration;
import com.rbbn.cpaas.mobile.utilities.webrtc.CodecSet;
import com.rbbn.cpaas.mobile.utilities.webrtc.ICEOptions;

public class ConfigurationHelper {

    public static void setConfigurations(String baseUrl) {
        Configuration configuration = Configuration.getInstance();
        configuration.setDTLS(true);
        configuration.setIceOption(ICEOptions.ICE_VANILLA);
        configuration.setICECollectionTimeout(12);
        setPreferedCodecs(baseUrl);
    }

    private static void setPreferedCodecs(String baseUrl) {

        Configuration configuration = Configuration.getInstance();

        CodecSet codecSet = new CodecSet();
        configuration.setPreferredCodecSet(codecSet);
    }
}
