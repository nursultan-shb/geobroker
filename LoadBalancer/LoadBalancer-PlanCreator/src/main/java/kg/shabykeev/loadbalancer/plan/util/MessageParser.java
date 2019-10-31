package kg.shabykeev.loadbalancer.plan.util;

import de.hasenburg.geobroker.commons.model.KryoSerializer;
import de.hasenburg.geobroker.commons.model.message.Payload;
import de.hasenburg.geobroker.commons.model.message.PayloadKt;
import de.hasenburg.geobroker.commons.model.message.Topic;
import kg.shabykeev.loadbalancer.commons.ServerLoadMetrics;
import kg.shabykeev.loadbalancer.commons.TopicMetrics;
import kg.shabykeev.loadbalancer.commons.ZMsgType;
import kg.shabykeev.loadbalancer.plan.generator.Metrics;
import kotlin.Pair;
import org.zeromq.ZMsg;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class MessageParser {

    private static KryoSerializer kryo = new KryoSerializer();

    public static Metrics parseMessage(List<ZMsg> messages) {
        ArrayList<ServerLoadMetrics> lmList = new ArrayList<>();
        ArrayList<TopicMetrics> topicPubMessagesList = new ArrayList<>();

        for (ZMsg message : messages) {
            Pair<String, Payload> pair = PayloadKt.transformZMsgWithId(message, kryo);
            if (pair != null) {
                String server = pair.getFirst();
                Payload payload = pair.getSecond();

                if (payload instanceof Payload.MetricsPayload) {
                    Payload.MetricsPayload metricsPayload = (Payload.MetricsPayload) payload;
                    ServerLoadMetrics slm = new ServerLoadMetrics(server, null, metricsPayload.getCpuLoad());
                    lmList.add(slm);

                    //parse topic metrics
                    if (metricsPayload.getPublishedMessages().size() > 0) {
                        for (Map.Entry<Topic, Integer> entry : metricsPayload.getPublishedMessages().entrySet()) {

                            topicPubMessagesList.add(new TopicMetrics(server, entry.getKey().getTopic(), entry.getValue()));
                        }
                    }
                }
            }
        }

        return new Metrics(aggregateServerLoadMetrics(lmList), aggregateTopicMetrics(topicPubMessagesList));
    }

    public static Metrics parseMessage(String metrics) {

        ArrayList<ServerLoadMetrics> lmList = new ArrayList<>();
        ArrayList<TopicMetrics> topicPubMessagesList = new ArrayList<>();

        metrics = deleteEdgeSymbols(metrics.trim());
        String[] strValues = metrics.split("],");

        for (String strValue : strValues) {
            String value = replaceSpecialCharacters(strValue);
            String[] elements = Arrays.stream(value.split(",")).map(String::trim).toArray(String[]::new);

            if (value.contains(ZMsgType.TOPIC_METRICS.toString())) {
                String server = elements[2];
                ServerLoadMetrics slm = new ServerLoadMetrics(server, elements[0], Double.valueOf(elements[3]));
                lmList.add(slm);

                //parse topic metrics
                if (elements.length > 4) {
                    topicPubMessagesList.addAll(parseTopicMetrics(elements[4], server));
                }
            }
        }

        return new Metrics(aggregateServerLoadMetrics(lmList), aggregateTopicMetrics(topicPubMessagesList));
    }

    private static ArrayList<TopicMetrics> parseTopicMetrics(String value, String server) {
        ArrayList<TopicMetrics> tmList = new ArrayList<>();

        if (value.trim().length() < 3) {
            return tmList;
        }

        String[] keyValuePairs = value.split("\\|");

        for (String pair : keyValuePairs) {
            String[] entry = pair.split("=");
            TopicMetrics tm = new TopicMetrics(server, entry[0].trim(), Integer.valueOf(entry[1].trim()));

            tmList.add(tm);
        }

        return tmList;
    }

    private static String deleteEdgeSymbols(String str) {
        return str.substring(1, str.length() - 1).trim();
    }

    private static String replaceSpecialCharacters(String str) {
        return str.replace("[", "").replace("]", "");
    }


    /**
     * Takes ArrayList of ServerLoadMetrics in
     * Groups ServerLoadMetrics by a server and aggregates by max load.
     * Returns an aggregated by server a list of ServerLoadMetrics.
     *
     * @param lmList a list of ServerLoadMetrics
     * @return ArrayList of ServerLoadMetrics grouped by a server
     */
    private static ArrayList<ServerLoadMetrics> aggregateServerLoadMetrics(ArrayList<ServerLoadMetrics> lmList) {
        ArrayList<ServerLoadMetrics> slAggMetrics = new ArrayList<>();

        Map<String, Optional<ServerLoadMetrics>> lmMap = lmList.stream()
                .collect(groupingBy(ServerLoadMetrics::getServer,
                        Collectors.maxBy(Comparator.comparing(ServerLoadMetrics::getLoad))));

        for (Map.Entry element : lmMap.entrySet()) {
            Object obj = element.getValue();
            if (obj != null) {
                slAggMetrics.add(((Optional<ServerLoadMetrics>) obj).get());
            }
        }

        return slAggMetrics;
    }

    /**
     * Takes ArrayList of TopicMetrics in
     * Groups TopicMetrics by a server and aggregates by max messages count,
     * thereby leaving only latest messages by a topic.
     * Returns an aggregated by server a list of TopicMetrics.
     *
     * @param tmList a list of TopicMetrics
     * @return ArrayList of TopicMetrics grouped by a server
     */
    private static ArrayList<TopicMetrics> aggregateTopicMetrics(ArrayList<TopicMetrics> tmList) {
        ArrayList<TopicMetrics> topicAggMetrics = new ArrayList<>();

        Map<String, Map<String, Optional<TopicMetrics>>> tmMap = tmList.stream()
                .collect(groupingBy(TopicMetrics::getServer, groupingBy(TopicMetrics::getTopic,
                        Collectors.maxBy(Comparator.comparing(TopicMetrics::getMessagesCount)))));

        for (Map.Entry element : tmMap.entrySet()) {
            Object obj = element.getValue();
            if (obj != null) {
                Map<String, Optional<TopicMetrics>> nestedMap = ((Map) (obj));
                for (Map.Entry nestedElement : nestedMap.entrySet()) {
                    Object nestedObject = nestedElement.getValue();
                    if (nestedObject != null) {
                        topicAggMetrics.add(((Optional<TopicMetrics>) nestedObject).get());
                    }
                }
            }
        }

        return topicAggMetrics;
    }
}
