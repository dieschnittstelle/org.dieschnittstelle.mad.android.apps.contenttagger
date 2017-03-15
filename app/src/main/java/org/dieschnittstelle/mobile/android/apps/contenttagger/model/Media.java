package org.dieschnittstelle.mobile.android.apps.contenttagger.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.orm.dsl.Ignore;
import com.orm.dsl.Table;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Comparator;

/**
 * Created by master on 15.03.16.
 *
 * for bitmap handling, look into: https://developer.android.com/topic/performance/graphics/index.html
 */
@Table
public class Media extends Taggable implements Serializable {

    public static enum ContentType {LOCALURI, EXTURI, LOCALDATA};

    public static final int THUMBNAIL_WIDTH = 150;
    public static final int THUMBNAIL_HEIGHT= 150;

    // Comparators
    public static Comparator<Media> COMPARE_BY_DATE = new Comparator<Media>() {
        @Override
        public int compare(Media lhs, Media rhs) {
            Long lhsdate = lhs.created;
            Long rhsdate = rhs.created;
            return lhsdate.compareTo(rhsdate);
        }
    };


    private String title;

    private long created;

    private String contentUri;

    private Long id;

    private String associations;

    private ContentType contentType;

    @Ignore
    private Bitmap thumbnail;

    @Ignore
    private Bitmap image;

    private String thumbnailPath;

    public Media() {

    }

