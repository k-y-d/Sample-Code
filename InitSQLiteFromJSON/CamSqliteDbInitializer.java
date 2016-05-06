package com.yangdevatca.road.ontariocams;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.transition.Explode;
import android.util.Log;

import java.io.File;
import java.util.List;

public class CamSqliteDbInitializer {
    private final Context mContext;
    private InitializeTaskListener mListener;
    private List<Cam> mCams;

    public  CamSqliteDbInitializer(Context context, InitializeTaskListener listener, List<Cam> cams){
        mContext = context;
        mListener = listener;
        mCams = cams;
    }

    void Initialize(){
//--Log.d(GlobalSettings.DEBUG_TAG, "Starting CamDatabaseOpenHelper()");
        SQLiteOpenHelper dbOpener = new CamDatabaseOpenHelper(mContext);
//--Log.d(GlobalSettings.DEBUG_TAG, "Finished CamDatabaseOpenHelper()");
        new InitializeTask().execute(dbOpener);
    }

    private boolean doInitialize(SQLiteOpenHelper openHelper){
        boolean success = true;
        SQLiteDatabase db = openHelper.getWritableDatabase();

        try {
//--Log.d(GlobalSettings.DEBUG_TAG, "Starting beginTransaction()");
            ContentValues fields = new ContentValues();
            db.beginTransaction();

            //Here the deleting operation is to make sure the initialization starts from an empty table, because there are cases where the initialization could be interrupt before it completes.
            int count = db.delete(OntCamDBContract.CamTable.TABLE_NAME, "1", null);
//--Log.d(GlobalSettings.DEBUG_TAG, "rows deleted : " + count);

            for (Cam cam : mCams) {
                fields.put(OntCamDBContract.CamTable.COLUMN_NAME_LATITUDE, cam.latitude);
                fields.put(OntCamDBContract.CamTable.COLUMN_NAME_LONGITUDE, cam.longitude);
                fields.put(OntCamDBContract.CamTable.COLUMN_NAME_CAMURL, cam.camUrl);

                db.insert(OntCamDBContract.CamTable.TABLE_NAME, null, fields);
            }
//--Log.d(GlobalSettings.DEBUG_TAG, "Finished setTransactionSuccessful()");
            db.setTransactionSuccessful();
        }catch (Exception e){
            success = false;
            e.printStackTrace();
            //--Log.e(GlobalSettings.DEBUG_TAG, e.getMessage());
        }
        finally {
            db.endTransaction();
//--Log.d(GlobalSettings.DEBUG_TAG, "Finished endTransaction()");
            db.close();
            openHelper.close();
            mCams = null;
        }
//--Log.d(GlobalSettings.DEBUG_TAG, "DB Init success: " + success);
        return success;
    }

    private void onPostResult(boolean success){
        mListener.onInitializeTaskFinished(success);
    }

    private class InitializeTask extends AsyncTask<SQLiteOpenHelper, Void, Boolean>{
        @Override
        protected Boolean doInBackground(SQLiteOpenHelper... params) {
            return doInitialize(params[0]);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            onPostResult(success);
        }
    }

    public interface InitializeTaskListener{
        void onInitializeTaskFinished(boolean success);
    }
}

class CamDatabaseOpenHelper extends SQLiteOpenHelper {

    CamDatabaseOpenHelper(Context context) {
        super(context, OntCamDBContract.DATABASE_NAME, null, OntCamDBContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(OntCamDBContract.CamTable.TABLE_CREATE);
        db.execSQL(OntCamDBContract.CamTable.CREATE_INDEX);
//--Log.d(GlobalSettings.DEBUG_TAG, "called onCreate() of CamDatabaseOpenHelper");
    }

    @Override
    public void onOpen(SQLiteDatabase db){
        //Log.d(GlobalSettings.DEBUG_TAG, "called onOpen() of CamDatabaseOpenHelper");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Log.d(GlobalSettings.DEBUG_TAG, "called onUpgrade() of CamDatabaseOpenHelper");
    }
}
