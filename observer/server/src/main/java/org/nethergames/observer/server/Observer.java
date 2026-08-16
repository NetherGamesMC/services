package org.nethergames.observer.server;

import io.javalin.Javalin;
import io.javalin.micrometer.MicrometerPlugin;
import io.javalin.openapi.OpenApiContact;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.OpenApiPluginConfiguration;
import io.javalin.openapi.plugin.redoc.ReDocConfiguration;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.sentry.Sentry;
import lombok.Getter;
import org.nethergames.observer.server.manager.*;
import org.nethergames.observer.server.storage.S3StorageProvider;
import org.nethergames.observer.server.util.Configuration;
import org.nethergames.observer.server.util.JavalinUtil;
import org.nethergames.observer.server.util.MetricsAttacher;
import org.nethergames.utils.deployment.GithubDataFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class Observer {

    private static Observer observer;
    private final Logger logger = LoggerFactory.getLogger("Observer");
    private final MatchIdManager trackingManager = new MatchIdManager();
    private final PunishmentManager punishmentManager;
    private final MongoManager mongoManager;
    private final PlayerManager playerManager = new PlayerManager();
    private final KafkaManager kafkaManager = new KafkaManager();
    private final ReportManager reportManager;
    private final Javalin javalin;
    private final PrometheusMeterRegistry metricsRegistry;
    private final GithubDataFile dataFile = new GithubDataFile();
    private final EvidenceManager evidenceManager;
    private final TracingManager tracingManager;
    private final S3StorageProvider storageProvider;
    private final S3StorageProvider altStorageProvider;
    private final SocialManager socialManager;
    private final DatabaseManager databaseManager;
    private final APIManager apiManager;

    @Getter
    private final static String environment;
    @Getter
    private final static String serverName;

    static {
        if (System.getenv("ENVIRONMENT") != null) {
            environment = System.getenv("ENVIRONMENT");
        } else {
            environment = "Testing";
        }

        serverName = System.getenv("HOSTNAME");
    }

    public Observer() {
        observer = this;

        storageProvider = new S3StorageProvider(Configuration.S3_HOST, Configuration.S3_BUCKET, Configuration.S3_REGION, Configuration.S3_SECRET_ID, Configuration.S3_SECRET_KEY);
        altStorageProvider = new S3StorageProvider(Configuration.SCALEWAY_S3_HOST, Configuration.SCALEWAY_S3_BUCKET, Configuration.SCALEWAY_S3_REGION, Configuration.SCALEWAY_S3_SECRET_ID, Configuration.SCALEWAY_S3_SECRET_KEY);
        mongoManager = new MongoManager();
        socialManager = new SocialManager();
        reportManager = new ReportManager(this);
        evidenceManager = new EvidenceManager(this);
        punishmentManager = new PunishmentManager(this);
        tracingManager = new TracingManager(this);
        metricsRegistry = MetricsAttacher.attach(this);
        databaseManager = new DatabaseManager();
        apiManager = new APIManager();


        if (Configuration.SENTRY_DSN != null && !Configuration.SENTRY_DSN.isEmpty()) {
            logger.info("Sentry monitoring is enabled.");

            Sentry.init(options -> {
                options.setSampleRate(0.2);
                options.setTracesSampleRate(0.2);

                options.setDsn(Configuration.SENTRY_DSN);

                options.setEnvironment(environment);
                options.setRelease(Observer.getObserver().getDataFile().getCommit());
                options.setServerName(options.getServerName());
            });
        }

        logger.warn("Observer initialized, starting web server.");

        javalin = Javalin.create(config -> {
            OpenApiPluginConfiguration openApiConfiguration = new OpenApiPluginConfiguration();


            config.registerPlugin(new OpenApiPlugin(openApiConfig -> {
                openApiConfiguration.withDefinitionConfiguration((version, definition) -> definition.withInfo(openApiInfo -> {
                    OpenApiContact openApiContact = new OpenApiContact();
                    openApiContact.setName("Tobias Grether");
                    openApiContact.setEmail("tobias@nethergames.org");

                    openApiInfo.setTitle("Observer - Centralized Moderation Endpoint");
                    openApiInfo.setDescription("Observer is a centralized Moderation endpoint for the NetherGames network.");
                    openApiInfo.setContact(openApiContact);
                    openApiInfo.setVersion("2.1.9");
                }));
            }));
            config.registerPlugin(new SwaggerPlugin(swaggerConfiguration -> {}));
            config.registerPlugin(new ReDocPlugin(reDocConfiguration -> {}));
            config.registerPlugin(new MicrometerPlugin(micrometerPluginConfig -> {
                micrometerPluginConfig.registry = metricsRegistry;
            }));

            config.router.apiBuilder(PathRegistry::init);
        }).exception(Exception.class, JavalinUtil::handleException).after(JavalinUtil::afterHandler);


        try {
            dataFile.loadApplicationVersion();
        } catch (Throwable t) {
            logger.error("Error while determining versions", t);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.warn("Shutting down Observer..");

            javalin.stop();
            kafkaManager.shutdown();
        }));

        javalin.start(8080);
    }

    public static Observer getObserver() {
        return observer;
    }
}