    public Media(String title, String contentUri) {
        this.title = title;
        this.contentUri = contentUri;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public void setAssociations(String assoc) {
        this.associations = assoc;
    }

    @Override
    public String getAssociations() {
        return this.associations;
    }

    public String getContentUri() {
        return contentUri;
    }

    public void setContentUri(String contentUri) {
        this.contentUri = contentUri;
    }

//    @Override
//    public void preDestroy() {
//        // before a link is removed, we need to remove it from any tags that are associated with it
//        for (Tag tag : this.getTags()) {
//            tag.getTaggedItems().remove(this);
//            addPendingUpdate(tag);
//        }
//    }

    // update last modified on update
    public void create()  {
        this.created = System.currentTimeMillis();
        super.create();
    }

    public long getCreated() {
        return this.created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    @Override
    public String toString() {
        return "Media{" +
                "title='" + title + '\'' +
                ", contentUri='" + contentUri + '\'' +
                ", created=" + created +
                ", id=" + id +
                ", tags=" + getTags() +
                '}';
    }

    public void update()  {
        this.created = System.currentTimeMillis();
        super.update();
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    /*
     *
     */
    public static interface OnImageLoadedHandler {

        public void onImageLoaded(Bitmap image);

    }

    /*
     * load a thumbnail
     */
    public void loadThumbnailNew(final Context context, final OnImageLoadedHandler callback) {
        if (this.thumbnail != null) {
            Log.i(logger,"loadThumbnail(): thumbnail has been created already");
            callback.onImageLoaded(this.thumbnail);
        }
        else if (this.thumbnailPath != null) {
            Log.i(logger,"loadThumbnail(): load thumbnail from local path: " + this.thumbnailPath);

            new AsyncTask<Void,Void,Bitmap>() {

                @Override
                protected Bitmap doInBackground(Void... voids) {
                    return readImageFromPath(Media.this.thumbnailPath);
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    callback.onImageLoaded(bitmap);
                }

            }.execute();

        }
        else {
            Log.i(logger,"loadThumbnail(): load image and try to convert it");
            loadImage(context, new OnImageLoadedHandler() {
                @Override
                public void onImageLoaded(final Bitmap image) {
                    new AsyncTask<Void,Void,Bitmap>() {

                        @Override
                        protected Bitmap doInBackground(Void... voids) {
                            if (image != null) {
                                return extractThumbnail(image, context);
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Bitmap bitmap) {
                            callback.onImageLoaded(bitmap);
                        }

                    }.execute();
                }
            });
        }
    }

    /*
    * this covers
    */
    public void loadThumbnail(final Context context, final OnImageLoadedHandler callback) {
        if (this.thumbnail != null) {
            Log.i(logger,"createThumbnail(): thumbnail has been created already");
            callback.onImageLoaded(this.thumbnail);
        }
        else if (this.thumbnailPath != null) {
            Log.i(logger,"createThumbnail(): load thumbnail from local path: " + this.thumbnailPath);

            new AsyncTask<Void,Void,Bitmap>() {

                @Override
                protected Bitmap doInBackground(Void... voids) {
                    return readImageFromPath(Media.this.thumbnailPath);
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    callback.onImageLoaded(bitmap);
                }

            }.execute();

        }
        else if (this.contentType == ContentType.EXTURI) {
            new AsyncTask<Void, Void, Bitmap>() {

                @Override
                protected Bitmap doInBackground(Void... voids) {
                    Log.i(logger, "createThumbnail(): loading image data for media item with url: " + Media.this.getContentUri());
                    try {
                        URL url = new URL(Media.this.getContentUri());
                        Bitmap orig = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                        return extractThumbnail(orig,context);
                    } catch (Exception e) {
                        Log.e(logger, "createThumbnail(): got exception trying to load media: " + e, e);
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    callback.onImageLoaded(bitmap);
                }

            }.execute();

        }
        else if (this.contentType == ContentType.LOCALURI) {
            new AsyncTask<Void,Void,Bitmap>() {

                @Override
                protected Bitmap doInBackground(Void... voids) {
                    try {
                        Log.i(logger,"createThumbnail(): will load local data and create thumbnail...");
                        // see https://developer.android.com/guide/topics/providers/document-provider.html
                        ParcelFileDescriptor parcelFileDescriptor =
                                context.getContentResolver().openFileDescriptor(Uri.parse(Media.this.getContentUri()), "r");
                        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        Bitmap orig = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                        parcelFileDescriptor.close();
                        return extractThumbnail(orig,context);
                    }
                    catch (Exception e) {
                        Log.e(logger,"createThumbnail(): got an exception trying to read data from local uri: " + e,e);
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    callback.onImageLoaded(bitmap);
                }

            }.execute();

        }
        else {
            Log.e(logger,"createThumbnail(): contentType not supported: " + this.contentType);
            callback.onImageLoaded(null);
        }
    }

    public void releaseImage() {
        if (this.image != null) {
            this.image.recycle();
            this.image = null;
        }
    }

    /*
    * load the original image
    */
    public void loadImage(final Context context, final OnImageLoadedHandler callback) {
        if (this.image != null) {
            callback.onImageLoaded(this.image);
        }
        else if (this.contentType == ContentType.EXTURI) {
            new AsyncTask<Void, Void, Bitmap>() {

                @Override
                protected Bitmap doInBackground(Void... voids) {
                    Log.i(logger, "loadImage(): loading image data for media item with url: " + Media.this.getContentUri());
                    try {
                        URL url = new URL(Media.this.getContentUri());
                        Bitmap orig = BitmapFactory.decodeStream(url.openConnection().getInputStream());
//                        image = orig;
                        return orig;
                    } catch (Exception e) {
                        Log.e(logger, "loadImage(): got exception trying to load media: " + e, e);
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    callback.onImageLoaded(bitmap);
                }

            }.execute();

        }
        else if (this.contentType == ContentType.LOCALURI) {
            new AsyncTask<Void,Void,Bitmap>() {

                @Override
                protected Bitmap doInBackground(Void... voids) {
                    try {
                        Log.i(logger,"loadImage(): will load local data and create thumbnail...");
                        // see https://developer.android.com/guide/topics/providers/document-provider.html
                        ParcelFileDescriptor parcelFileDescriptor =
                                context.getContentResolver().openFileDescriptor(Uri.parse(Media.this.getContentUri()), "r");
                        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        Bitmap orig = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                        parcelFileDescriptor.close();
//                        image = orig;
                        return orig;
                    }
                    catch (Exception e) {
                        Log.e(logger,"loadImage(): got an exception trying to read data from local uri: " + e,e);
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    callback.onImageLoaded(bitmap);
                }

            }.execute();

        }
        else {
            Log.e(logger,"loadThumbnail(): contentType not supported: " + this.contentType);
            callback.onImageLoaded(null);
        }
    }

    public Bitmap extractThumbnail(Bitmap orig,Context context) {
        Bitmap thumb = ThumbnailUtils.extractThumbnail(orig,THUMBNAIL_WIDTH,THUMBNAIL_HEIGHT);
        String path = "/thumbnails/"+System.currentTimeMillis() + ".bmp";
        String writtenPath = writeImageToPath(thumb,path,context);
        this.setThumbnailPath(writtenPath);
        this.setThumbnail(thumb);
        return thumb;
    }

    public String writeImageToPath(Bitmap image,String filename, Context context) {

        FileOutputStream out = null;
        try {
            File file = new File(context.getFilesDir(), filename);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            out = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored

            return String.valueOf(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Bitmap readImageFromPath(String path) {
        Bitmap bmp = BitmapFactory.decodeFile(path);
        return bmp;
    }


}
