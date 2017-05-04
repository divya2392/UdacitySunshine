package com.example.android.sunshine.data;

import android.provider.BaseColumns;

/**
 * Created by divya on 04/05/17.
 */

/**
 * Defines table and column names for the weather database. This class is not necessary, but keeps
 * the code organized.
 */
public class WeatherContract {

    /* Inner class that defines the table contents of the weather table */
    public static final class WeatherEntry implements BaseColumns {

        /* Used internally as the name of our weather table. */
        public static final String TABLE_NAME = "weather";

        public static final String COLUMN_DATE = "date";

        /* Weather ID as returned by API, used to identify the icon to be used */
        public static final String COLUMN_WEATHER_ID = "weather_id";

        /* Min and max temperatures in °C for the day (stored as floats in the database) */
        public static final String COLUMN_MIN_TEMP = "min";
        public static final String COLUMN_MAX_TEMP = "max";

        /* Humidity is stored as a float representing percentage */
        public static final String COLUMN_HUMIDITY = "humidity";

        /* Pressure is stored as a float representing percentage */
        public static final String COLUMN_PRESSURE = "pressure";

        /* Wind speed is stored as a float representing wind speed in mph */
        public static final String COLUMN_WIND_SPEED = "wind";

        /*
        * Degrees are meteorological degrees (e.g, 0 is north, 180 is south).
        * Stored as floats in the database.
        *
        * Note: These degrees are not to be confused with temperature degrees of the weather.
        */
        public static final String COLUMN_DEGREES = "degrees";

    }
}
