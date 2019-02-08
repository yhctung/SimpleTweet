package com.codepath.apps.restclienttemplate;

// import android.content.Intent;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.codepath.apps.restclienttemplate.models.Tweet;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

// import android.view.Menu;
// import android.view.MenuItem;

public class TimelineActivity extends AppCompatActivity {

    private TwitterClient client;
    private RecyclerView rvTweets;
    private TweetsAdapter adapter;
    private List<Tweet> tweets;

    static long tweetLastID = Long.MAX_VALUE;

    private final int REQUEST_CODE = 20;
    private SwipeRefreshLayout swipeContainer;

    // For infinite pagination
    private EndlessRecyclerViewScrollListener scrollListener; // store member variable for listener

    // static long tweetUID = Long.MAX_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        client = TwitterApp.getRestClient(this);

        swipeContainer = findViewById(R.id.swipeContainer);

        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        // Find the recycler view
        rvTweets = findViewById(R.id.rvTweets);

        // Initialize list of tweets and adapter from the data source
        tweets = new ArrayList<>();
        adapter = new TweetsAdapter(this, tweets);

        // Recycler View setup: layout manager and setting the adapter
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvTweets.setLayoutManager(linearLayoutManager);
        rvTweets.setAdapter(adapter);
        populateHomeTimeline();

        // For Pull-to-Refresh
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d("TwitterClient", "content is refreshed");
                populateHomeTimeline();
            }
        });

        // Retain an instance so that you can call 'resetState()' for fresh searches
        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager){
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view){
                // triggered when new data is needed
                Log.i("TwitterClient", "No more data on scroll");
                loadMoreData();
            }
        };
        // Add scroll listener to RecyclerView
        rvTweets.addOnScrollListener(scrollListener);
    }

    // make another API call to get next page of tweets and add objects to our current tweet list
    public void loadMoreData() {
        client.getNextPageOfTweets(new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.d("smile", response.toString());

                List<Tweet> tweetsToAppend = new ArrayList<>();

                for (int i = 0; i < response.length(); i++){
                    try {
                        // Make tweet object from json object
                        JSONObject jsonObject = response.getJSONObject(i);
                        Tweet tweet = Tweet.fromJson(jsonObject);
                        tweetsToAppend.add(tweet);

                        if(tweet.uid < tweetLastID){
                            tweetLastID = tweet.uid;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                // add all the tweets
                adapter.addTweets(tweetsToAppend);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                Log.e("smile", throwable.toString());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.e("smile", throwable.toString());
            }
        }, tweetLastID);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;    // we did the menu inflation, you won't have too
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.compose){
            // Tapped on compose icon
            // Toast.makeText(this, "Compose!", Toast.LENGTH_SHORT).show();
            // Navigate to a new activity
            Intent i = new Intent(this, ComposeActivity.class);
            startActivityForResult(i, REQUEST_CODE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // REQUEST_CODE is defined above
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            // Pull info out of the data Intent (Tweet)
            assert data != null;
            Tweet tweet = Parcels.unwrap(data.getParcelableExtra("tweet"));
            // Update the recycler view with this tweet
            tweets.add(0, tweet);
            adapter.notifyItemInserted(0);
            rvTweets.smoothScrollToPosition(0);
        }
    }

    private void populateHomeTimeline() {
        client.getHomeTimeline(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.d("TwitterClient", response.toString());

                List<Tweet> tweetsToAdd = new ArrayList<>();

                // Iterate through the list of tweets
                for (int i = 0; i < response.length(); i++) {
                    try {
                        // Convert each JSON object into a Tweet object
                        JSONObject jsonTweetObject = response.getJSONObject(i);
                        Tweet tweet = Tweet.fromJson(jsonTweetObject);
                        // Add the tweets into our data source
                        tweetsToAdd.add(tweet);
                            // Notify adapter one by one
                            // adapter.notifyItemInserted(tweets.size() - 1);  // added at last position

                        // setting the "lowest" id to uid of last loaded tweet
                        if (tweet.uid < tweetLastID){
                            tweetLastID = tweet.uid;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                // for pull down to refresh
                // Clear the existing data
                adapter.clear();
                // show the data we just received
                adapter.addTweets(tweetsToAdd);
                // Now we call setRefreshing(false) to signal refresh has finished
                swipeContainer.setRefreshing(false);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.e("TwitterClient", responseString);

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.e("TwitterClient", errorResponse.toString());
            }

        });
    }
}
