package instagram.tenishev.com.selfiemirror;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * ImageCache class.
 *
 * serves for fetching and caching
 */
public class ImageCache extends Thread {
    private static final String TAG = ImageCache.class.getSimpleName();

    private File cacheDir;
    private Handler handler;
    private Handler mMessagesHandler;
    private HandlerThread mMessagesThread;

    public static final int QUEUE_CAPACITY = 50;


    private class LoadingImageEntity {
        public String url;
        public WeakReference<ImageViewHolder> holder;

        public LoadingImageEntity(final String url, final ImageViewHolder holder) {
            this.url = url;
            this.holder = new WeakReference<ImageViewHolder>(holder);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LoadingImageEntity)) return false;

            LoadingImageEntity that = (LoadingImageEntity) o;

            if( holder.get() != null && that.holder.get() != null ) {
                if (!holder.get().equals(that.holder.get())) return false;
            }

            if( holder.get() != that.holder.get() ) return false;
            if (!url.equals(that.url)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = 11;
            if( url != null ) {
                result = 37 * result + url.hashCode();
            }
            if( holder != null && holder.get() != null ) {
                result = 37 * result + holder.get().hashCode();
            }
            return result;
        }
    }

    private LinkedBlockingDeque<LoadingImageEntity> mDecodeQueue = new LinkedBlockingDeque<LoadingImageEntity>(QUEUE_CAPACITY);

    public ImageCache(final Context context) {
        cacheDir = context.getCacheDir();
        if( !cacheDir.exists() ) {
            cacheDir.mkdirs();
        }

        handler = new Handler(Looper.getMainLooper());

        mMessagesThread = new HandlerThread("loader_message_queue_thread");
        mMessagesThread.start();

        mMessagesHandler = new Handler(mMessagesThread.getLooper());
    }

    public void add(final String url, final ImageViewHolder vh) {
        mMessagesHandler.post(new Runnable() {
            @Override
            public void run() {
                if( vh.image.getTag() != null) {
                    LoadingImageEntity oldEntity = (LoadingImageEntity) vh.image.getTag();
                    if( oldEntity.url.equals(url) ) {
                        return;
                    }
                    mDecodeQueue.remove(oldEntity);
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        vh.progress.setVisibility(View.VISIBLE);
                        vh.image.setVisibility(View.INVISIBLE);
                    }
                });

                LoadingImageEntity entity = new LoadingImageEntity(url, vh);
                vh.image.setTag(entity);
                mDecodeQueue.push(entity);
            }
        });
    }

    @Override
    public void run() {
        try {
            while (true) {
                final LoadingImageEntity entity = mDecodeQueue.take();
                if( entity.holder.get() != null ) {
                    final Bitmap bitmap = getBitmapImage(entity.url, entity.holder.get());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if( entity.holder.get() != null ) {
                                final ImageViewHolder vh = entity.holder.get();
                                if( vh.image.getTag().equals(entity) ) {
                                    vh.progress.setVisibility(View.INVISIBLE);
                                    vh.image.setVisibility(View.VISIBLE);
                                    vh.image.setImageBitmap(bitmap);
                                }
                            }
                        }
                    });
                }
            }
        } catch( InterruptedException e ) {
            e.printStackTrace();
        }
    }

    public Bitmap getBitmapImage(final String url, final ImageViewHolder vh) {
        Bitmap bitmap = null;
        File file = downloadImage(url);
        if( file != null && file.exists() ) {
            // get it
            bitmap = getBitmapFromFile(file, vh);
        }
        return bitmap;
    }

    private Bitmap getBitmapFromFile(final File file, final ImageViewHolder vh)
    {
        final BitmapFactory.Options bitmapLoadingOptions = new BitmapFactory.Options();
        Bitmap bitmap = null;

//                TODO: optimize for bitmap scaling for less memory consume
//                bitmapLoadingOptions.inJustDecodeBounds = true;
//                BitmapFactory.decodeByteArray(buffer.toByteArray(), 0, buffer.size(), bitmapLoadingOptions);
//                BitmapFactory.decodeFile(file.getAbsolutePath(), bitmapLoadingOptions);

        bitmapLoadingOptions.inJustDecodeBounds = false;
        bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), bitmapLoadingOptions);

        return bitmap;
    }

    private File downloadImage(final String url) {
        File result = null;
        try {
            URL resource = new URL(url);
            String filename = resource.getFile();
            int lastSlashIndex = resource.getFile().lastIndexOf("/");
            if( lastSlashIndex != -1 ) {
                filename = filename.substring(lastSlashIndex + 1);
            }
            File destFile = new File(cacheDir, filename);
            if( !destFile.exists() ) {
                copyFile(resource, destFile);
            }
            result = destFile;
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
        return result;
    }

    public boolean copyFile(final URL url, final File destFile) {
        boolean success = false;

        final int SUB_BUFFER_SIZE = 4096;
        byte[] buffer = new byte[SUB_BUFFER_SIZE];

        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(destFile);
            inputStream = url.openStream();
            int bytesRead = inputStream.read(buffer);
            while (bytesRead != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                bytesRead = inputStream.read(buffer);
            }
            success = true;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        return success;
    }
}
