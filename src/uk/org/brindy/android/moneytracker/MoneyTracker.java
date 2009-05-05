package uk.org.brindy.android.moneytracker;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class MoneyTracker extends ListActivity {

	private static final int ACTIVITY_ADD_EXPENSE = 0;
	private static final int ACTIVITY_EDIT_EXPENSE = 1;

	private static final int ADD_EXPENSE_MENU_ID = Menu.FIRST;
	private static final int EDIT_EXPENSE_MENU_ID = Menu.FIRST + 1;
	private static final int DELETE_EXPENSE_MENU_ID = Menu.FIRST + 2;
	private static final int CLEAR_EXPENSES_MENU_ID = Menu.FIRST + 3;

	private EditText mDisposable;

	private TextView mRemaining;

	private ExpensesDbAdapter mDbAdapter;

	private MenuItem mDeleteItem;

	private Cursor mCursor;

	private NumberFormat mDecimalFormat = DecimalFormat.getCurrencyInstance();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mDbAdapter = new ExpensesDbAdapter(this);
		mDbAdapter.open();

		mRemaining = (TextView) findViewById(R.id.remaining);

		mDisposable = (EditText) findViewById(R.id.disposable);
		mDisposable.setText(mDbAdapter.getValue("disposable"));
		mDisposable.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				fillData();
				mDbAdapter.setValue("disposable", mDisposable.getText()
						.toString());
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
		});

		fillData();
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		mDeleteItem.setEnabled(-1 != getSelectedItemPosition());
		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, ADD_EXPENSE_MENU_ID, 0, R.string.menu_add_expense);

		mDeleteItem = menu.add(0, DELETE_EXPENSE_MENU_ID, 0,
				R.string.menu_delete_expense);
		mDeleteItem.setEnabled(false);

		menu.add(0, CLEAR_EXPENSES_MENU_ID, 0, R.string.menu_clear_expenses);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		boolean result = super.onMenuItemSelected(featureId, item);

		switch (item.getItemId()) {
		case ADD_EXPENSE_MENU_ID:
			onAddExpense();
			return true;

		case EDIT_EXPENSE_MENU_ID:
			onEditExpense();
			return true;

		case DELETE_EXPENSE_MENU_ID:
			onDeleteExpense();
			return true;

		case CLEAR_EXPENSES_MENU_ID:
			onClearExpenses();
			return true;
		}

		fillData();
		return result;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		onEditExpense();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_CANCELED) {
			return;
		}

		Bundle extras = data.getExtras();

		switch (requestCode) {
		case ACTIVITY_ADD_EXPENSE:
			double value = extras.getDouble(ExpensesDbAdapter.KEY_VALUE);
			String desc = extras.getString(ExpensesDbAdapter.KEY_DESC);
			mDbAdapter.createExpense(value, desc);
			break;

		case ACTIVITY_EDIT_EXPENSE:
			Long rowID = extras.getLong(ExpensesDbAdapter.KEY_ROWID);
			if (rowID != null) {
				double editValue = extras
						.getDouble(ExpensesDbAdapter.KEY_VALUE);
				String editDesc = extras.getString(ExpensesDbAdapter.KEY_DESC);
				mDbAdapter.updateExpense(rowID, editValue, editDesc);
			}
			break;
		}
		fillData();
	}

	private void calculateRemaining() {
		BigDecimal dec = new BigDecimal(0);
		if (mDisposable.getText().toString().trim().length() > 0) {
			dec = new BigDecimal(mDisposable.getText().toString());
		}

		Cursor c = mDbAdapter.fetchAllExpenses();
		if (c.moveToFirst()) {

			while (!c.isAfterLast()) {

				BigDecimal exp = new BigDecimal(c.getDouble(c
						.getColumnIndex(ExpensesDbAdapter.KEY_VALUE)));
				dec = dec.subtract(exp);

				if (!c.moveToNext()) {
					Log.w("MoneyTracker#calculateRemaining()",
							"Failed to move to next in cursor");
					break;
				}
			}

		}
		c.close();

		NumberFormat fmt = mDecimalFormat;
		mRemaining.setText(fmt.format(dec) + " remaining");
	}

	private void fillData() {
		calculateRemaining();

		// Get all of the rows from the database and create the item list
		mCursor = mDbAdapter.fetchAllExpenses();
		startManagingCursor(mCursor);

		// Create an array to specify the fields we want to display in the list
		// (only TITLE)
		String[] from = new String[] { ExpensesDbAdapter.KEY_VALUE,
				ExpensesDbAdapter.KEY_DESC };

		// and an array of the fields we want to bind those fields to (in this
		// case just text1)
		int[] to = new int[] { R.id.value, R.id.description };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(this,
				R.layout.expenses_row, mCursor, from, to) {

			@Override
			public void setViewText(TextView v, String text) {
				if (v.getId() == R.id.value) {
					NumberFormat fmt = mDecimalFormat;
					v.setText(fmt.format(Double.parseDouble(text)));
				} else {
					super.setViewText(v, text);
				}

			}

		};
		setListAdapter(cursorAdapter);
	}

	private void onAddExpense() {
		Intent i = new Intent(this, ExpenseEdit.class);
		startActivityForResult(i, ACTIVITY_ADD_EXPENSE);
	}

	private void onEditExpense() {
		Intent i = new Intent(this, ExpenseEdit.class);

		if (-1 != getSelectedItemPosition()) {
			mCursor.moveToPosition(getSelectedItemPosition());

			i.putExtra(ExpensesDbAdapter.KEY_ROWID, getSelectedItemId());

			i.putExtra(ExpensesDbAdapter.KEY_VALUE, mCursor.getDouble(mCursor
					.getColumnIndex(ExpensesDbAdapter.KEY_VALUE)));

			i.putExtra(ExpensesDbAdapter.KEY_DESC, mCursor.getString(mCursor
					.getColumnIndex(ExpensesDbAdapter.KEY_DESC)));

			startActivityForResult(i, ACTIVITY_EDIT_EXPENSE);
		}
	}

	private void onDeleteExpense() {
		if (-1 != getSelectedItemPosition()) {
			mDbAdapter.deleteExpense(getSelectedItemId());
			fillData();
		}
	}

	private void onClearExpenses() {
		mDbAdapter.deleteAllExpenses();
		fillData();
	}
}