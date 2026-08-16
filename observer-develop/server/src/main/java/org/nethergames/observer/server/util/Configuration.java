package org.nethergames.observer.server.util;

public class Configuration {
    public static final String SENTRY_DSN = System.getenv("SENTRY_DSN");

    public static final String S3_HOST = System.getenv("PERM_HOST");
    public static final String S3_BUCKET = System.getenv("PERM_BUCKET");
    public static final String S3_REGION = System.getenv("PERM_REGION");
    public static final String S3_SECRET_ID = System.getenv("PERM_SECRET_ID");
    public static final String S3_SECRET_KEY = System.getenv("PERM_SECRET_KEY");

    public static final String SCALEWAY_S3_HOST = System.getenv("TEMP_HOST");
    public static final String SCALEWAY_S3_BUCKET = System.getenv("TEMP_BUCKET");
    public static final String SCALEWAY_S3_REGION = System.getenv("TEMP_REGION");
    public static final String SCALEWAY_S3_SECRET_ID = System.getenv("TEMP_SECRET_ID");
    public static final String SCALEWAY_S3_SECRET_KEY = System.getenv("TEMP_SECRET_KEY");

    public static final String DATABASE_NAME = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "Observer";
    public static final String MONGO_URI = System.getenv("MONGO_URI") != null ? System.getenv("MONGO_URI") : "mongodb";

    public static final String SOCIAL_HOST = System.getenv("SOCIAL_HOST") != null ? System.getenv("SOCIAL_HOST") : "social";


    public static final String PUBLIC_API_TOKEN = System.getenv("PUBLIC_API_TOKEN") != null ? System.getenv("PUBLIC_API_TOKEN") : null;
    public static final String PUBLIC_API_HOST = System.getenv("PUBLIC_API_HOST") != null ? System.getenv("PUBLIC_API_HOST") : "https://apiv2.nethergames.org";
}
