package com.boxtrotstudio.android.interdimensionalcable;


import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.boxtrotstudio.android.interdimensionalcable.core.BlurTransformation;
import com.boxtrotstudio.android.interdimensionalcable.core.CastManager;
import com.boxtrotstudio.android.interdimensionalcable.core.Video;
import com.boxtrotstudio.android.interdimensionalcable.utils.PRunnable;
import com.google.android.gms.cast.games.GameManagerClient;
import com.google.android.gms.cast.games.GameManagerState;
import com.google.gson.JsonObject;
import com.jgabrielfreitas.core.BlurImageView;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ControllerActivity extends AppCompatActivity {

    private static final BlurTransformation BLUR = new BlurTransformation();
    private static final String API_URL = "https://www.googleapis.com/youtube/v3/videos?id={ID}&key=AIzaSyAm7LxKXvPQIS_jQDqlYfPf7_cmTpwxpA8&part=snippet,contentDetails,statistics,status";

    @Bind(value = R.id.skip_button)
    FloatingActionButton skip;

    @Bind(value = R.id.video_title)
    TextView title;

    @Bind(value = R.id.thumbnail_image)
    ImageView thumbnail;

    private CastManager manager;
    private GameManagerClient client;
    private OkHttpClient httpClient;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_controller);
        ButterKnife.bind(this);
        this.manager = CableApplication.getCastManager();

        if (!this.manager.isConnectedToCast()) {
            Intent intent = new Intent(this, CastSelectionActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        this.client = this.manager.getGameClient();

        this.httpClient = new OkHttpClient();

        this.client.setListener(new GameManagerClient.Listener() {
            @Override
            public void onStateChanged(GameManagerState gameManagerState,
                                       GameManagerState gameManagerState1) { }

            @Override
            public void onGameMessageReceived(String s, JSONObject jsonObject) {
                try {
                    String videoId = jsonObject.getString("video");

                    getVideoInfoAsync(videoId, new PRunnable<Video>() {
                        @Override
                        public void run(final Video video) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Picasso.with(ControllerActivity.this)
                                            .load(video.getThumbnailUrl())
                                            .transform(BLUR)
                                            .into(thumbnail);

                                    title.setText(video.getTitle());
                                }
                            });
                        }
                    });

                } catch (JSONException e) {
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        });


        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("action", "voteSkip");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                client.sendGameMessage(obj);
            }
        });
    }

    private void getVideoInfoAsync(String id, final PRunnable<Video> callback) throws IOException {
        String api_url = API_URL.replace("{ID}", id);

        final Request request = new Request.Builder()
                .url(api_url)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();

                JsonObject data = CableApplication.JSON.parse(json).getAsJsonObject();

                String title = data.get("items").getAsJsonArray()
                        .get(0).getAsJsonObject()
                        .get("snippet").getAsJsonObject()
                        .get("title").getAsString();

                String thumbnailUrl = data.get("items").getAsJsonArray()
                        .get(0).getAsJsonObject()
                        .get("snippet").getAsJsonObject()
                        .get("thumbnails").getAsJsonObject()
                        .get("high").getAsJsonObject()
                        .get("url").getAsString();

                Video video = new Video(title, thumbnailUrl);
                callback.run(video);
            }
        });
    }
}
