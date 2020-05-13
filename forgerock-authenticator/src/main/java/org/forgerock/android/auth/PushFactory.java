/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.nimbusds.jose.JOSEException;

import org.forgerock.android.auth.exception.MechanismCreationException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Responsible for generating instances of {@link Push}.
 *
 * Understands the concept of a version number associated with a Push mechanism
 * and will parse the URI according to this.
 */
class PushFactory extends MechanismFactory {

    /* The FCM device's registration token used for Push notifications */
    private String fcmToken;

    private final PushParser parser = new PushParser();

    private static final String TAG = PushFactory.class.getSimpleName();

    /**
     * Creates the MechanismFactory and loads the available mechanism information.
     *
     * @param context The application context
     * @param storageClient The storage system
     * @param fcmToken The FCM device registration token an Android app needs to receive push messages
     */
    public PushFactory(Context context, StorageClient storageClient, String fcmToken) {
        super(context, storageClient);
        this.fcmToken = fcmToken;
    }

    @Override
    protected void createFromUriParameters(int version, @NonNull String mechanismUID, @NonNull Map<String,
            String> map, @NonNull FRAListener<Mechanism> listener) {

        // Check FCM device token
        if(this.fcmToken == null) {
            listener.onException(new MechanismCreationException("Invalid FCM token."));
            return;
        }

        // Check if Google Play Services is enabled
        if (!checkGooglePlayServices()) {
            listener.onException(new MechanismCreationException("Google Play Services not enabled."));
            return;
        }

        if (version == 1) {
            this.buildPushMechanism(mechanismUID, map, listener);
        } else {
            Logger.warn(TAG, "Unknown version: %s", version);
            listener.onException(new MechanismCreationException("Unknown version: " + version));
        }
    }

    @Override
    protected MechanismParser getParser() {
        return this.parser;
    }

    private void buildPushMechanism(String mechanismUID, Map<String,
            String> map, FRAListener<Mechanism> listener) {
        String issuer = map.get(MechanismParser.ISSUER);
        String accountName = map.get(MechanismParser.ACCOUNT_NAME);
        String registrationEndpoint = map.get(PushParser.REGISTRATION_ENDPOINT);
        String authenticationEndpoint = map.get(PushParser.AUTHENTICATION_ENDPOINT);
        String base64Secret = map.get(PushParser.SHARED_SECRET);
        String base64Challenge = map.get(PushParser.CHALLENGE);
        String amlbCookie = map.get(PushParser.AM_LOAD_BALANCER_COOKIE);
        String messageId = getFromMap(map, PushParser.MESSAGE_ID, null);

        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", this.fcmToken);
        payload.put("deviceType", "android");
        payload.put("communicationType", "gcm");
        payload.put("mechanismUid", mechanismUID);
        payload.put("response", PushResponder.generateChallengeResponse(base64Secret, base64Challenge));

        PushResponder.getInstance().registration(registrationEndpoint, amlbCookie, base64Secret,
                messageId, payload, new FRAListener<Integer>() {
            @Override
            public void onSuccess(Integer returnCode) {
                if (returnCode != HTTP_OK) {
                    listener.onException(new MechanismCreationException("Communication with " +
                            "server returned " + returnCode + " code."));
                } else {
                    Mechanism push = Push.builder()
                            .setMechanismUID(mechanismUID)
                            .setIssuer(issuer)
                            .setAccountName(accountName)
                            .setAuthenticationEndpoint(authenticationEndpoint)
                            .setRegistrationEndpoint(registrationEndpoint)
                            .setSecret(base64Secret)
                            .build();
                    listener.onSuccess(push);
                }
            }

            @Override
            public void onException(Exception e) {
                    listener.onException(new MechanismCreationException("Failed to register with server. "
                            + e.getLocalizedMessage(), e));
            }
        });

    }

    @VisibleForTesting
    boolean checkGooglePlayServices() {
        Context context = getContext();
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.showErrorNotification(context, resultCode);
            } else {
                Logger.warn(TAG, "Error loading Google Play Services.");
            }
            return false;
        }
        return true;
    }

}
