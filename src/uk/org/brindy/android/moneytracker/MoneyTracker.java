package uk.org.brindy.android.moneytracker;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MoneyTracker extends ListActivity {

	private static final int ACTIVITY_ADD_EXPENSE = 1;
	private static final int ACTIVITY_EDIT_EXPENSE = 2;

	private static final int ADD_EXPENSE_MENU_ID = Menu.FIRST;
	private static final int EDIT_EXPENSE_MENU_ID = Menu.FIRST + 1;
	private static final int DELETE_EXPENSE_MENU_ID = Menu.FIRST + 2;
	private static final int CLEAR_EXPENSES_MENU_ID = Menu.FIRST + 3;
	private static final int BACKUP_MENU_ID = Menu.FIRST + 4;
	private static final int BACKUP_SUBMENU_NOW_ID = Menu.FIRST + 5;
	private static final int BACKUP_SUBMENU_RESTORE_ID = Menu.FIRST + 6;

	private ListView mExpenses;

	private EditText mDisposable;

	private TextView mRemaining;

	private ExpensesDbHelper mDbHelper;

	private MenuItem mDeleteItem;

	private static final DecimalFormat FORMATTER = (DecimalFormat) DecimalFormat
			.getCurrencyInstance();
	static {
		FORMATTER.setNegativePrefix(FORMATTER.getCurrency().getSymbol());
		FORMATTER.setNegativeSuffix("");
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mExpenses = (ListView) findViewById(android.R.id.list);

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

		SubMenu sub = menu.addSubMenu(0, BACKUP_MENU_ID, 0,
				R.string.menu_backup);

		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {

			sub.add(BACKUP_MENU_ID, BACKUP_SUBMENU_NOW_ID, 0,
					R.string.menu_backup_now);

			sub.add(BACKUP_MENU_ID, BACKUP_SUBMENU_RESTORE_ID, 0,
					R.string.menu_backup_restore);

		}

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

		case BACKUP_MENU_ID:
			return !isMediaReady();

		case BACKUP_SUBMENU_NOW_ID:
			onBackupNow();
			return true;

		case BACKUP_SUBMENU_RESTORE_ID:
			onBackupRestore();
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

	public static String formatRemaining(double remaining) {
		return FORMATTER.format(remaining);
	}

	private void calculateRemaining() {
		double remaining = 0.0;
		if (mDisposable.getText().toString().trim().length() > 0) {
			remaining = this.mDbHelper.remaining();
		}
		String level = remaining < 0.0 ? " over budget" : " remaining";
		mRemaining.setText(formatRemaining(remaining) + level);
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

			public View getView(final int position, View convertView,
					ViewGroup parent) {
				Expense exp = expenses.get(position);
				View view = View.inflate(context, R.layout.expenses_row, null);

				View.OnClickListener listener = new View.OnClickListener() {
					public void onClick(View v) {
						doEdit(position);
					}
				};

				ImageView edit = (ImageView) view.findViewById(R.id.edit);
				edit.setOnClickListener(listener);

				TextView value = (TextView) view.findViewById(R.id.value);
				value.setText(FORMATTER.format(exp.getValue()));

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
		if (-1 != getSelectedItemPosition()) {
			doEdit(getSelectedItemPosition());
		}
	}

	private void doEdit(int position) {
		Intent i = new Intent(this, ExpenseEdit.class);
		Expense expense = mDbHelper.findExpenseById(mExpenses
				.getItemIdAtPosition(position));

		i.putExtra(Expense.KEY_ROWID, expense.getId());
		i.putExtra(Expense.KEY_VALUE, expense.getValue());
		i.putExtra(Expense.KEY_DESC, expense.getDescription());

		startActivityForResult(i, ACTIVITY_EDIT_EXPENSE);

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

	private void onBackupNow() {
		if (isMediaReady()) {
			final File backup = getBackupFile();
			if (backup.exists()) {
				final Context ctx = this;
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Overwrite existing backup?");
				builder.setItems(new String[] { "Yes", "No" },
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								if (which == 0) {
									backup(backup);
								} else {
									Toast.makeText(ctx, "Backup cancelled",
											Toast.LENGTH_LONG).show();
								}
							}
						});
				builder.show();
			} else {
				backup(backup);
			}
		}
	}

	private void restore(final File backup, final Context ctx) {
		try {
			mDbHelper.restore(backup);
			mDisposable.setText(String.valueOf(mDbHelper.getDisposable()));
			fillData();
			Toast.makeText(ctx, R.string.backup_restored, Toast.LENGTH_LONG)
					.show();
		} catch (Exception ex) {
			Toast.makeText(ctx, ex.getMessage(), Toast.LENGTH_LONG);
		}
	}

	private void backup(File backup) {
		try {
			mDbHelper.backup(backup);
			Toast.makeText(this, R.string.backup_complete, Toast.LENGTH_LONG)
					.show();
		} catch (IOException e) {
			Toast.makeText(
					this,
					"Unexpected error while backing up (" + e.getMessage()
							+ ")", Toast.LENGTH_LONG).show();
		}
	}

	private File getBackupFile() {
		File dir = Environment.getExternalStorageDirectory();
		File backup = new File(dir, "MoneyTracker.backup");
		return backup;
	}

	private void onBackupRestore() {
		if (isMediaReady()) {
			final File backup = getBackupFile();
			if (!backup.exists()) {
				Toast.makeText(this, R.string.backup_does_not_exist,
						Toast.LENGTH_LONG).show();
			} else {
				final Context ctx = this;
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Really restore from backup?");
				builder.setItems(new String[] { "Yes", "No" },
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								if (which == 0) {
									restore(backup, ctx);
								} else {
									Toast.makeText(ctx, "Restore cancelled",
											Toast.LENGTH_LONG).show();
								}
							}

						});
				builder.show();
			}
		}
	}

	private boolean isMediaReady() {

		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {
			return true;
		}

		Toast.makeText(this, R.string.no_external_media, Toast.LENGTH_LONG)
				.show();
		return false;
	}
}