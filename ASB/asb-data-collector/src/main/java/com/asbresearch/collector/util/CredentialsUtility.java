package com.asbresearch.collector.util;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.experimental.UtilityClass;
import org.springframework.util.ResourceUtils;

import java.io.FileInputStream;
import java.io.IOException;

@UtilityClass
public class CredentialsUtility {

    public static GoogleCredentials googleCredentials(final String credentialsLocation) throws IOException {
        try (FileInputStream serviceAccountStream = new FileInputStream(ResourceUtils.getFile(credentialsLocation))) {
            return ServiceAccountCredentials.fromStream(serviceAccountStream);
        }
    }
}
