package com.purecomet.videomerger.model

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.purecomet.videomerger.MainActivity
import com.purecomet.videomerger.R
import com.purecomet.videomerger.videoprocessing.VideoProcessor

class VideosViewModel(applicationContext: Application) : AndroidViewModel(applicationContext) {
    var videosLiveData: MutableLiveData<List<Video>>

    // TODO this shouldn't be mutable but I also don't want to copy the list each update
    val selectedVideos: MutableLiveData<MutableList<Video>>

    init {
        Log.i("Model", "creating model")
        videosLiveData = MutableLiveData()
        selectedVideos = MutableLiveData()
        selectedVideos.value = ArrayList()
    }

    fun getVideosData(): LiveData<List<Video>> {
        return videosLiveData
    }

    fun getSelectedVideosData(): LiveData<MutableList<Video>> {
        return selectedVideos
    }

    fun loadData(context: Context) {
        LoadDataTask(videosLiveData, context).execute()
    }

    fun mergeVideos(context: Context) {

        if(selectedVideos.value?.size ==2)
        {
            MergeVideosTask(selectedVideos.value!!, context).execute()
        }
        else
        {
            Toast.makeText(context, context.resources.getString(R.string.error_text),Toast.LENGTH_LONG).show()
        }
    }

    fun addSelectedVideo(video: Video) {
        selectedVideos.value!!.add(video)
        selectedVideos.value = selectedVideos.value
    }

    fun removeSelectedVideo(video: Video) {
        selectedVideos.value!!.remove(video)
        selectedVideos.value = selectedVideos.value
    }

    private class LoadDataTask(
        videosLiveData: MutableLiveData<List<Video>>,
        context: Context) : AsyncTask<Void, Void, List<Video>>() {
        val videosLiveData: MutableLiveData<List<Video>> = videosLiveData
        val context = context

        override fun doInBackground(vararg urls: Void): List<Video> {
            return VideoProcessor().run(context)
        }

        override fun onPostExecute(result: List<Video>) {
            videosLiveData.value = result
            Toast.makeText(
                context,
                context.resources.getString(R.string.scan_toast, result.size),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private class MergeVideosTask(
        selectVideos: List<Video>,
        context: Context) : AsyncTask<Void, Int, Video?>() {
        val selectVideos: List<Video> = selectVideos
        val context = context
        lateinit var notificationManager: NotificationManager
        lateinit var notificationChannel: NotificationChannel
        lateinit var builder: Notification.Builder
        val channelId = "i.apps.notifications"
        val description = "Merge notification"
        override fun onPreExecute() {
            super.onPreExecute()
            addNotification(context)
            builder.setProgress(100, 0, false);
            notificationManager.notify(1234, builder.build());

        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
            builder.setProgress(100, values[0]!!, false);
            notificationManager.notify(1234, builder.build());
        }
        private fun addNotification(context: Context)
        {
            notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT)

            // RemoteViews are used to use the content of
            // some different layout apart from the current activity layout
            //val contentView = RemoteViews(packageName, R.layout.activity_after_notification)

            // checking if android version is greater than oreo(API 26) or not
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationChannel = NotificationChannel(channelId, description, NotificationManager.IMPORTANCE_HIGH)
                notificationChannel.enableLights(true)
                notificationChannel.lightColor = Color.GREEN
                notificationChannel.enableVibration(false)
                notificationManager.createNotificationChannel(notificationChannel)

                builder = Notification.Builder(context, channelId)
                    .setContentTitle(context.getString(R.string.noti_title))
                    .setContentText(context.getString(R.string.noti_message))
                    .setSmallIcon(R.drawable.icon)
                    .setLargeIcon(
                        BitmapFactory.decodeResource(context.resources,
                            R.drawable.icon))
                    .setContentIntent(pendingIntent)
            }
            else
            {
                builder = Notification.Builder(context)
                    .setContentTitle(context.getString(R.string.noti_title))
                    .setContentText(context.getString(R.string.noti_message))
                    .setSmallIcon(R.drawable.icon)
                    .setLargeIcon(
                        BitmapFactory.decodeResource(context.resources,
                            R.drawable.icon))
                    .setContentIntent(pendingIntent)
            }
            notificationManager.notify(1234, builder.build())
        }

        override fun doInBackground(vararg urls: Void): Video? {

            return VideoProcessor().mergeSelectedVideos(context, selectVideos)
        }

        override fun onPostExecute(result: Video?) {
            if (result != null) {
             //   Toast.makeText(context, context.resources.getString(R.string.created_video, result.name), Toast.LENGTH_LONG).show()
                builder.setContentText(context.getString(R.string.noti_complete_text));
                // Removes the progress bar
                builder.setProgress(0, 0, false);
                notificationManager.notify(1234, builder.build());
            }
        }
    }

}