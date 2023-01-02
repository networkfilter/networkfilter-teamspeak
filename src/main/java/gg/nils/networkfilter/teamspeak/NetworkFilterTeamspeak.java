package gg.nils.networkfilter.teamspeak;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import gg.nils.networkfilter.teamspeak.api.NetworkFilterAPI;
import gg.nils.networkfilter.teamspeak.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class NetworkFilterTeamspeak {

    private final TS3Config ts3Config;
    private final TS3Query ts3Query;

    public NetworkFilterTeamspeak() {
        ConfigProperties configProperties = new ConfigProperties();

        new NetworkFilterAPI(configProperties.getApiKey());

        this.ts3Config = new TS3Config();
        this.ts3Config.setHost(configProperties.getTeamspeakHost());

        this.ts3Config.setEnableCommunicationsLogging(true);

        this.ts3Config.setFloodRate(TS3Query.FloodRate.DEFAULT);
        this.ts3Config.setReconnectStrategy(ReconnectStrategy.exponentialBackoff());

        this.ts3Config.setConnectionHandler(new ConnectionHandler() {
            @Override
            public void onConnect(TS3Api ts3Api) {
                // log.info("onConnect");
                ts3Api.login(
                        configProperties.getTeamspeakUsername(),
                        configProperties.getTeamspeakPassword()
                );
                ts3Api.selectVirtualServerByPort(
                        configProperties.getTeamspeakPort(),
                        configProperties.getTeamspeakNickname()
                );

                ts3Api.registerAllEvents();
                ts3Api.addTS3Listeners(new TS3EventAdapter() {
                    @Override
                    public void onClientJoin(ClientJoinEvent event) {
                        if (event.getClientType() != 0) return;

                        if (Arrays.stream(event.getClientServerGroups().split(",")).map(Integer::parseInt).anyMatch(value ->
                                configProperties.getTeamspeakBypassGroups().contains(value))) {
                            log.info("Skipped \"{}\" ({})", event.getClientNickname(), event.getUniqueClientIdentifier());
                            return;
                        }

                        ClientInfo client = NetworkFilterTeamspeak.this.ts3Query.getApi().getClientInfo(event.getClientId());

                        NetworkFilterTeamspeak.this.check(
                                event.getClientNickname(),
                                event.getClientId(),
                                event.getUniqueClientIdentifier(),
                                event.getClientDatabaseId(),
                                client.getIp(),
                                configProperties.getTeamspeakVPNChannel(),
                                configProperties.getTeamspeakVPNGroup()
                        );
                    }
                });

                for (Client client : ts3Api.getClients()) {
                    if (client.getType() != 0) continue;

                    if (Arrays.stream(client.getServerGroups()).anyMatch(value ->
                            configProperties.getTeamspeakBypassGroups().contains(value))) {
                        log.info("Skipped \"{}\" ({})", client.getNickname(), client.getUniqueIdentifier());
                        continue;
                    }

                    NetworkFilterTeamspeak.this.check(
                            client.getNickname(),
                            client.getId(),
                            client.getUniqueIdentifier(),
                            client.getDatabaseId(),
                            client.get(ClientProperty.CONNECTION_CLIENT_IP),
                            configProperties.getTeamspeakVPNChannel(),
                            configProperties.getTeamspeakVPNGroup()
                    );
                }
            }

            @Override
            public void onDisconnect(TS3Query ts3Query) {
                // log.info("onDisconnect");
            }
        });

        this.ts3Query = new TS3Query(this.ts3Config);
        this.ts3Query.connect();
    }

    public static void main(String[] args) {
        new NetworkFilterTeamspeak();
    }

    public void check(String nickname, int clientId, String uniqueClientIdentifier, int databaseId, String ip, int channelId, int groupId) {
        NetworkFilterAPI.getInstance().check(ip).whenComplete((block, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                return;
            }

            if (block) {
                this.ts3Query.getAsyncApi().moveClient(clientId, channelId);
                this.ts3Query.getAsyncApi().addClientToServerGroup(groupId, databaseId);
            }

            log.info("\"{}\" ({}) has {} vpn", nickname, uniqueClientIdentifier, block ? "a" : "no");
        });
    }
}
