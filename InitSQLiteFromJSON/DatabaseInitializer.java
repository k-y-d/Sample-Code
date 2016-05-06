package com.yangdevatca.road.ontariocams;

import android.content.Context;

import java.util.List;

public class DatabaseInitializer implements CamsJsonFileLoader.LoadTaskListener, CamSqliteDbInitializer.InitializeTaskListener {
    private Context mContext;
    private InitializeTaskListener mListener;

    public DatabaseInitializer(Context context, InitializeTaskListener listener){
        this.mContext = context;
        this.mListener = listener;
    }

    public void Initialize(){
        new CamsJsonFileLoader(mContext, this).load();
        return;
    }

    //CamsJsonFileLoader.LoadTaskListener
    @Override
    public void onLoadTaskFinished(List<Cam> cams) {
//--Log.d(GlobalSettings.DEBUG_TAG, "Json file load finished");
        //Double check the number of records inserted/initialized in SQLite database is the same as expected.
        if(cams == null || cams.size() != GlobalSettings.CAMS_COUNT ){
            mListener.onInitializeTaskFinished(false);
            return;
        }
//        debugPrintList(cams);

        new CamSqliteDbInitializer(mContext, this, cams).Initialize();
    }

    //CamSqliteDbInitializer.InitializeTaskListener
    @Override
    public void onInitializeTaskFinished(boolean success) {
        mListener.onInitializeTaskFinished(success);
    }

    public interface InitializeTaskListener{
        void onInitializeTaskFinished(boolean success);
    }

}
