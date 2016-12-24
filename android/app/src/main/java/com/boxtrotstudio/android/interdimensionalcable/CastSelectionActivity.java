package com.boxtrotstudio.android.interdimensionalcable;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.boxtrotstudio.android.interdimensionalcable.core.CastManager;
import com.boxtrotstudio.android.interdimensionalcable.utils.PRunnable;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.games.GameManagerClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;

import butterknife.Bind;
import butterknife.ButterKnife;

public class CastSelectionActivity extends AppCompatActivity implements Observer {

    @Bind(R.id.connect_image)
    ImageView connectImage;

    @Bind(R.id.connect_text)
    TextView connectText;

    @Bind(R.id.connect_progress)
    ProgressBar progress;

    @Bind(R.id.toolbar)
    Toolbar toolbar;


    private CastManager manager;
    private Timer castCheckerTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cast_selection);
        ButterKnife.bind(this);

        toolbar.setTitle("Inter-Dimensional Cable");
        setSupportActionBar(toolbar);

        this.manager = CableApplication.getCastManager();
        this.castCheckerTimer = new Timer();

        this.castCheckerTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(CAST_CHECKER);
            }
        }, 1, 500);

        this.manager.setCastDeviceSelected(new PRunnable<CastDevice>() {
            @Override
            public void run(CastDevice obj) {
                onSelected(obj);
            }
        });

        this.manager.setConnectionFailed(new PRunnable<String>() {
            @Override
            public void run(String obj) {
                Toast.makeText(CastSelectionActivity.this, obj, Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.cast_menu, menu);
        MenuItem item = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat.getActionProvider(item);
        mediaRouteActionProvider.setRouteSelector(manager.getMediaRouteSelecter());
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        manager.scan();
        manager.addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        manager.endScan();
        manager.deleteObserver(this);
    }

    private void onSelected(CastDevice device) {
        if (device != null) {
            connectText.setVisibility(View.INVISIBLE);
            progress.setVisibility(View.VISIBLE);
        } else {
            connectText.setVisibility(View.VISIBLE);
            progress.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        if (this.manager.isConnectedToCast()) {
            final GameManagerClient client = this.manager.getGameClient();
            askForName(new PRunnable<String>() {
                @Override
                public void run(String obj) {
                    try {
                        JSONObject object = new JSONObject();
                        object.put("name", obj);
                        client.sendPlayerAvailableRequest(object);

                        Intent readyActivity = new Intent(CastSelectionActivity.this, ControllerActivity.class);
                        startActivity(readyActivity);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void askForName(final PRunnable<String> name) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("What's your name?");

        final EditText text = new EditText(this);
        text.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(text);

        builder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                name.run(text.getText().toString());
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                manager.setSelectedDevice(null);
            }
        });

        builder.show();
    }

    private final Runnable CAST_CHECKER = new Runnable() {
        @Override
        public void run() {
            if (manager.getMediaRouter().getRoutes().size() <= 1) {
                connectImage.setImageDrawable(getResources().getDrawable(R.drawable.alert));
                connectText.setText("No chromecast device found!");
            } else {
                connectImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_chromecast_cast_button_icon));
                connectText.setText(getResources().getText(R.string.CONNECT_TEXT));
            }
        }
    };
}
