package systems.soapbox.ombuds.client.omb.memory;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import systems.soapbox.ombuds.client.omb.model.response.BlockReferenceResponse;
import systems.soapbox.ombuds.client.omb.model.response.BulletinResponse;
import systems.soapbox.ombuds.client.omb.model.response.NewBulletinsResponse;

/**
 * Created by askuck on 1/19/16.
 */
public class PublicRecordDbHelper extends SQLiteOpenHelper {

    private static PublicRecordDbHelper sInstance;

    public static final String DB_NAME = "PublicRecord.db";
    public static final int DB_VERSION = 1;

    public static abstract class NewBltnsTable implements BaseColumns {
        public static final String TABLE_NAME       = "new_bulletins";
        public static final String COLUMN_TXID      = "txid";
        public static final String COLUMN_TIME      = "time";
        public static final String COLUMN_MSG       = "msg";
        public static final String COLUMN_LAT       = "lat";
        public static final String COLUMN_LON       = "lon";
        public static final String COLUMN_H         = "h";
        public static final String COLUMN_AUTHOR    = "author";
        public static final String COLUMN_NUM_ENDOS = "num_endos";
        public static final String COLUMN_BLOCK_REF = "blockRef";
    }

    public static abstract class BlockRefTable implements BaseColumns {
        public static final String TABLE_NAME       = "block_references";
        public static final String COLUMN_HASH      = "hash";
        public static final String COLUMN_HEIGHT    = "height";
        public static final String COLUMN_TIME      = "time";

    }

    public static final String SQL_WHERE_ARG = " = ? ";

    public PublicRecordDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static synchronized PublicRecordDbHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PublicRecordDbHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_NEW_BLTNS_TABLE = "CREATE TABLE IF NOT EXISTS " + NewBltnsTable.TABLE_NAME +
                "(" +
                NewBltnsTable.COLUMN_TXID       + " TEXT PRIMARY KEY, " +
                NewBltnsTable.COLUMN_TIME       + " INTEGER, " +
                NewBltnsTable.COLUMN_MSG        + " TEXT, " +
                NewBltnsTable.COLUMN_LAT        + " INTEGER, " +
                NewBltnsTable.COLUMN_LON        + " INTEGER, " +
                NewBltnsTable.COLUMN_H          + " INTEGER, " +
                NewBltnsTable.COLUMN_AUTHOR     + " TEXT, " +
                NewBltnsTable.COLUMN_NUM_ENDOS  + " INTEGER, " +
                NewBltnsTable.COLUMN_BLOCK_REF  + " TEXT, " +
                "FOREIGN KEY(" + NewBltnsTable.COLUMN_BLOCK_REF + ") REFERENCES " +
                BlockRefTable.TABLE_NAME + "(" + BlockRefTable.COLUMN_HASH + ")" +
                ")";

        String CREATE_BLOCK_REF_TABLE = "CREATE TABLE IF NOT EXISTS " + BlockRefTable.TABLE_NAME +
                "(" +
                BlockRefTable.COLUMN_HASH    + " TEXT PRIMARY KEY, " +
                BlockRefTable.COLUMN_HEIGHT  + " INTEGER, " +
                BlockRefTable.COLUMN_TIME    + " INTEGER" +
                ")";

        db.execSQL(CREATE_NEW_BLTNS_TABLE);
        db.execSQL(CREATE_BLOCK_REF_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < newVersion) {
            // unimplemented
        }
    }

    public void updateNewBulletins(NewBulletinsResponse response) {
        for(BulletinResponse bltn : response.bulletins) {
            maybeAddBulletin(bltn);
        }
    }

    private void maybeAddBulletin(BulletinResponse bltn) {
        maybeAddBlockRef(bltn.blockReference);

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(NewBltnsTable.COLUMN_TXID, bltn.txid);
            values.put(NewBltnsTable.COLUMN_TIME, bltn.timestamp);
            values.put(NewBltnsTable.COLUMN_MSG, bltn.msg);
            values.put(NewBltnsTable.COLUMN_LAT, bltn.loc.lat);
            values.put(NewBltnsTable.COLUMN_LON, bltn.loc.lon);
            values.put(NewBltnsTable.COLUMN_H, bltn.loc.h);
            values.put(NewBltnsTable.COLUMN_AUTHOR, bltn.author);
            values.put(NewBltnsTable.COLUMN_NUM_ENDOS, bltn.numEndos);
            values.put(NewBltnsTable.COLUMN_BLOCK_REF, bltn.blockReference.hash);

            db.insertOrThrow(NewBltnsTable.TABLE_NAME, null, values);
            db.setTransactionSuccessful();

        } catch (Exception e) {

        } finally {
            db.endTransaction();
        }
    }

    private void maybeAddBlockRef(BlockReferenceResponse blockRef) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(BlockRefTable.COLUMN_HASH, blockRef.hash);
            values.put(BlockRefTable.COLUMN_HEIGHT, blockRef.height);
            values.put(BlockRefTable.COLUMN_TIME, blockRef.timeStamp);

            db.insertOrThrow(BlockRefTable.TABLE_NAME, null, values);
            db.setTransactionSuccessful();

        } catch (Exception e) {

        } finally {
            db.endTransaction();
        }
    }
}
