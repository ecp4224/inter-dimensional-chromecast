package com.boxtrotstudio.android.interdimensionalcable;


import android.app.Application;

import com.boxtrotstudio.android.interdimensionalcable.core.CastManager;
import com.google.android.gms.cast.games.GameManagerClient;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

public class CableApplication extends Application {
    private CastManager manager;
    private static CableApplication instance;

    public static final JsonParser JSON = new JsonParser();

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        manager = new CastManager(this, getResources().getString(R.string.app_id));
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (this.manager.isConnectedToCast()) {
            final GameManagerClient client = this.manager.getGameClient();

            JSONObject obj = new JSONObject();
            try {
                obj.put("reason", "App terminated");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            client.sendPlayerQuitRequest(obj);
        }
    }

    public static CableApplication getInstance() {
        return instance;
    }

    public static CastManager getCastManager() {
        return instance.manager;
    }

    public CastManager getManager() {
        return manager;
    }
}
