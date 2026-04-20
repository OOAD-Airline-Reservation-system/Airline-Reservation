package com.airline.reservation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Aviationstack aviationstack = new Aviationstack();
    private Razorpay razorpay = new Razorpay();
    private Gemini gemini = new Gemini();
    private Firebase firebase = new Firebase();
    private Aerodatabox aerodatabox = new Aerodatabox();

    public Jwt getJwt() { return jwt; }
    public Aviationstack getAviationstack() { return aviationstack; }
    public Razorpay getRazorpay() { return razorpay; }
    public Gemini getGemini() { return gemini; }
    public Firebase getFirebase() { return firebase; }
    public Aerodatabox getAerodatabox() { return aerodatabox; }

    public static class Jwt {
        private String secret;
        private long expirationMs;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpirationMs() { return expirationMs; }
        public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
    }

    public static class Aviationstack {
        private String apiKey = "";
        private String baseUrl = "http://api.aviationstack.com/v1";
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class Razorpay {
        private String keyId = "";
        private String keySecret = "";
        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
        public String getKeySecret() { return keySecret; }
        public void setKeySecret(String keySecret) { this.keySecret = keySecret; }
    }

    public static class Gemini {
        private String apiKey = "";
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public static class Aerodatabox {
        private String apiKey = "";
        private String apiHost = "aerodatabox.p.rapidapi.com";
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getApiHost() { return apiHost; }
        public void setApiHost(String apiHost) { this.apiHost = apiHost; }
    }

    public static class Firebase {
        private String serviceAccountKey = "classpath:firebase-service-account.json";
        private String projectId = "";
        public String getServiceAccountKey() { return serviceAccountKey; }
        public void setServiceAccountKey(String v) { this.serviceAccountKey = v; }
        public String getProjectId() { return projectId; }
        public void setProjectId(String v) { this.projectId = v; }
    }
}
