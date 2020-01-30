package com.example.servicebestpractice;

import android.app.*;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.File;

public class DownloadService extends Service {

    private DownloadTask downloadTask;

    private String downloadUrl;

    /**
     * 监听回调接口的匿名实现类
     */
    private DownloadListener listener = new DownloadListener() {

        /**
         * 重写了5个方法
         * @param progress
         */
        @Override
        public void onProgress(int progress) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //构建了一个通知,然后调用manage的方法去触发通知
                getNotificationManager().notify(1, getNotification("Downloading...", progress));
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onSuccess() {
            downloadTask = null;
            //下载成功时将前台服务通知关闭,并创建一个下载成功的通知
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("Download success", -1));
            Toast.makeText(DownloadService.this, "Download success", Toast.LENGTH_SHORT).show();
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onFailed() {
            downloadTask = null;
            //下载失败时将前台服务通知关闭,并创建一个下载失败的通知
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("Download Failed", -1));
            Toast.makeText(DownloadService.this, "Download Failed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPause() {
            downloadTask = null;
            Toast.makeText(DownloadService.this, "Paused", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCanceled() {
            downloadTask = null;
            stopForeground(true);
            Toast.makeText(DownloadService.this, "Canceled", Toast.LENGTH_SHORT).show();
        }
    };

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    public DownloadService() {
    }

    private DownloadBinder mBinder = new DownloadBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel(String id, String name, int importence) {
        NotificationManager manager = getNotificationManager();
        NotificationChannel channel = new NotificationChannel(id, name, importence);
        manager.createNotificationChannel(channel);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification getNotification(String title, int progress) {
        String channelId = "notify";
        String channelName = "普通通知";
        int importence = NotificationManager.IMPORTANCE_HIGH;
        createNotificationChannel(channelId, channelName, importence);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "notify");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        if (progress >= 0) {
            //当progress大于等于零时才需要显示进度
            builder.setContentText(progress + "%");
            //通知的最大进度,当前进度,是否使用模糊进度条
            builder.setProgress(100, progress, false);
        }
        return builder.build();
    }

    /**
     * 为了让DownloadService可以和活动之间进行通信,只需让这个类继承Binder
     */
    class DownloadBinder extends Binder {

        public void startDownload(String url) {
            if (downloadTask == null) {
                downloadUrl = url;
                //创建一个DownloadTask的实例,相当于开启一个线程,传入一个我们实现过的listener,目的是回调
                downloadTask = new DownloadTask(listener);
                downloadTask.execute(downloadUrl);  //调用execute开始下载
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForeground(1, getNotification("Downloading...", 0));   //令该服务成为一个前台服务
                    Toast.makeText(DownloadService.this, "Downloading...", Toast.LENGTH_SHORT).show();
                }
            }
        }

        public void pauseDownload() {
            if (downloadTask != null) {
                downloadTask.pauseDownload();
            }
        }

        public void cancelDownload() {
            if (downloadTask != null) {
                downloadTask.cancelDownload();
            } else {
                if (downloadUrl != null) {
                    //取消下载时应该将文件删除,并将通知关闭
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file = new File(directory + fileName);
                    if (file.exists()) {
                        file.delete();
                    }
                    getNotificationManager().cancel(1);
                    stopForeground(true);
                    Toast.makeText(DownloadService.this, "Canceled", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
