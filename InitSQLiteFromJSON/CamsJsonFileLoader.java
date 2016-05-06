package com.yangdevatca.road.ontariocams;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;

public class CamsJsonFileLoader {
    private final Context mContext;
    private final LoadTaskListener mListener;

    public CamsJsonFileLoader(Context context, LoadTaskListener listener) {
        mContext = context;
        mListener = listener;
    }

    public void load() {
        new LoadTask().execute(0);
    }

    private List<Cam> doLoad() throws IOException {
        List<Cam> cams = null;
//--Log.d(GlobalSettings.DEBUG_TAG, "Started Json load");
        final Gson gson = new Gson();
        InputStream is = null;
        Reader reader = null;

        try {
            AssetManager manager = mContext.getAssets();
            is = manager.open(GlobalSettings.CAMS_JSON_FILE);
            BufferedInputStream bis = new BufferedInputStream(is);
            reader = new InputStreamReader(bis);
            final Type typeOfSrc = new TypeToken<List<Cam>>() {}.getType();
            cams = gson.fromJson(reader, typeOfSrc);
        }finally {
            if(is != null)
                is.close();
            if(reader != null)
                reader.close();
        }
//--Log.d(GlobalSettings.DEBUG_TAG, "Finished Json load");

        return cams;
    }

    private void onPostResult(List<Cam> cams){
        mListener.onLoadTaskFinished(cams);
    }

    private class LoadTask extends AsyncTask<Integer, Void, List<Cam>>{
        @Override
        protected List<Cam> doInBackground(Integer... params) {
            List<Cam> list = null;
            try {
                list = doLoad();
            } catch (IOException e) {
                e.printStackTrace();
                //--Log.e(GlobalSettings.DEBUG_TAG, e.getMessage());
            }
            return list;
        }

        @Override
        protected void onPostExecute(List<Cam> cams) {
            onPostResult(cams);
        }
    }

    public interface LoadTaskListener{
        void onLoadTaskFinished(List<Cam> cams);
    }
}

// This is the class which maps to the JSON object. The JSON objects array defined in a JSON file will be loaded and converted to a list of objects of this class by using Gson library.
// The class definition is shorted for demo purpose.
class Cam {
    @SerializedName("lati")
    public double latitude;
    @SerializedName("longi")
    public double longitude;
    public String camUrl;

    @Override
    public String toString() {
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return json;
    }
}

