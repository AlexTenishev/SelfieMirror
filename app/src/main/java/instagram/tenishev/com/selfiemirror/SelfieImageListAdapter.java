package instagram.tenishev.com.selfiemirror;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 *
 */
public class SelfieImageListAdapter extends BaseAdapter {

    private final LayoutInflater mInflater;
    private final ImageCache mImageCache;
    private List<ImageItem> mItems;
    private WeakReference<Context> mContext;

    public SelfieImageListAdapter(Context context, List<ImageItem> items) {
        mImageCache = new ImageCache(context);
        mImageCache.start();

        mItems = items;
        mInflater = LayoutInflater.from(context);
        mContext = new WeakReference<Context>(context);
    }

    public void setData(List<ImageItem> data) {
        mItems = data;

        final Context context = mContext.get();
        if( context != null && context instanceof Activity) {
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }

    public void addData(List<ImageItem> data) {
        mItems.addAll(data);

        final Context context = mContext.get();
        if( context != null && context instanceof Activity) {
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // get to know what type of view do we need here
        boolean isBigView = ( position % 3 ) == 0;

        ImageViewHolder holder;
        if( convertView == null ) {
            convertView = mInflater.inflate(isBigView ? R.layout.img_big_row : R.layout.img_small_row, null);
            holder = new ImageViewHolder();
            holder.isBigView = isBigView;
            holder.image = (ImageView) convertView.findViewById(R.id.image);
            holder.caption = (TextView) convertView.findViewById(R.id.caption);
            holder.progress = (ProgressBar) convertView.findViewById(R.id.progress);
            convertView.setTag(holder);
        } else {
            holder = (ImageViewHolder) convertView.getTag();
        }

        final ImageItem item = (ImageItem) getItem(position);
        mImageCache.add(item.url, holder);
        holder.isBigView = isBigView;
        holder.caption.setText(item.caption);

        return convertView;
    }
}
