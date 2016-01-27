package com.jonathonwmoore.jwm.howtocs496;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements PlayerNotificationCallback, ConnectionStateCallback {
    private static final String CLIENT_ID = "5e9492e44fec4eaa824eb9f6ce749582";
    private static final String REDIRECT_URI = "cs496-howto-app-login://callback";
    private static final int REQUEST_CODE = 5024;
    private static final String API_KEY = "5RXPP7LSQLSY4SJGW";
    private static final String BASE_URL = "http://developer.echonest.com/api/v4/";
    private static final String API_TARGET_KEY = "com_jonathonwmoore_jwm_howtocs496_api_target";

    // Spotify Player object
    private Player mPlayer;

    // variables for playlist
    private JSONArray echoNestPlaylist = new JSONArray();
    private List<String> songPlaylist = new ArrayList();

    // UI elements
    private Button pauseButton, nextButton, submitButton;
    private EditText artistEntry;
    private ImageView displayAlbum;
    private TextView displayResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        submitButton = (Button) findViewById(R.id.main_submit_playlist);
        artistEntry = (EditText) findViewById(R.id.main_input_artist);
        displayAlbum = (ImageView) findViewById(R.id.album_image);
        displayResult = (TextView) findViewById(R.id.response_text);
        pauseButton = (Button) findViewById(R.id.main_button_pause);
        nextButton = (Button) findViewById(R.id.main_button_next);
        pauseButton.setTag(1);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int status = (Integer) v.getTag();
                if (status == 1) {
                    mPlayer.pause();
                    pauseButton.setText("Play");
                    v.setTag(0);
                } else {
                    mPlayer.resume();
                    pauseButton.setText("Pause");
                    v.setTag(1);
                }
            }
        });

        // Logging the user in
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
        // end login section
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addPlayerNotificationCallback(MainActivity.this);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
        Toast toast = Toast.makeText(
                getApplicationContext(),
                "Welcome!",
                Toast.LENGTH_SHORT
        );
        toast.show();
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
        Toast toast = Toast.makeText(
                getApplicationContext(),
                "Good Bye!",
                Toast.LENGTH_SHORT
        );
        toast.show();
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("MainActivity", "Login failed");
        Toast toast = Toast.makeText(
                getApplicationContext(),
                "There was a problem with logging you in to Spotify",
                Toast.LENGTH_SHORT
        );
        toast.show();
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
        if (eventType == EventType.TRACK_CHANGED) {
            Toast toast = Toast.makeText(
                    getApplicationContext(),
                    "Now playing next song",
                    Toast.LENGTH_SHORT
            );
            toast.show();

            try {
                // playerState.trackUri gives result as "spotify:track:<id>"
                String trackID = playerState.trackUri.substring(14);

                // asynchronous web request to GET track metadata
                String imageURI = "https://api.spotify.com/v1/tracks/" + trackID;
                new GetRequestCall().execute("spotify", imageURI);
            } catch (Exception e) {
                e.printStackTrace();
                // exception will occur if we try to get playerState.trackUri when there are no tracks in player
            }
        }
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("MainActivity", "Playback error occured: " + errorType.name());
    }

    @Override
    public void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void nextSong(View view) {
        mPlayer.skipToNext();
    }

    public void submitPlaylist(View view) {
        // validate that artist field is not blank
        artistEntry.setError(null);
        String artistName = artistEntry.getText().toString();
        if (artistName.length() == 0) {
            artistEntry.setError("Name is required to generate playlist");
        } else {
            try {
                artistName = URLEncoder.encode(artistName, "UTF-8");
                // create GET url for Echo Nest
                String playlistRequestURL = BASE_URL + "playlist/static?api_key=" + API_KEY
                        + "&format=json"
                        + "&results=100"
                        + "&artist=" + artistName
                        + "&type=artist-radio"
                        + "&limited_interactivity=true"
                        + "&bucket=id:spotify"
                        + "&bucket=tracks"
                        + "&limit=true";
                new GetRequestCall().execute("echonest", playlistRequestURL);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Toast toast = Toast.makeText(
                        getApplicationContext(),
                        "There was an error submitting the artist you entered",
                        Toast.LENGTH_SHORT
                );
                toast.show();
            }
        }
    }

    public void parsePlaylist(JSONObject resultobj) {
        String currentTrack = "";
        try {
            String message = resultobj.getJSONObject("response").getJSONObject("status").getString("message");
            if (message.toLowerCase().equals("success")) {
                mPlayer.clearQueue();
                mPlayer.skipToNext();
                echoNestPlaylist = resultobj.getJSONObject("response").getJSONArray("songs");
                for (int i = 0; i < echoNestPlaylist.length(); i++) {
                    currentTrack = echoNestPlaylist.getJSONObject(i).getJSONArray("tracks").getJSONObject(0).getString("foreign_id");
                    songPlaylist.add(currentTrack);
                    mPlayer.queue(currentTrack);
                }
            } else {
                Toast toast = Toast.makeText(
                        getApplicationContext(),
                        "Unable to create a radio playlist from that artist name.  Please try again.",
                        Toast.LENGTH_SHORT
                );
                toast.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** parsing spotify web API metadata */
    public void parseMetadata(JSONObject resultobj) {
        String currentTrack = "";
        String currentAlbum = "";
        String currentArtist = "";
        String currentAlbumImageURL = "";
        URL imageURL;

        // get track name
        try {
            currentTrack = "Track: " + resultobj.get("name").toString();
        } catch (Exception e) {
            e.printStackTrace();
            currentTrack = "Unable to get Track Name";
        }

        // get album name
        try {
            currentAlbum = "Album: " + resultobj.getJSONObject("album").get("name").toString();
        } catch (Exception e) {
            e.printStackTrace();
            currentAlbum = "Unable to get Album Name";
        }

        // get artist name
        try {
            currentArtist = "Artist: " + resultobj.getJSONArray("artists").getJSONObject(0).get("name").toString();
        } catch (Exception e) {
            e.printStackTrace();
            currentArtist = "Unable to get Artist Name";
        }

        // get album image URL
        try {
            currentAlbumImageURL = resultobj.getJSONObject("album").getJSONArray("images").getJSONObject(0).get("url").toString();
        } catch (Exception e) {
            e.printStackTrace();
            currentAlbumImageURL = null;
        }

        // display artist information
        displayResult.setText(
                currentTrack + "\n" +
                currentAlbum + "\n" +
                currentArtist
        );

        // Picasso library will load image from URL with this line
        Picasso.with(MainActivity.this).load(currentAlbumImageURL).into(displayAlbum);
    }

    /** class for asynchronous HttpURLConnection call */
    private class GetRequestCall extends AsyncTask<String, Integer, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... params) {
            HttpURLConnection conn = null;
            URL url;
            JSONObject responseObject = new JSONObject();
            try {
                url = new URL(params[1]);
                conn = (HttpURLConnection) url.openConnection();
                String responseString = readStream(conn.getInputStream());
                responseObject = new JSONObject(responseString);
                responseObject.put(API_TARGET_KEY, params[0]);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return responseObject;
        }

        private String readStream(InputStream is) {
            BufferedReader reader = null;
            StringBuffer response = new StringBuffer();
            try {
                reader = new BufferedReader(new InputStreamReader(is));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return response.toString();
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);
            String findTarget = "";
            try {
                findTarget = result.get(API_TARGET_KEY).toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (findTarget.equals("echonest")) {
                parsePlaylist(result);
            } else if (findTarget.equals("spotify")) {
                parseMetadata(result);
            } else {
                Toast toast = Toast.makeText(
                        getApplicationContext(),
                        "Error parsing result from server",
                        Toast.LENGTH_SHORT
                );
                toast.show();
            }
        }
    }

}
