package gg.nils.networkfilter.teamspeak.config;

import gg.nils.networkfilter.teamspeak.NetworkFilterTeamspeak;
import lombok.Getter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

@Getter
public class ConfigProperties {

    private final String apiKey;
    private final String teamspeakHost;
    private final int teamspeakPort;
    private final String teamspeakNickname;
    private final String teamspeakUsername;
    private final String teamspeakPassword;

    public ConfigProperties() {
        File file = new File("config.properties");

        if (!file.exists()) {
            try (InputStream input = NetworkFilterTeamspeak.class.getClassLoader().getResourceAsStream("config.properties")) {

                Files.copy(Objects.requireNonNull(input), file.toPath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        Properties properties = new Properties();

        try (InputStream input = new FileInputStream(file)) {
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        this.apiKey = getConfigVar("API_KEY", properties);
        this.teamspeakHost = getConfigVar("TEAMSPEAK_HOST", properties);
        this.teamspeakPort = Integer.parseInt(getConfigVar("TEAMSPEAK_PORT", properties));
        this.teamspeakNickname = getConfigVar("TEAMSPEAK_NICKNAME", properties);
        this.teamspeakUsername = getConfigVar("TEAMSPEAK_USERNAME", properties);
        this.teamspeakPassword = getConfigVar("TEAMSPEAK_PASSWORD", properties);
    }

    private String getConfigVar(String key, Properties properties) {
        String env = System.getenv(key);

        return env != null ? env : properties.getProperty(key.replaceAll("_", ".").toLowerCase(Locale.ROOT));
    }
}
