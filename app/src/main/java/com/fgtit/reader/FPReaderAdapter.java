package com.fgtit.reader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class FPReaderAdapter extends BaseAdapter {
    ArrayList<String> status;
    Context context;
    LayoutInflater inflater;

    public FPReaderAdapter(Context context, ArrayList<String> status){
        this.status = status;
        this.context = context;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * How many items are in the data set represented by this Adapter.
     *
     * @return Count of items.
     */
    @Override
    public int getCount() {
        return status.size();
    }

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param position Position of the item whose data we want within the adapter's
     *                 data set.
     * @return The data at the specified position.
     */
    @Override
    public Object getItem(int position) {
        return status.get(position);
    }

    /**
     * Get the row id associated with the specified position in the list.
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Get a View that displays the data at the specified position in the data set. You can either
     * create a View manually or inflate it from an XML layout file. When the View is inflated, the
     * parent View (GridView, ListView...) will apply default layout parameters unless you use
     * {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
     * to specify a root view and to prevent attachment to the root.
     *
     * @param position    The position of the item within the adapter's data set of the item whose view
     *                    we want.
     * @param view The old view to reuse, if possible. Note: You should check that this view
     *                    is non-null and of an appropriate type before using. If it is not possible to convert
     *                    this view to display the correct data, this method can create a new view.
     *                    Heterogeneous lists can specify their number of view types, so that this View is
     *                    always of the right type (see {@link #getViewTypeCount()} and
     *                    {@link #getItemViewType(int)}).
     * @param parent      The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if(view == null){
            view = inflater.inflate(R.layout.fp_reader_item,null);
        }
        final String status = (String) getItem(position);

        TextView notification = view.findViewById(R.id.notification);
        TextView statusText = view.findViewById(R.id.status_text);
        ImageView statusImg = view.findViewById(R.id.status_image);
        LinearLayout linearLayout = view.findViewById(R.id.status);


        if (status.contains("Failed")){
            notification.setText((String) getItem(position-1));
            statusText.setText(status);
            statusText.setTextColor(context.getResources().getColor(R.color.fp_red));
            statusImg.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_x_circle));
        }else {
            if (position == this.status.size()-1){
                notification.setText(status);
                linearLayout.setVisibility(View.INVISIBLE);
            }else {
                notification.setText(status);
                statusText.setText(context.getResources().getString(R.string.fingerprint_captured));
            }
        }

        return view;
    }
}
