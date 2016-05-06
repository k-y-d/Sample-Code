package com.yangdevatca.road.ontariocams;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;

/**
 * CommonAsyncTaskManager is used to store and lookup the launched AsyncTasks in order to determine
 * the most recently launched task for the same source.
 *
 * Each AsyncTask has a required parameter sourceId which identifies the source view with which
 * this task is associated.
 *
 * For example, two ImageViews on an Activity will represent two separate sourceId, and two
 * AsyncTasks launched for the same ImageView will have the same sourceId as parameter.
 *
 * If there are multiple AsyncTasks launched for the same source, CommonAsyncTaskManager will only
 * post back the result of the most recently launched task, and cancel any other AsyncTasks
 * if possible, or discard the results of any other AsyncTasks.
 */
class CommonAsyncTaskManager implements AsyncTaskSequenceHolder{
    static int SOURCE_PRIORITY_LOW = 1;
    static int SOURCE_PRIORITY_HIGH = 2;
    private static final float HASHTABLE_PARAM_RATIO = 0.75f;
    private Hashtable<Integer, SourceTableValue> sourceTable;

    CommonAsyncTaskManager(int originCount){
        sourceTable = new Hashtable<>((int)(originCount/HASHTABLE_PARAM_RATIO + 1), HASHTABLE_PARAM_RATIO);
    }

    boolean removeSource(int sourceId){
        return sourceTable.remove(sourceId) != null;
    }

    void cancelAllTasks(){
        if(sourceTable == null)
            return;

        Set<Integer> set = sourceTable.keySet();
//--Log.d(GlobalSettings.DEBUG_TAG, "cancelAllTasks(), set.size()=" + set.size());

        for (Integer key : set ) {
            SourceTableValue value = sourceTable.get(key);
            if(value != null && value.asyncTask != null){
                value.asyncTask.cancel(true);
//--Log.d(GlobalSettings.DEBUG_TAG, "cancelAllTasks(), sourceId=" + key);
            }
        }
    }

    void launchImageLoadTask(int sourceId, int id, String url, ImageLoadAsyncTask.Listener listener, UiAvailability uiAvailability){
        ImageLoadAsyncTask task = new ImageLoadAsyncTask(sourceId, id, url, listener, this);
        launchTask(task, uiAvailability);
    }

    void launchImageLoadTask(int sourceId, int id, String url, ImageLoadAsyncTask.Listener listener, UiAvailability uiAvailability, int priority){
        ImageLoadAsyncTask task = new ImageLoadAsyncTask(sourceId, id, url, listener, this);
        launchTask(task, uiAvailability, priority);
    }

    private void launchTask(CommonAsyncTask task, UiAvailability uiAvailability){
        launchTask(task, uiAvailability, SOURCE_PRIORITY_HIGH);
    }

    private void launchTask(CommonAsyncTask task, UiAvailability uiAvailability, int priority){
        int sourceId = task.getSourceId();
        SourceTableValue value = sourceTable.get(sourceId);
        if(value != null && value.priority > priority)
            return;

        if(value != null){
            AsyncTask preTask = value.asyncTask;
            if(preTask != null){
                preTask.cancel(true);
            }
        }

        sourceTable.put(sourceId, new SourceTableValue(task, uiAvailability));
        task.execute();
    }

    @Override
    public boolean isInOrder(CommonAsyncTask task) {
        boolean hit;
        int sourceId = task.getSourceId();
        SourceTableValue value = sourceTable.get(sourceId);
        AsyncTask latestTask = value.asyncTask;
        boolean isLatest = latestTask != null && latestTask == task;
        hit = isLatest && value.uiAvailability != null && value.uiAvailability.isUiAvailable();
//--Log.d(GlobalSettings.DEBUG_TAG, String.format("isInOrder(): sourceId=%d, ui=%s, isLatest=%s", sourceId, value.uiAvailability.isUiAvailable(), isLatest));
        if(isLatest){
            sourceTable.remove(sourceId);
        }
        return hit;
    }

    private static class SourceTableValue{
        AsyncTask asyncTask;
        UiAvailability uiAvailability;
        int priority;

        SourceTableValue(AsyncTask asyncTask, UiAvailability uiAvailability){
            this(asyncTask, uiAvailability, SOURCE_PRIORITY_HIGH);
        }

        SourceTableValue(AsyncTask asyncTask, UiAvailability uiAvailability, int priority){
            this.asyncTask = asyncTask;
            this.uiAvailability = uiAvailability;
            this.priority = priority;
        }
    }
}

interface AsyncTaskCommonFields{
    void setSourceId(int sourceId);
    int getSourceId();
}

