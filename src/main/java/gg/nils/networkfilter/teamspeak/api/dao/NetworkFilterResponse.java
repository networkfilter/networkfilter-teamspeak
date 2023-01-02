package gg.nils.networkfilter.teamspeak.api.dao;

import lombok.Data;

@Data
public class NetworkFilterResponse {

    private final boolean success;
    private final int queriesLeft;
    private final NetworkFilterResponseData data;

    @Data
    public class NetworkFilterResponseData {
        private final String ip;
        private final String asn;
        private final String org;
        private final boolean blocked;
        private final String blockedType;
        private final boolean cached;
        private final long took;
    }

}