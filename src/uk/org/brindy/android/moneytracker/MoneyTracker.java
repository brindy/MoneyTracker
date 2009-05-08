package uk.org.brindy.android.moneytracker;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
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

	private ExpensesDbHelper mDbHelper;

	private MenuItem mDeleteItem;

	private NumberFormat mDecimalFormat = DecimalFormat.getCurrencyInstance();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mDbHelper = new ExpensesDbHelper(this);

		mRemaining = (TextView) findViewById(R.id.remaining);

		mDisposable = (EditText) findViewById(R.id.disposable);
		mDisposable.setText(Double.toString(mDbHelper.getDisposable()));
		mDisposable.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				double disposable = 0.0;
				if (s.toString().trim().length() > 0) {
					disposable = Double.parseDouble(s.toString());
				}
				mDbHelper.setDisposable(disposable);
				calculateRemaining();
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
		onEditExpense();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_CANCELED) {
			return;
		}

		Bundle extras = data.getExtras();
		Expense exp;

		switch (requestCode) {
		case ACTIVITY_ADD_EXPENSE:
			exp = new Expense();
			exp.setValue(extras.getDouble(Expense.KEY_VALUE));
			exp.setDescription(extras.getString(Expense.KEY_DESC));
			mDbHelper.createExpense(exp);
			break;

		case ACTIVITY_EDIT_EXPENSE:
			Long rowID = extras.getLong(Expense.KEY_ROWID);
			if (rowID != null) {
				exp = mDbHelper.findExpenseById(rowID);
				exp.setValue(extras.getDouble(Expense.KEY_VALUE));
				exp.setDescription(extras.getString(Expense.KEY_DESC));
				mDbHelper.updateExpense(exp);
			}
			break;
		}
		fillData();
	}

	private void calculateRemaining() {
		double remaining = 0.0;
		if (mDisposable.getText().toString().trim().length() > 0) {
			remaining = Double.parseDouble(mDisposable.getText().toString());
		}

		List<Expense> expenses = mDbHelper.fetchAllExpenses();
		for (Expense expense : expenses) {
			remaining -= expense.getValue();
		}

		NumberFormat fmt = mDecimalFormat;
		mRemaining.setText(fmt.format(remaining) + " remaining");
	}

	private void fillData() {
		calculateRemaining();

		final Context context = this;
		final List<Expense> expenses = mDbHelper.fetchAllExpenses();
		setListAdapter(new BaseAdapter() {

			public int getCount() {
				return expenses.size();
			}

			public Object getItem(int position) {
				return expenses.get(position);
			}

			public long getItemId(int position) {
				Expense expense = expenses.get(position);
				return expense.getId();
			}

			public View getView(int position, View convertView, ViewGroup parent) {
				Log.d(getClass().getName(), "getView()");

				Expense exp = expenses.get(position);
				View view = View.inflate(context, R.layout.expenses_row, null);

				TextView value = (TextView) view.findViewById(R.id.value);
				value.setText(mDecimalFormat.format(exp.getValue()));

				TextView desc = (TextView) view.findViewById(R.id.description);
				desc.setText(exp.getDescription());

				return view;
			}

		});
	}

	private void onAddExpense() {
		Intent i = new Intent(this, ExpenseEdit.class);
		startActivityForResult(i, ACTIVITY_ADD_EXPENSE);
	}

	private void onEditExpense() {
		Intent i = new Intent(this, ExpenseEdit.class);

		if (-1 != getSelectedItemPosition()) {
			Expense expense = mDbHelper.findExpenseById(getSelectedItemId());

			i.putExtra(Expense.KEY_ROWID, expense.getId());
			i.putExtra(Expense.KEY_VALUE, expense.getValue());
			i.putExtra(Expense.KEY_DESC, expense.getDescription());

			startActivityForResult(i, ACTIVITY_EDIT_EXPENSE);
		}
	}

	private void onDeleteExpense() {
		if (-1 != getSelectedItemPosition()) {
			mDbHelper.deleteExpense(getSelectedItemId());
			fillData();
		}
	}

	private void onClearExpenses() {
		mDbHelper.deleteAllExpenses();
		fillData();
	}
}