interface AsyncTaskSequenceHolder{
    boolean isInOrder(CommonAsyncTask task);
}

interface AsyncTaskState{
    boolean isCancelled();
    void setCancelledState();
    void setExpireDate(Date date);
    void publishProgress(Object object);
}

interface UiAvailability{
    boolean isUiAvailable();
}

abstract class CommonAsyncTask extends AsyncTask<Void, Object, Void> implements AsyncTaskCommonFields, AsyncTaskState {
    boolean isCancelled = false;
    boolean hasException = false;
    boolean isResultValid = true;
    Date expireDate;
    int sourceId = -1;
    int id;
    AsyncTaskSequenceHolder holder;

    CommonAsyncTask (int sourceId, int id, AsyncTaskSequenceHolder holder){
        this.sourceId = sourceId;
        this.id = id;
        this.holder = holder;
    }

    @Override
    public int getSourceId() {
        return this.sourceId;
    }

    @Override
    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    @Override
    public void onPostExecute(Void aVoid) {
        this.isResultValid = this.holder.isInOrder(this);
    }

    @Override
    public void onCancelled() {
        this.isCancelled = true;
//--Log.d(GlobalSettings.DEBUG_TAG, String.format("onCancelled(): %d, %d", sourceId, id));
    }

    @Override
    public void publishProgress(Object object) {
        super.publishProgress(object);
    }

    @Override
    public void setCancelledState() {
        this.isCancelled = true;
    }

    @Override
    public void setExpireDate(Date date) {
        this.expireDate = date;
    }
}

abstract class CommonAsyncTaskListener {
    void onProgressUpdate(Object data){}
    void onTaskFailed(Exception ex){}
}

/**
 * We can define other AsyncTask that extends CommonAsyncTask and follows the similar pattern as
 * ImageLoadAsyncTask, such as DatabaseQueryAsyncTask, FileLoadAsyncTask, ImageProcessAsyncTask, etc.
 */
class ImageLoadAsyncTask extends CommonAsyncTask {
    static final String BASE_URL = "http://www.base_url.com";
    static final int CONNECT_TIMEOUT = 3000;
    static final int READ_TIMEOUT = 5000;
    private final String baseUrl;
    private String imageUrl;
    private Bitmap image = null;
    private Listener listener;
    private boolean iNetAvailable = true;

    ImageLoadAsyncTask (int sourceId, int id, String url, Listener listener, AsyncTaskSequenceHolder holder){
        super(sourceId, id, holder);
        this.baseUrl = BASE_URL;
        this.listener = listener;
        this.imageUrl = baseUrl + url;
//--Log.d(GlobalSettings.DEBUG_TAG, "Image Load Task Thread id: " + Thread.currentThread().getId());

    }

    void setImageUrl(String url){
        imageUrl = baseUrl + url;
    }

    @Override
    public Void doInBackground(Void... params) {
//--Log.d(GlobalSettings.DEBUG_TAG, "Image Load doInBK Thread id: " + Thread.currentThread().getId());

        try {
            image = fetchImage(imageUrl, this);
        }catch (UnknownHostException | SocketTimeoutException e){
            this.hasException = true;
            iNetAvailable = false;
            //--Log.e(GlobalSettings.DEBUG_TAG, String.format("Internet Connection Failed, %s", e.getMessage()));
            //--Log.e(GlobalSettings.DEBUG_TAG, e.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
            //--Log.e(GlobalSettings.DEBUG_TAG, e.toString());
            this.hasException = true;
        }

        return null;
    }

    @Override
    public void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        if(isResultValid && !hasException && listener != null){
            listener.onTaskFinished(sourceId, id, image, null);
        }else if(isResultValid && hasException && listener != null && !iNetAvailable){
            listener.onInetFailed(sourceId, id);
        }
//--Log.d(GlobalSettings.DEBUG_TAG, "Image Load onPost Thread id: " + Thread.currentThread().getId());

        return;
    }

    @Override
    public void onProgressUpdate(Object... values) {
        super.onProgressUpdate(values);
        if(listener != null){
            listener.onProgressUpdate(values[0]);
        }
    }

    static Bitmap fetchImage(String sUrl, AsyncTaskState taskState) throws IOException{
        Bitmap image = null;
        /**
         * Make http request to get the image from the given Url.
         * At multiple appropriate points of the code execution, check if this task was cancelled.
         * If it was cancelled, terminate the code execution and turn in order to save time and resources.
         */
        return image;
    }

    static abstract class Listener extends CommonAsyncTaskListener {
        abstract void onTaskFinished(int sourceId, int id, Bitmap image, Date expireDate);
        abstract void onInetFailed(int sourceId, int id);
    }
}

