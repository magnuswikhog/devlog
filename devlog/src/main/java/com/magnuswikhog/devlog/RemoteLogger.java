package com.magnuswikhog.devlog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.jetbrains.annotations.NonNls;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;


@SuppressWarnings("HardCodedStringLiteral")
public class RemoteLogger {
	private static final String TAG = "RemoteLogger";

	public interface RemoteLoggerInterface{
	    void onStoreSuccess();
	    void onStoreFailure();
    }



    private static final String STORE_LOG_SCRIPT_URL = "https://apis.magnuswikhog.com/remote-logger/1.0/store_log.php";
    private static final String SCRIPT_PASSWORD = "ugu6765t(iyo07rdiKIT(Rtyfjhbkjsdgs8uo32f";
    private static final String PREFERENCE_KEY_SEQUENCE_NUMBER = "remote_logger_sequence_number";

    private JSONArray mLogEntries;
    private long mSequenceNumber;

    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mPreferenceEditor;
    private RequestQueue mRequestQueue;

    private RemoteLoggerInterface mInterface;




    public RemoteLogger(Context context){
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mPreferenceEditor = mPreferences.edit();
        mRequestQueue = Volley.newRequestQueue(context);
        mLogEntries = new JSONArray();
        mSequenceNumber = System.currentTimeMillis();
    }


    private synchronized long getNextSequenceNumber(){
        long current = mPreferences.getLong(PREFERENCE_KEY_SEQUENCE_NUMBER, System.currentTimeMillis());
        mPreferenceEditor.putLong(PREFERENCE_KEY_SEQUENCE_NUMBER, current+1);
        mPreferenceEditor.apply();
        return current+1;
    }


    /*
    private static String calToStr(Calendar date, String format) {
        DateFormat formatter = new SimpleDateFormat(format, Locale.US);
        return formatter.format(date.getTime());
    }

    private static String calToDateTimeHiresStr(Calendar adatetime){
        return calToStr(adatetime,DATE_TIME_STAMP_HIRES_FORMAT);
    }

    private static String getTimeStampString() {
        Calendar now = Calendar.getInstance();
        return calToDateTimeHiresStr(now);
    }
    */


    public void v(@NonNls String tag, @NonNls String message){ appendLog("V", tag, message); }
    public void d(@NonNls String tag, @NonNls String message){ appendLog("D", tag, message); }
    public void i(@NonNls String tag, @NonNls String message){ appendLog("I", tag, message); }
    public void w(@NonNls String tag, @NonNls String message){ appendLog("W", tag, message); }
    public void e(@NonNls String tag, @NonNls String message){ appendLog("E", tag, message); }


    public synchronized void appendLog(@NonNls String level, @NonNls String tag, @NonNls String message){
        try {
            String uuid = UUID.randomUUID().toString();
            long seq = getNextSequenceNumber();

            JSONObject logEntry = new JSONObject();
            logEntry.put("seq", ++mSequenceNumber);
            logEntry.put("timestamp", System.currentTimeMillis());
            logEntry.put("level", level);
            logEntry.put("tag", tag);
            logEntry.put("message", message);
            logEntry.put("uuid", uuid);
            logEntry.put("seq", seq);
            mLogEntries.put(logEntry);

            //Log.v(TAG, "appendLog()  seq="+seq+"   uuid="+uuid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public synchronized void removeEntries(JSONArray uuids){
        int removeCount = 0;

        if( null != uuids ) {
            for (int n = 0; n < uuids.length(); n++) {
                int i = -1;
                while (true) {
                    i++;
                    if (i >= mLogEntries.length())
                        break;

                    try {
                        JSONObject entry = (JSONObject) mLogEntries.get(i);
                        if (uuids.getString(n).equals(entry.getString("uuid"))) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                                mLogEntries.remove(i);
                            else
                                mLogEntries = removeFromJsonArray(mLogEntries, i);
                            removeCount++;
                            break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        //Log.v(TAG, "Removed "+removeCount+" entries based on their uuids. Current number of entries: "+mLogEntries.length());
    }


    public synchronized void storeLog(Context context) {
        try {
            String packageName = context.getPackageName();
            packageName = packageName != null ? packageName : "null";

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("password", SCRIPT_PASSWORD);
            jsonRequest.put("log_entries", mLogEntries);
            jsonRequest.put("package_name", packageName);
            jsonRequest.put("device_id", getUniqueDeviceIdentifier(context));
            jsonRequest.put("version_code", BuildConfig.VERSION_CODE);
            jsonRequest.put("version_name", BuildConfig.VERSION_NAME);
            jsonRequest.put("extra", "");


            //Log.v("JSON", "Sending "+mLogEntries.length()+" entries to server...");

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    STORE_LOG_SCRIPT_URL,
                    jsonRequest,
                    onRequestSuccess,
                    onRequestError);

            mRequestQueue.add(jsonObjectRequest);
        }
        catch (Exception e) {
            DevLog.printStackTrace(e);
        }
    }


    private Response.Listener<JSONObject> onRequestSuccess = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            if( !response.optString("status", "").equals("ok") ) {
                DevLog.e(TAG, "Log couldn't be stored in server database!");

                if( mInterface != null )
                    mInterface.onStoreFailure();
            }
            else {
                //Log.v(TAG, "Stored "+response.optString("stored_count", "(not set)")+" entries. Message: "+response.optString("message"));
                //Log.v(TAG, "stored_uuids="+String.valueOf(response.opt("stored_uuids")));
                JSONArray storedUuids = null;
                try {
                    storedUuids = new JSONArray( response.optString("stored_uuids", "[]") );
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                removeEntries( storedUuids );

                if( mInterface != null )
                    mInterface.onStoreSuccess();
            }
        }
    };


    private Response.ErrorListener onRequestError = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            @NonNls String errorStr = "Volley error: ";
            errorStr += "HTTP code " + (error != null && error.networkResponse != null ? error.networkResponse.statusCode : "(none)") + "   ";
            errorStr += "Message: " + (error != null && error.getMessage() != null ? error.getMessage() : "(none)");

            DevLog.e(TAG, errorStr);
            if( error != null ) {
                for (StackTraceElement stackTraceElement : error.getStackTrace()) {
                    DevLog.e("RemoteLogger/VolleyError", "at " + String.valueOf(stackTraceElement));
                }
            }

            if( mInterface != null )
                mInterface.onStoreFailure();
        }
    };


    @SuppressLint("HardwareIds")
    private String getUniqueDeviceIdentifier(Context context){
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }





    private JSONArray removeFromJsonArray(JSONArray jsonArray, int index) {
        JSONArray output = new JSONArray();
        int len = jsonArray.length();
        for (int i = 0; i < len; i++)   {
            if (i != index) {
                try {
                    output.put(jsonArray.get(i));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return output;
    }


    public void setInterface(RemoteLoggerInterface aInterface){
        mInterface = aInterface;
    }
}
