package com.asb.analytics.api.adapters;

import com.asb.analytics.domain.betfair.AppVersion;
import com.asb.analytics.domain.betfair.DeveloperAppKey;
import com.asb.analytics.domain.betfair.Wallet;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
public class AccountAdapter {

    public static Wallet getWallet(String json) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(json, new TypeReference<Wallet>(){});
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getApplicationId(String json) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            List<DeveloperAppKey> keys = mapper.readValue(json, new TypeReference<List<DeveloperAppKey>>(){});

            for (DeveloperAppKey key : keys) {

                Optional<AppVersion> optional = key.getAppVersions().stream()
                        .filter(k -> k.isActive() && !k.isDelayData()).findFirst();

                if (optional.isPresent())
                    return optional.get().getApplicationKey();

                optional = key.getAppVersions().stream()
                        .filter(AppVersion::isActive).findFirst();

                String appKey = optional.
                        map(AppVersion::getApplicationKey).orElse(null);

                if (appKey != null)
                    return appKey;
            }

            return null;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
