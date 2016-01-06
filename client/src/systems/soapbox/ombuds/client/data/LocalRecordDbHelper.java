package systems.soapbox.ombuds.client.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;

import systems.soapbox.ombuds.lib.record.Bulletin;
import systems.soapbox.ombuds.lib.record.Endorsement;

/**
 * Created by askuck on 1/5/16.
 */
public class LocalRecordDbHelper extends SQLiteOpenHelper {

    private static LocalRecordDbHelper sInstance;

    public static final String DB_NAME = "LocalRecord.db";
    public static final int DB_VERSION = 1;

    public static abstract class BltnTable implements BaseColumns {
        public static final String TABLE_NAME = "bltns";
        public static final String COLUMN_TXID = "txid";
        public static final String COLUMN_TIME = "time";
        public static final String COLUMN_MSG = "msg";
        public static final String COLUMN_LAT = "lat";
        public static final String COLUMN_LON = "lon";
        public static final String COLUMN_H = "h";
    }

    public static abstract class EndoTable implements BaseColumns {
        public static final String TABLE_NAME = "endos";
        public static final String COLUMN_TXID = "txid";
        public static final String COLUMN_TIME = "time";
        public static final String COLUMN_BLTN_ID = "bltn_id";
    }

    public static final String SQL_WHERE_ARG = " = ? ";

    public LocalRecordDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static synchronized LocalRecordDbHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LocalRecordDbHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_BLTNS_TABLE = "CREATE TABLE IF NOT EXISTS " + BltnTable.TABLE_NAME +
                "(" +
//                BltnTable._ID           + " INTEGER PRIMARY KEY, " +
                BltnTable.COLUMN_TXID   + " TEXT PRIMARY KEY," +
                BltnTable.COLUMN_TIME   + " INTEGER, " +
                BltnTable.COLUMN_MSG    + " TEXT, " +
                BltnTable.COLUMN_LAT    + " INTEGER, " +
                BltnTable.COLUMN_LON    + " INTEGER, " +
                BltnTable.COLUMN_H      + " INTEGER " +
                ")";

        String CREATE_ENDOS_TABLE = "CREATE TABLE IF NOT EXISTS " + EndoTable.TABLE_NAME +
                "(" +
                EndoTable.COLUMN_TXID       + " TEXT PRIMARY KEY," +
                EndoTable.COLUMN_TIME       + " INTEGER," +
                EndoTable.COLUMN_BLTN_ID    + " TEXT" +
                ")";

        db.execSQL(CREATE_BLTNS_TABLE);
        db.execSQL(CREATE_ENDOS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < newVersion) {
            // unimplemented
        }
    }

    public void add(Transaction tx, Bulletin bltn) {
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(BltnTable.COLUMN_TXID, tx.getHashAsString());
            values.put(BltnTable.COLUMN_TIME, bltn.getTimestamp().get());
            values.put(BltnTable.COLUMN_MSG, bltn.getMessage().get());

            values.put(BltnTable.COLUMN_LAT, bltn.getLocation().getLat());
            values.put(BltnTable.COLUMN_LON, bltn.getLocation().getLon());
            values.put(BltnTable.COLUMN_H, bltn.getLocation().getH());

            db.insertOrThrow(BltnTable.TABLE_NAME, null, values);
            db.setTransactionSuccessful();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    public void add(Transaction tx, Endorsement endo) {
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(EndoTable.COLUMN_TXID, tx.getHashAsString());
            values.put(EndoTable.COLUMN_TIME, endo.getTimestamp().get());
            values.put(EndoTable.COLUMN_BLTN_ID, endo.getBulletinId().getHash().toString());

            db.insertOrThrow(EndoTable.TABLE_NAME, null, values);
            db.setTransactionSuccessful();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    public boolean isRecord(final Sha256Hash txid) {
        return isBulletin(txid);
        //return isBulletin(txid) || isEndorsement(txid);
    }

    public boolean isBulletin(final Sha256Hash txid) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                BltnTable.COLUMN_TXID,
        };

        String selection = BltnTable.COLUMN_TXID + SQL_WHERE_ARG;
        String[] selectionArgs = {txid.toString()};

        Cursor c = db.query(
                BltnTable.TABLE_NAME,                   // The table to query
                projection,                             // The columns to return
                selection,                              // The columns for the WHERE clause
                selectionArgs,                          // The values for the WHERE clause
                null,                                   // don't group the rows
                null,                                   // don't filter by row groups
                null                                    // The sort order
        );

        return c.getCount() > 0;
    }

    public boolean isEndorsement(final Sha256Hash txid) {
        throw new UnsupportedOperationException();
    }

}
