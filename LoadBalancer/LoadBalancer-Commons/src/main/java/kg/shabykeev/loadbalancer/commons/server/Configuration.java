package kg.shabykeev.loadbalancer.commons.server;

import com.moandjiezana.toml.Toml;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;

public class Configuration {
    private static final Logger logger = LogManager.getLogger();

    private String serverId;
    private Integer frontendPort = 7225;
    private Integer backendPort = 5559;
    private String address;

    public String getPlanCreatorAddress() {
        return planCreatorAddress;
    }

    private String planCreatorAddress;

    public Integer getPlanGenerationDelay() {
        return planGenerationDelay;
    }

    public void setPlanGenerationDelay(Integer planGenerationDelay) {
        this.planGenerationDelay = planGenerationDelay;
    }

    private Integer planGenerationDelay = 10; //seconds

    public String getServerId() {
        return serverId;
    }

    public Integer getFrontendPort() {
        return frontendPort;
    }

    public Integer getBackendPort() {
        return backendPort;
    }

    public String getAddress() {
        return address;
    }

    public static Configuration readConfiguration(String fileName) {
        try {
            Configuration c = new Configuration();
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream is = classloader.getResourceAsStream(fileName);

            Toml toml = new Toml().read (is);

            return parseToml(c, toml);
        } catch (Exception e) {
            logger.fatal("Could not read configuration", e);
        }
        System.exit(1);
        return null; // WHY DO I NEED YOU?
    }

    private static Configuration parseToml(Configuration c, Toml toml) {

        // server information
        Toml toml_server = toml.getTable("server");
        c.serverId = toml_server.getString("serverId", c.serverId);
        c.address = toml_server.getString("address", c.address);
        c.frontendPort = Math.toIntExact(toml_server.getLong("frontendPort", c.frontendPort.longValue()));
        c.backendPort = Math.toIntExact(toml_server.getLong("backendPort", c.backendPort.longValue()));
        c.planGenerationDelay = Math.toIntExact(toml_server.getLong("planGenerationDelay", c.planGenerationDelay.longValue()));
        c.planCreatorAddress = toml_server.getString("planCreatorAddress", c.planCreatorAddress);
        return c;
    }
}
