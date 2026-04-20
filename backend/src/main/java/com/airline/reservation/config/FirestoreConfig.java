package com.airline.reservation.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

/**
 * Initializes Firebase Admin SDK and exposes a Firestore bean.
 * SRP: sole responsibility is Firebase bootstrap.
 * DIP: depends on AppProperties abstraction for config values.
 */
@Configuration
public class FirestoreConfig {

    private final AppProperties appProperties;
    private final ResourceLoader resourceLoader;

    public FirestoreConfig(AppProperties appProperties, ResourceLoader resourceLoader) {
        this.appProperties = appProperties;
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public Firestore firestore() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            var resource = resourceLoader.getResource(
                    appProperties.getFirebase().getServiceAccountKey());
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                    .setProjectId(appProperties.getFirebase().getProjectId())
                    .build();
            FirebaseApp.initializeApp(options);
        }
        return FirestoreClient.getFirestore();
    }
}
