package org.starrepublic.beyond_the_end.configs;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class BTEConfigs {
    public static final BTEClientConfig CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    public static final BTEServerConfig SERVER;
    public static final ModConfigSpec SERVER_SPEC;


    static {

        Pair<BTEClientConfig, ModConfigSpec> clientPair =
                new ModConfigSpec.Builder()
                        .configure(BTEClientConfig::new);

        CLIENT = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();


        Pair<BTEServerConfig, ModConfigSpec> serverPair =
                new ModConfigSpec.Builder()
                        .configure(BTEServerConfig::new);

        SERVER = serverPair.getLeft();
        SERVER_SPEC = serverPair.getRight();
    }


    private BTEConfigs() {

    }
}
