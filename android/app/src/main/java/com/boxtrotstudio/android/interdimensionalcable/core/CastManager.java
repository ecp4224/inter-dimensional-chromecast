package com.boxtrotstudio.android.interdimensionalcable.core;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;

import com.boxtrotstudio.android.interdimensionalcable.utils.PRunnable;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.games.GameManagerClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.lang.ref.WeakReference;
import java.util.Observable;


public class CastManager extends Observable {

    private final Context context;
    private final String APP_ID;
    private final MediaRouter mMediaRouter;
    private final MediaRouteSelector mMediaRouteSelecter;
    private final MediaRouteCallback mMediaCallback;

    private CastDevice selectedDevice;
    private GoogleApiClient mApiClient;
    private GameManagerClient gameClient;

    private PRunnable<String> connectionFailed;
    private PRunnable<CastDevice> castDeviceSelected;

    public CastManager(Context context, String APP_ID) {
        this.context = context;
        this.APP_ID = APP_ID;

        mMediaRouter = MediaRouter.getInstance(context);
        mMediaRouteSelecter = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(APP_ID))
                .build();

        mMediaCallback = new MediaRouteCallback();
    }

    public MediaRouter getMediaRouter() {
        return mMediaRouter;
    }

    public MediaRouteSelector getMediaRouteSelecter() {
        return mMediaRouteSelecter;
    }

    public MediaRouteCallback getMediaCallback() {
        return mMediaCallback;
    }

    public CastDevice getSelectedDevice() {
        return selectedDevice;
    }

    public GameManagerClient getGameClient() {
        return gameClient;
    }

    public GoogleApiClient getApiClient() {
        return mApiClient;
    }

    public boolean isConnectedToCast() {
        return gameClient != null && !gameClient.isDisposed();
    }

    public PRunnable<String> getConnectionFailed() {
        return connectionFailed;
    }

    public void setConnectionFailed(PRunnable<String> connectionFailed) {
        this.connectionFailed = connectionFailed;
    }

    public PRunnable<CastDevice> getCastDeviceSelected() {
        return castDeviceSelected;
    }

    public void setCastDeviceSelected(PRunnable<CastDevice> castDeviceSelected) {
        this.castDeviceSelected = castDeviceSelected;
    }

    public boolean isApiClientConnected() {
        return mApiClient != null && mApiClient.isConnected();
    }

    public void scan() {
        mMediaRouter.addCallback(mMediaRouteSelecter, mMediaCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    public void endScan() {
        mMediaRouter.removeCallback(mMediaCallback);
    }

    private void connectApi() {
        ConnectionListener listener = new ConnectionListener();

        Cast.CastOptions.Builder api = Cast.CastOptions.builder(selectedDevice, new CastListener());
        mApiClient = new GoogleApiClient.Builder(context)
                .addApi(Cast.API, api.build())
                .addConnectionCallbacks(listener)
                .addOnConnectionFailedListener(listener)
                .build();

        mApiClient.connect();
    }

    private void disconnect() {
        if (mApiClient != null && mApiClient.isConnected()) {
            mApiClient.disconnect();
        }

        mApiClient = null;
        setChanged();
        notifyObservers();
    }

    public void setSelectedDevice(CastDevice device) {
        this.selectedDevice = device;

        if (castDeviceSelected != null)
            castDeviceSelected.run(this.selectedDevice);

        disconnect();

        if (this.selectedDevice != null) {
            try {
                connectApi();
            } catch (Throwable t) {
                disconnect();
            }
        } else {
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        }
    }


    private class MediaRouteCallback extends MediaRouter.Callback {

        @Override
        public void onRouteSelected(MediaRouter route, MediaRouter.RouteInfo info) {
            CastDevice device = CastDevice.getFromBundle(info.getExtras());
            setSelectedDevice(device);
        }

        @Override
        public void onRouteUnselected(MediaRouter route, MediaRouter.RouteInfo info) {
            setSelectedDevice(null);
        }
    }


    private class ConnectionListener implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            setSelectedDevice(null);
            if (connectionFailed != null)
                connectionFailed.run("Failed to connect to cast device! (Reason: " + connectionResult.getErrorMessage() + ")");
        }

        @Override
        public void onConnected(Bundle bundle) {
            if (!isApiClientConnected()) {
                System.err.println("Cast connected but API is disconnected!");
                setSelectedDevice(null);
                return;
            }

            Cast.CastApi.launchApplication(mApiClient, APP_ID)
                    .setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() {
                        @Override
                        public void onResult(@NonNull Cast.ApplicationConnectionResult result) {
                            Status status = result.getStatus();
                            ApplicationMetadata appMetaData = result.getApplicationMetadata();
                            if (status.isSuccess()) {
                                //Launch Game
                                String sessionId = result.getSessionId();
                                GameManagerClient.getInstanceFor(mApiClient, sessionId)
                                        .setResultCallback(new ResultCallback<GameManagerClient.GameManagerInstanceResult>() {
                                            @Override
                                            public void onResult(@NonNull GameManagerClient.GameManagerInstanceResult gameManagerInstanceResult) {
                                                gameClient = gameManagerInstanceResult.getGameManagerClient();
                                                setChanged();
                                                notifyObservers();
                                            }
                                        });
                            } else {
                                if (connectionFailed != null)
                                    connectionFailed.run("Failed to launch game!");
                            }
                        }
                    });
        }

        @Override
        public void onConnectionSuspended(int i) {
            setSelectedDevice(null);
        }
    }

    /**
     * Cast API callbacks.
     */
    private class CastListener extends Cast.Listener {
        @Override
        public void onApplicationDisconnected(int statusCode) {
            setSelectedDevice(null);
        }
    }
}
