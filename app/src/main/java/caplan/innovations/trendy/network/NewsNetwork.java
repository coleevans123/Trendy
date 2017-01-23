package caplan.innovations.trendy.network;

import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import caplan.innovations.trendy.R;
import caplan.innovations.trendy.application.TrendyApplication;
import caplan.innovations.trendy.model.NewsItem;
import caplan.innovations.trendy.utilities.JsonExtractor;

/**
 * Created by Corey on 1/21/2017.
 * Project: Trendy
 * <p></p>
 * Purpose of Class:
 */
public class NewsNetwork {

    private static final String TAG = NewsNetwork.class.getSimpleName();

    private static final int TIMEOUT_THIRTY_SECONDS = 30;

    private static final String BASE_URL = "https://newsapi.org/v1/articles";
    private static final String PARAM_API_KEY = "apiKey";
    private static final String PARAM_SOURCE = "source";

    /**
     * In the JSON object response from the api --> maps to an array of JSON objects
     */
    private static final String RESPONSE_ARTICLES = "articles";

    private static final String SOURCE_GOOGLE = "google-news";
    private static final String SOURCE_BBC = "bbc-news";

    /**
     * Gets the news from BBC on this thread and blocks until it does so.
     *
     * @return The news items that the network retrieved or null if an error occurred.
     * For simplicity's sake, we are not going to distinguish between the
     * different types of errors. Some errors include loss of network connection,
     * or invalid JSON being returned from the server.
     */
    @Nullable
    public static JSONArray getBbcNewsAndBlock() {
        return getNews(NewsItem.NEWS_BBC);
    }

    /**
     * Gets the news from Google on this thread and blocks until it does so.
     *
     * @return The news items that the network retrieved or null if an error occurred.
     * For simplicity's sake, we are not going to distinguish between the
     * different types of errors. Some errors include loss of network connection,
     * or invalid JSON being returned from the server.
     */
    @Nullable
    public static JSONArray getGoogleNewsAndBlock() {
        return getNews(NewsItem.NEWS_GOOGLE);
    }

    @Nullable
    private static JSONArray getNews(@NewsItem.NewsType int newsType) {
        String url = buildUrl(newsType);

        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        Log.d(TAG, "getNews: Sending a request to: " + url);
        JsonObjectRequest request = new JsonObjectRequest(url, null, future, future);
        TrendyRequestQueue.addToRequestQueue(request);

        try {
            // Beware, this blocks this currently running thread until the result is retrieved.
            JSONObject jsonObject = future.get(TIMEOUT_THIRTY_SECONDS, TimeUnit.SECONDS);

            try {
                Log.d(TAG, "onResponse: Received response: " + jsonObject.toString(4));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JSONArray jsonArray = new JsonExtractor(jsonObject)
                    .getJsonArray(RESPONSE_ARTICLES);

            // Insert the date at which the item was retrieved for sorting purposes
            injectDateIntoNewsObjects(jsonArray);

            return jsonArray;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Log.e(TAG, "getNews: ", e);

            return null;
        }

    }

    private static String buildUrl(@NewsItem.NewsType int newsType) {
        String apiKey = TrendyApplication.context().getString(R.string.news_api_key);
        String url = String.format("%s?%s=%s", BASE_URL, PARAM_API_KEY, apiKey);
        switch (newsType) {
            case NewsItem.NEWS_GOOGLE:
                url = String.format("%s&%s=%s", url, PARAM_SOURCE, SOURCE_GOOGLE);
                break;
            case NewsItem.NEWS_BBC:
                url = String.format("%s&%s=%s", url, PARAM_SOURCE, SOURCE_BBC);
                break;
            default:
                throw new IllegalArgumentException("Invalid newsType, found: " + newsType);
        }
        return url;
    }

    private static void injectDateIntoNewsObjects(@Nullable JSONArray jsonArray) {
        if (jsonArray != null) {
            for (int i = jsonArray.length() - 1; i >= 0; i--) {
                try {
                    jsonArray.getJSONObject(i)
                            .put("my_date", Calendar.getInstance().getTimeInMillis());
                } catch (JSONException e) {
                    Log.d(TAG, "getNews: ", e);
                }
            }
        }
    }

}
