package systems.soapbox.ombuds.client.ui.omb;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.security.NoSuchAlgorithmException;

import systems.soapbox.ombuds.client.omb.memory.PublicRecordDbHelper;
import systems.soapbox.ombuds.client.util.CursorRecyclerAdapter;
import systems.soapbox.ombuds.client_test.R;
import systems.soapbox.ombuds.lib.field.Message;

/**
 * Created by askuck on 1/19/16.
 */
public class PublicRecordAllAdapter extends CursorRecyclerAdapter {

    Context context;
    Resources resources;

    public PublicRecordAllAdapter(Context context, Cursor cursor) {
        super(cursor);
        this.context = context;
        this.resources = context.getResources();
    }

    @Override
    public void onBindViewHolderCursor(RecyclerView.ViewHolder holder, Cursor cursor) {
        BulletinViewHolder bltnVH = (BulletinViewHolder) holder;

        int idCol       = cursor.getColumnIndexOrThrow(PublicRecordDbHelper.NewBltnsTable._ID);
        int txidCol     = cursor.getColumnIndexOrThrow(PublicRecordDbHelper.NewBltnsTable.COLUMN_TXID);
        int timeCol     = cursor.getColumnIndexOrThrow(PublicRecordDbHelper.NewBltnsTable.COLUMN_TIME);
        int msgCol      = cursor.getColumnIndexOrThrow(PublicRecordDbHelper.NewBltnsTable.COLUMN_MSG);
        int latCol      = cursor.getColumnIndexOrThrow(PublicRecordDbHelper.NewBltnsTable.COLUMN_LAT);
        int lonCol      = cursor.getColumnIndexOrThrow(PublicRecordDbHelper.NewBltnsTable.COLUMN_LON);
        int hCol        = cursor.getColumnIndexOrThrow(PublicRecordDbHelper.NewBltnsTable.COLUMN_H);
        int authCol     = cursor.getColumnIndexOrThrow(PublicRecordDbHelper.NewBltnsTable.COLUMN_AUTHOR);
        int numEndosCol = cursor.getColumnIndexOrThrow(PublicRecordDbHelper.NewBltnsTable.COLUMN_NUM_ENDOS);

        int id          = cursor.getInt(idCol);
        String txid     = cursor.getString(txidCol);
        long time       = cursor.getLong(timeCol);
        String msg      = cursor.getString(msgCol);
        double lat      = cursor.getDouble(latCol);
        double lon      = cursor.getDouble(lonCol);
        double h        = cursor.getDouble(hCol);
        String auth     = cursor.getString(authCol);
        int numEndos    = cursor.getInt(numEndosCol);

        final int colorBackground = resources.getColor(R.color.bg_bright);
        bltnVH.view.setCardBackgroundColor(colorBackground);
        bltnVH.view.setPreventCornerOverlap(false);
        bltnVH.view.setUseCompatPadding(true);
        try {
            String addrColor = Utils.colorAddr(auth);
            bltnVH.authorView.setTextColor( Color.parseColor(addrColor) );
            bltnVH.authorView.setTypeface(Typeface.MONOSPACE);
        } catch (NoSuchAlgorithmException e) {
            // that's ok
        }
        bltnVH.timeView.setTextColor(resources.getColor(R.color.fg_significant));

        bltnVH.view.setTag(id);
        String topics = Utils.listToHashtagString(Message.topicExtractor(msg));
        bltnVH.typeView.setText( topics.isEmpty() ? context.getString(R.string.profile_row_no_topics) : topics );
        bltnVH.numEndos.setText(Integer.toString(numEndos));
        bltnVH.messageView.setText(msg);;
        bltnVH.timeView.setText(DateUtils.formatDateTime(context, time*1000L, DateUtils.FORMAT_SHOW_DATE));
        bltnVH.authorView.setText( "@" + ( auth.length() < 6 ? auth : auth.substring(0, 6)) );
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.record_row_card, parent, false);
        return new BulletinViewHolder(itemView);
    }

    private static class BulletinViewHolder extends RecyclerView.ViewHolder {
        CardView view;
        TextView typeView;
        TextView messageView;
        TextView timeView;
        TextView authorView;
        TextView numEndos;

        public BulletinViewHolder(View itemView) {
            super(itemView);

            this.view   = (CardView) itemView;
            typeView    = (TextView) itemView.findViewById(R.id.record_row_type);
            messageView = (TextView) itemView.findViewById(R.id.record_row_message);
            timeView    = (TextView) itemView.findViewById(R.id.record_row_time);
            authorView  = (TextView) itemView.findViewById(R.id.record_row_author);
            numEndos    = (TextView) itemView.findViewById(R.id.record_row_num_endos);
        }
    }
}
