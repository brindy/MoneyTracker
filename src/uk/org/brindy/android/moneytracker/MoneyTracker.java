package uk.org.brindy.android.moneytracker;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

import uk.org.brindy.android.moneytracker.CurrencyKeyboard.Listener;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class MoneyTracker extends ListActivity implements Listener {

	private static final int ACTIVITY_ADD_EXPENSE = 1;
	private static final int ACTIVITY_EDIT_EXPENSE = 2;

	private static final int EDIT_EXPENSE_MENU_ID = Menu.FIRST + 1;
	private static final int CLEAR_EXPENSES_MENU_ID = Menu.FIRST + 3;
	private static final int BACKUP_MENU_ID = Menu.FIRST + 4;
	private static final int BACKUP_SUBMENU_NOW_ID = Menu.FIRST + 5;
	private static final int BACKUP_SUBMENU_RESTORE_ID = Menu.FIRST + 6;

	private static final int CONTEXT_MENU_EDIT = Menu.FIRST + 7;
	private static final int CONTEXT_MENU_DELETE = Menu.FIRST + 8;

	private ListView mExpenses;

	private EditText mDisposable;

	private TextView mRemaining;

	private ExpensesDbHelper mDbHelper;

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
		mExpenses.setLongClickable(true);
		registerForContextMenu(mExpenses);

		mDbHelper = new ExpensesDbHelper(this);

		mRemaining = (TextView) findViewById(R.id.remaining);

		mDisposable = (EditText) findViewById(R.id.disposable);
		mDisposable.setText(FORMATTER.format(mDbHelper.getDisposable()));
		mDisposable.setInputType(0);

		mDisposable.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CurrencyKeyboard.show(mDisposable, MoneyTracker.this);
			}
		});
		mDisposable.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					CurrencyKeyboard.show(mDisposable, MoneyTracker.this);
				}
			}
		});

		ImageButton addButton = (ImageButton) findViewById(R.id.add);
		addButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				onAddExpense();
			}
		});

		fillData();
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, CONTEXT_MENU_EDIT, 0, "Edit");
		menu.add(0, CONTEXT_MENU_DELETE, 0, "Delete");
	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case CONTEXT_MENU_EDIT:
			doEdit(info.position);
			return true;
		case CONTEXT_MENU_DELETE:
			onDeleteExpense(info.position);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

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
		case EDIT_EXPENSE_MENU_ID:
			onEditExpense();
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
	public void setAmount(double d) {
		mDbHelper.setDisposable(d);
		calculateRemaining();
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

		switch (requestCode) {
		case ACTIVITY_ADD_EXPENSE:
			onAddExpenseResult(extras);
			break;

		case ACTIVITY_EDIT_EXPENSE:
			switch (resultCode) {
			default:
				onUpdateExpenseResult(extras);
				break;

			case ExpenseEdit.RESULT_DELETE:
				onDeleteExpenseResult(extras);
				break;
			}
			break;
		}
		fillData();
	}

	private void onDeleteExpenseResult(Bundle extras) {
		Long rowID = extras.getLong(Expense.KEY_ROWID);
		mDbHelper.deleteExpense(rowID);
		fillData();
	}

	private void onUpdateExpenseResult(Bundle extras) {
		Expense exp;
		Long rowID = extras.getLong(Expense.KEY_ROWID);
		if (rowID != null) {
			exp = mDbHelper.findExpenseById(rowID);
			exp.setValue(extras.getDouble(Expense.KEY_VALUE));
			exp.setDescription(extras.getString(Expense.KEY_DESC));
			exp.setDate(new Date(extras.getLong(Expense.KEY_DATE)));
			exp.setCredit(extras.getBoolean(Expense.KEY_CREDIT));
			mDbHelper.updateExpense(exp);
		}
	}

	private void onAddExpenseResult(Bundle extras) {
		Expense exp;
		exp = new Expense();
		exp.setValue(extras.getDouble(Expense.KEY_VALUE));
		exp.setDescription(extras.getString(Expense.KEY_DESC));
		exp.setDate(new Date(extras.getLong(Expense.KEY_DATE)));
		exp.setCredit(extras.getBoolean(Expense.KEY_CREDIT));
		mDbHelper.createExpense(exp);
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

				final Expense exp = expenses.get(position);
				View view = convertView;
				if (null == view) {
					view = View.inflate(context, R.layout.expenses_row, null);
				}

				TextView value = (TextView) view.findViewById(R.id.value);
				String text = FORMATTER.format(exp.getValue());

				if (exp.isCredit()) {
					text = "+" + text;
				} else {
					text = "-" + text;
				}

				value.setText(text);

				TextView desc = (TextView) view.findViewById(R.id.description);
				desc.setText(exp.getDescription());

				TextView date = (TextView) view.findViewById(R.id.date);
				date.setText(Formatter.format(exp.getDate()));

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
		i.putExtra(Expense.KEY_DATE, expense.getDate().getTime());
		i.putExtra(Expense.KEY_CREDIT, expense.isCredit());

		startActivityForResult(i, ACTIVITY_EDIT_EXPENSE);

	}

	private void onDeleteExpense(final int position) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Delete this item?");
		builder.setItems(new String[] { "Yes", "No" },
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (which == 0) {
							long id = mExpenses.getItemIdAtPosition(position);
							mDbHelper.deleteExpense(id);
							fillData();
						}
					}
				});

		builder.show();

	}

	private void onClearExpenses() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Clear all expenses?");
		builder.setItems(new String[] { "Yes", "No" },
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (which == 0) {
							mDbHelper.deleteAllExpenses();
							fillData();
						}
					}
				});

		builder.show();
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