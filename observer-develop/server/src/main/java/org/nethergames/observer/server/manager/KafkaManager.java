package org.nethergames.observer.server.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.nethergames.observer.data.general.PlayerMessage;
import org.nethergames.observer.data.kick.Kick;
import org.nethergames.observer.data.punishment.Punishment;
import org.nethergames.observer.data.punishment.request.PunishmentRemovalAction;
import org.nethergames.observer.data.punishment.request.PunishmentWhitelistAction;
import org.nethergames.observer.data.reports.ServerReportBroadcast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class KafkaManager {
    private final Logger logger = LoggerFactory.getLogger("Kafka Module");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private KafkaProducer<String, String> producer;
    private boolean enabled = false;

    public KafkaManager() {
        String netHost = System.getenv("KAFKA_HOST");
        if (netHost == null) {
            logger.warn("KAFKA_HOST env variable is not set. Punishment broadcasting and other kafka features are disabled.");
            return;
        }

        // create Producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, netHost);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        producer = new KafkaProducer<>(properties);
        enabled = true;
    }

    public void shutdown() {
        producer.flush();
        producer.close();

        logger.info("Kafka is now disabled");
    }

    public void broadcastChatMessage(String targetXuid, PlayerMessage playerMessage) {
        if (!enabled) {
            logger.info("Cant publish, kafka is disabled");
            return;
        }

        try {
            String message = playerMessage.getMessage();
            producer.send(new ProducerRecord<>("ess_messages", "9:" + targetXuid, message), (metadata, exception) -> {
                if (metadata.offset() == -1) {
                    logger.error("Failed to publish {} to channel messages", message, exception);
                }
            });
        } catch (Throwable t) {
            logger.error("Error while publishing chat message {} to channel reports", playerMessage.toString(), t);
        }
    }

    public void broadcastReports(ServerReportBroadcast report) {
        if (!enabled) {
            logger.info("Cant publish, kafka is disabled");
            return;
        }

        try {
            String message = this.objectMapper.writeValueAsString(report);
            producer.send(new ProducerRecord<>("observer", "reports", message), (metadata, exception) -> {
                if (metadata.offset() == -1) {
                    logger.error("Failed to publish {} to channel reports", report, exception);
                }
            });
        } catch (Throwable t) {
            logger.error("Error while publishing reports notification for reports {} to channel reports", report.toString(), t);
        }
    }

    public void broadcastPunishment(Punishment punishment) {
        if (!enabled) {
            logger.info("Cant publish, kafka is disabled");
            return;
        }

        try {
            String message = this.objectMapper.writeValueAsString(punishment);
            producer.send(new ProducerRecord<>("observer", "punished", message), defaultCallPunished(punishment));
        } catch (Throwable t) {
            logger.error("Error while publishing punishment notification for punishment {} to channel punishments", punishment.toString(), t);
        }
    }

    public void broadcastKick(Kick kick) {
        if (!enabled) {
            logger.info("Cant publish, kafka is disabled");
            return;
        }

        try {
            String message = this.objectMapper.writeValueAsString(kick);
            producer.send(new ProducerRecord<>("observer", "kicked", message), defaultCallKick(kick));
        } catch (Throwable t) {
            logger.error("Error while publishing kick notification for kick {} to channel kicks", kick.toString(), t);
        }
    }

    public void punishmentsRemoved(PunishmentRemovalAction action) {
        if (!enabled) {
            logger.info("Cant publish, kafka is disabled");
            return;
        }

        try {
            String message = this.objectMapper.writeValueAsString(action);
            producer.send(new ProducerRecord<>("observer", "removal", message), defaultCallRemoval(action));
        } catch (Throwable t) {
            logger.error("Error while publishing punishment removal notification for punishment {}", action.toString(), t);
        }
    }

    public void punishmentsWhitelisted(PunishmentWhitelistAction action) {
        if (!enabled) {
            logger.info("Cant publish, kafka is disabled");
            return;
        }

        try {
            String message = this.objectMapper.writeValueAsString(action);
            producer.send(new ProducerRecord<>("observer", "whitelist", message), defaultCallRemoval(action));
        } catch (Throwable t) {
            logger.error("Error while publishing punishment whitelist notification for punishment {}", action.toString(), t);
        }
    }

    public Callback defaultCallPunished(Punishment punishment) {
        return (metadata, exception) -> {
            if (metadata.offset() == -1) {
                logger.error("Failed to publish {} to channel punishments", punishment, exception);
            }
        };
    }

    public Callback defaultCallKick(Kick kick) {
        return (metadata, exception) -> {
            if (metadata.offset() == -1) {
                logger.error("Failed to publish {} to channel kicks", kick, exception);
            }
        };
    }

    public Callback defaultCallRemoval(Object action) {
        return (metadata, exception) -> {
            if (metadata.offset() == -1) {
                logger.error("Failed to publish {} to channel punishments", action, exception);
            }
        };
    }
}
