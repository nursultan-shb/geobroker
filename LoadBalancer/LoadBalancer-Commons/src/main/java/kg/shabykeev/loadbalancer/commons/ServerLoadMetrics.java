package kg.shabykeev.loadbalancer.commons;

public class ServerLoadMetrics implements Comparable<ServerLoadMetrics>{

    public ServerLoadMetrics(String server, String localLoadAnalyzer, Double load){
        this.server = server;
        this.localLoadAnalyzer = localLoadAnalyzer;
        this.load = load;
    }


    public String getServer() {
        return server;
    }


    public void setServer(String server) {
        this.server = server;
    }

    public Double getLoad() {
        return load;
    }

    public void setLoad(Double load) {
        this.load = load;
    }

    /**
     * A pub/sub server, i.e., GeoBroker
     * */
    private String server;

    public String getLocalLoadAnalyzer() {
        return localLoadAnalyzer;
    }

    public void setLocalLoadAnalyzer(String localLoadAnalyzer) {
        this.localLoadAnalyzer = localLoadAnalyzer;
    }

    private String localLoadAnalyzer;
    private Double load;

    @Override
    public int compareTo(ServerLoadMetrics s) {
        return this.getLoad().compareTo(s.getLoad());
    }
}
