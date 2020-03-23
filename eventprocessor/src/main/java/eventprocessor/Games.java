package eventprocessor;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class Games {

    public static long SESSION_TIME_DIFFERENCE = 60*60*1000; //after this amount of millis consider this game in a new session

    static public class GameEvent implements JSONSerdeCompatible {
        public Long timestamp; //start time in milliseconds since epoch
        public String type; //start; judgement; end
        public String game;
        public String experiment;
        public String campaign;
        public String player;
    }

    static public class Game implements JSONSerdeCompatible {
        public Long timestamp; //start time in milliseconds since epoch
        public String state; //open; finished
        public String game;
        public String experiment;
        public String campaign;
        public Long judgements;
        public Long duration; //duration in milliseconds
        public String session_id;
        public String player;
    }

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "eventprocessor");
        //props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, JsonTimestampExtractor.class);
        String bootstrap_servers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
        if (bootstrap_servers == null) bootstrap_servers = "kafka:9092";
        System.out.println("Connecting to " + bootstrap_servers);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap_servers);
        final Serde<Game> gameSerde = new JSONSerde<Game>(Game.class);
        final Serde<GameEvent> gameEventSerde = new JSONSerde<GameEvent>(GameEvent.class);
        final StreamsBuilder builder = new StreamsBuilder();
        System.out.println("Running...");
        final KStream<String, GameEvent> gameEvents = builder.stream("game-events-input", Consumed.with(Serdes.String(), gameEventSerde))
                .map((key, gameevent) -> new KeyValue<String, GameEvent>(gameevent.game + ":" + gameevent.player, gameevent));

        //gameEvents.
        /*
        .groupBy(
                (key, value) -> value.playerId
        )
        */
        Grouped<String, GameEvent> grouping = Grouped.with(Serdes.String(), gameEventSerde);
        final KTable<String, Game> gameTable = gameEvents.groupByKey(grouping)
                .aggregate(
                        Game::new,
                        (aggKey, gameEvent, game) -> {
                            if (gameEvent.type == null) {
                                return game;
                            }
                            if (gameEvent.type.equals("start")) {
                                if (game.timestamp == null) {
                                    game.session_id = UUID.randomUUID().toString();
                                } else {
                                    long timesincelastevent = gameEvent.timestamp - (game.timestamp + game.duration);
                                    if (timesincelastevent > SESSION_TIME_DIFFERENCE) {
                                        game.session_id = UUID.randomUUID().toString();
                                    }
                                }
                                game.game = gameEvent.game;
                                game.player = gameEvent.player;
                                game.experiment = gameEvent.experiment;
                                game.campaign = gameEvent.campaign;
                                game.timestamp = gameEvent.timestamp;
                                game.duration = 0L;
                                game.judgements = 0L;
                                game.state = "open";
                            } else {
                                if (game.timestamp == null) {
                                        game.duration = 0L;
                                } else {
                                        game.duration = gameEvent.timestamp - game.timestamp;
                                }
                                if (gameEvent.type.equals("end")) {
                                    game.state = "closed";
                                } else if (gameEvent.type.equals("judgement") || gameEvent.type.equals("judgment")) {
                                    if (game.judgements == null) game.judgements = 0L;
                                    game.judgements++;
                                }
                            }
                            System.out.println("update");
                            return game;
                        },
                        Materialized.with(Serdes.String(), gameSerde)
                );
        gameTable.toStream().to("game-updates", Produced.with(Serdes.String(), gameSerde));

        builder.stream("game-updates", Consumed.with(Serdes.String(), gameSerde))
                .filter((playerId, game) -> (game.state != null && game.state.equals("closed")))
                .to("finished-games", Produced.with(Serdes.String(), gameSerde));

        //builder.stream("finished-games", Consumed.with(Serdes.String(), gameSerde))
         //       .to("finished-games", Produced.with(Serdes.String(), gameSerde));
        //final KStream<String, Game> gameUpdatesStream = gameTable.toStream();
/*
        builder.<String, String>stream("streams-plaintext-input")
                .flatMapValues(value -> Arrays.asList(value.split("\\W+")))
                .to("streams-linesplit-output");
 */

        final Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }

}
