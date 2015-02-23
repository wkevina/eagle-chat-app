package eaglechat.eaglechat;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

/**
 * Created by kevinward on 2/23/15.
 */
public class MultiLayoutCursorAdapter extends SimpleCursorAdapter {

    public interface Delegate {
        public int getViewTypeCount();

        public int getItemViewType(int position, Cursor cursor);

        public View newView(Context context, Cursor cursor, ViewGroup parent, LayoutInflater inflater);
    }

    protected Context mContext;
    protected Delegate mDelegate;

    LayoutInflater mInflater;

    public MultiLayoutCursorAdapter(Context context, Delegate delegate, int defaultLayout, Cursor c, String[] from, int[] to, int flags) {
        super(context, defaultLayout, c, from, to, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContext = context;
        mDelegate = delegate;
    }


    @Override
    public int getViewTypeCount() {
        return mDelegate.getViewTypeCount();
    }

    @Override
    public int getItemViewType(int position) {
        return mDelegate.getItemViewType(position, getCursor());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (!getCursor().moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        View v;
        if (convertView == null) {
            v = newView(mContext, getCursor(), parent);
        } else {
            v = convertView;
        }
        bindView(v, mContext, getCursor());
        return v;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mDelegate.newView(context, cursor, parent, mInflater);
    }
}
