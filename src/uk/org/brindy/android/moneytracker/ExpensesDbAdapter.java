/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package uk.org.brindy.android.moneytracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Simple notes database access helper class. Defines the basic CRUD operations
 * for the notepad example, and gives the ability to list all notes as well as
 * retrieve or modify a specific note.
 * 
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 */
public class ExpensesDbAdapter {

	public static final String KEY_VALUE = "value";
	public static final String KEY_DESC = "description";
	public static final String KEY_ROWID = "_id";

	private static final String VALUEMAP_TABLE = "valuemap";
	private static final String VALUEMAP_KEY_VALUE = "value";
	private static final String VALUEMAP_KEY_NAME = "name";
	private static final String CREATE_VALUEMAP_TABLE = "create table valuemap (name string not null, value string not null)";

	private static final String TAG = "ExpensesDbAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	/**
	 * Database creation sql statement
	 */
	private static final String CREATE_EXPENSES_TABLE = "create table expenses (_id integer primary key autoincrement, "
			+ "value real not null, description text not null);";

	private static final String DATABASE_NAME = "data";
	private static final String DATABASE_TABLE = "expenses";
	private static final int DATABASE_VERSION = 5;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_EXPENSES_TABLE);
			db.execSQL(CREATE_VALUEMAP_TABLE);

			ContentValues values = new ContentValues();
			values.put(VALUEMAP_KEY_NAME, "disposable");
			values.put(VALUEMAP_KEY_VALUE, "0");

			db.insert(VALUEMAP_TABLE, null, values);

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + VALUEMAP_TABLE);
			onCreate(db);
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public ExpensesDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	/**
	 * Open the notes database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public ExpensesDbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	public String getValue(String name) {
		name = name.replace("'", "\'");

		Cursor mCursor = null;
		try {
			mCursor = mDb
					.query(true, VALUEMAP_TABLE,
							new String[] { VALUEMAP_KEY_VALUE },
							VALUEMAP_KEY_NAME + "=\'" + name + "\'", null,
							null, null, null, null);
			if (mCursor != null) {
				mCursor.moveToFirst();
			}
			return mCursor.getString(0);
		} finally {
			if (null != mCursor) {
				mCursor.close();
			}
		}
	}

	public boolean setValue(String name, String value) {
		name = name.replace("'", "\'");
		value = value.replace("'", "\'");

		ContentValues args = new ContentValues();
		args.put("value", value);
		return mDb.update(VALUEMAP_TABLE, args, VALUEMAP_KEY_NAME + "=\'"
				+ name + "\'", null) > 0;
	}

	/**
	 * Create a new note using the value and description provided. If the note
	 * is successfully created return the new rowId for that note, otherwise
	 * return a -1 to indicate failure.
	 * 
	 * @param value
	 *            the value of the note
	 * @param description
	 *            the description of the note
	 * @return rowId or -1 if failed
	 */
	public long createExpense(Double value, String description) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_VALUE, value);
		initialValues.put(KEY_DESC, description);

		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}

	/**
	 * Delete the note with the given rowId
	 * 
	 * @param rowId
	 *            id of note to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteExpense(long rowId) {

		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Return a Cursor over the list of all notes in the database
	 * 
	 * @return Cursor over all notes
	 */
	public Cursor fetchAllExpenses() {

		return mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_VALUE,
				KEY_DESC }, null, null, null, null, null);
	}

	/**
	 * Return a Cursor positioned at the note that matches the given rowId
	 * 
	 * @param rowId
	 *            id of note to retrieve
	 * @return Cursor positioned to matching note, if found
	 * @throws SQLException
	 *             if note could not be found/retrieved
	 */
	public Cursor fetchExpense(long rowId) throws SQLException {

		Cursor mCursor =

		mDb.query(true, DATABASE_TABLE, new String[] { KEY_ROWID, KEY_VALUE,
				KEY_DESC }, KEY_ROWID + "=" + rowId, null, null, null, null,
				null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	/**
	 * Update the note using the details provided. The note to be updated is
	 * specified using the rowId, and it is altered to use the value and
	 * description values passed in
	 * 
	 * @param rowId
	 *            id of note to update
	 * @param value
	 *            value to set note value to
	 * @param description
	 *            value to set note description to
	 * @return true if the note was successfully updated, false otherwise
	 */
	public boolean updateExpense(long rowId, double value, String description) {
		ContentValues args = new ContentValues();
		args.put(KEY_VALUE, value);
		args.put(KEY_DESC, description);

		return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public void deleteAllExpenses() {
		mDb.execSQL("delete from " + DATABASE_TABLE);
	}
}
