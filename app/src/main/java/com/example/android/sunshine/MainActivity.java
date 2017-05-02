/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.sunshine.data.SunshinePreferences;
import com.example.android.sunshine.utilities.NetworkUtils;
import com.example.android.sunshine.utilities.OpenWeatherJsonUtils;

import java.io.IOException;
import java.net.URL;

import static android.R.attr.data;
import static android.R.attr.start;

public class MainActivity extends AppCompatActivity implements ForecastAdapter.ForecastAdapterOnClickHandler, LoaderManager.LoaderCallbacks<String[]>, SharedPreferences.OnSharedPreferenceChangeListener{

    private static final String TAG = MainActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private ForecastAdapter mForecastAdapter;
    //Create a field to store the weather display TextView
    private TextView mWeatherTextView;
    private TextView mErrorMessageDisplay;
    private ProgressBar mLoadingIndicator;

    private static final int FORECAST_LOADER_ID = 0;
    private static boolean PREFERENCES_HAVE_BEEN_UPDATED = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_forecast);
        //Use findViewById to get a reference to the weather display TextView
        mWeatherTextView = (TextView) findViewById(R.id.tv_weather_data);
        mErrorMessageDisplay = (TextView) findViewById(R.id.tv_error_message_display);
               LinearLayoutManager layoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mForecastAdapter = new ForecastAdapter(this);
        mRecyclerView.setAdapter(mForecastAdapter);

        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_loading_indicator);

        int loaderId = FORECAST_LOADER_ID;
        LoaderManager.LoaderCallbacks<String[]> callback = MainActivity.this;
        Bundle bundleForLoader = null;
        getSupportLoaderManager().initLoader(loaderId, bundleForLoader, callback);

        /*
         * Register MainActivity as an OnPreferenceChangedListener to receive a callback when a
         * SharedPreference has changed. Please note that we must unregister MainActivity as an
         * OnSharedPreferenceChanged listener in onDestroy to avoid any memory leaks.
         */

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    /**
          * OnStart is called when the Activity is coming into view. This happens when the Activity is
          * first created, but also happens when the Activity is returned to from another Activity. We
          * are going to use the fact that onStart is called when the user returns to this Activity to
          * check if the location setting or the preferred units setting has changed. If it has changed,
          * we are going to perform a new query.
          */

    @Override
    protected void onStart() {
        super.onStart();

        /*
         * If the preferences for location or units have changed since the user was last in
         * MainActivity, perform another query and set the flag to false.
         *
         * This isn't the ideal solution because there really isn't a need to perform another
         * GET request just to change the units, but this is the simplest solution that gets the
         * job done for now. Later in this course, we are going to show you more elegant ways to
         * handle converting the units from celsius to fahrenheit and back without hitting the
         * network again by keeping a copy of the data in a manageable format.
         */
        if(PREFERENCES_HAVE_BEEN_UPDATED)
        {
            Log.d(TAG,"onstart : pref updated");
            getSupportLoaderManager().restartLoader(FORECAST_LOADER_ID,null,this);
            PREFERENCES_HAVE_BEEN_UPDATED = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private void showWeatherDataView() {

        mErrorMessageDisplay.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showErrorMessage() {
        mRecyclerView.setVisibility(View.INVISIBLE);
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }


    private void openLocationInMap()
    {
        //Use preferred location rather than a default location to display in the map
        String addressString = SunshinePreferences.getPreferredWeatherLocation(this);
        Uri geoLocation = Uri.parse("geo:0,0?q=" + addressString);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if(intent.resolveActivity(getPackageManager()) != null)
        {
            startActivity(intent);
        }else
        {
            Log.d(TAG, "Couldn't call " + geoLocation.toString()
                    + ", no receiving apps installed!");
        }

        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        /* Use AppCompatActivity's method getMenuInflater to get a handle on the menu inflater */
        MenuInflater inflater = getMenuInflater();
        /* Use the inflater's inflate method to inflate our menu layout to this menu */
        inflater.inflate(R.menu.forecast, menu);
        /* Return true so that the menu is displayed in the Toolbar */
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            invalidateData();
            getSupportLoaderManager().restartLoader(FORECAST_LOADER_ID,null,this);
            return true;
        }

        if(id == R.id.action_map)
        {
            openLocationInMap();
            return true;
        }
        if (id == R.id.action_settings)
        {
            Intent startsettingActivity = new Intent(this,SettingsActivity.class);
            startActivity(startsettingActivity);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void invalidateData() {
        mForecastAdapter.setweatherData(null);
    }

   /* private void loadWeatherData() {
        showWeatherDataView();
        String location = SunshinePreferences.getPreferredWeatherLocation(this);
        new FetchWeatherTask().execute(location);
    }*/

    @Override
    public void onClick(String weatherForDay) {
        Context context = this;
        Class destinationClass = DetailActivity.class;
        Intent intentToStartDetailActivity = new Intent(context, destinationClass);
        intentToStartDetailActivity.putExtra(Intent.EXTRA_TEXT,weatherForDay);
        startActivity(intentToStartDetailActivity);
    }

    @Override
    public Loader<String[]> onCreateLoader(int id, Bundle args) {
        return new AsyncTaskLoader<String[]>(this) {

            String[] mWeatherData;

            @Override
            protected void onStartLoading() {
               if(mWeatherData !=null)
               {
                   deliverResult(mWeatherData);
               }else
               {
                   mLoadingIndicator.setVisibility(View.VISIBLE);
                   forceLoad();
               }
            }

            @Override
            public String[] loadInBackground() {
                String locationQuery = SunshinePreferences.getPreferredWeatherLocation(MainActivity.this);

                URL weatherRequestUrl = NetworkUtils.buildUrl(locationQuery);

                try {
                    String jsonWeatherResponse = NetworkUtils
                            .getResponseFromHttpUrl(weatherRequestUrl);

                    String[] simpleJsonWeatherData = OpenWeatherJsonUtils
                            .getSimpleWeatherStringsFromJson(MainActivity.this, jsonWeatherResponse);

                    return simpleJsonWeatherData;

                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }

            }

            @Override
            public void deliverResult(String[] data) {
                mWeatherData = data;
                super.deliverResult(data);
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<String[]> loader, String[] data) {
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mForecastAdapter.setweatherData(data);
        if (data != null) {
            showWeatherDataView();
        } else {
            showErrorMessage();
        }
    }

    @Override
    public void onLoaderReset(Loader<String[]> loader) {

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        PREFERENCES_HAVE_BEEN_UPDATED = true;

    }


    /*public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected String[] doInBackground(String... params) {

            *//* If there's no zip code, there's nothing to look up. *//*
            if (params.length == 0) {
                return null;
            }

            String location = params[0];
            URL weatherRequestUrl = NetworkUtils.buildUrl(location);

            try {
                String jsonWeatherResponse = NetworkUtils
                        .getResponseFromHttpUrl(weatherRequestUrl);

                String[] simpleJsonWeatherData = OpenWeatherJsonUtils
                        .getSimpleWeatherStringsFromJson(MainActivity.this, jsonWeatherResponse);

                return simpleJsonWeatherData;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] weatherData) {
            mLoadingIndicator.setVisibility(View.INVISIBLE);
            if (weatherData != null) {
                showWeatherDataView();
                mForecastAdapter.setweatherData(weatherData);
            } else {
                showErrorMessage();
            }
        }
    }*/
}