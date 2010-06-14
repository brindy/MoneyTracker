package uk.org.brindy.android.moneytracker;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;

import uk.org.brindy.android.moneytracker.CurrencyKeyboard.Listener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

public class ExpenseEdit extends Activity implements Listener {

	public static final int DATE_DIALOG_ID = 0x0;
	public static final int TIME_DIALOG_ID = 0x1;

	public static final int RESULT_DELETE = 0x55;

	private ToggleButton mExpenseToggle;

	private EditText mValueText;

	private EditText mDescText;

	private EditText mDateText;

	private Long mRowId;

	private Calendar mCalendar = Calendar.getInstance();

	private InputMethodManager mIMM;

	private NumberFormat FORMATTER = NumberFormat.getCurrencyInstance();

	private boolean shouldShow;

	// the callback received when the user "sets" the date in the dialog
	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			mCalendar.set(Calendar.YEAR, year);
			mCalendar.set(Calendar.MONTH, monthOfYear);
			mCalendar.set(Calendar.DATE, dayOfMonth);
			updateDisplay();
			showDialog(TIME_DIALOG_ID);
		}

	};

	private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
			mCalendar.set(Calendar.MINUTE, minute);
			updateDisplay();
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_expense);

		mValueText = (EditText) findViewById(R.id.value);
		mValueText.setInputType(0);
		mValueText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CurrencyKeyboard.show(mValueText, ExpenseEdit.this);
			}
		});
		mValueText.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus && shouldShow) {
					CurrencyKeyboard.show(mValueText, ExpenseEdit.this);
				}
				shouldShow = true;
			}
		});

		mDescText = (EditText) findViewById(R.id.description);
		mDateText = (EditText) findViewById(R.id.date);
		mExpenseToggle = (ToggleButton) findViewById(R.id.credit);

		Button confirmButton = (Button) findViewById(R.id.confirm);
		Button deleteButton = (Button) findViewById(R.id.delete);
		deleteButton.setVisibility(View.GONE);

		mRowId = null;
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			deleteButton.setVisibility(View.VISIBLE);

			double value = Math.abs(extras.getDouble(Expense.KEY_VALUE));
			String desc = extras.getString(Expense.KEY_DESC);
			mRowId = extras.getLong(Expense.KEY_ROWID);
			long time = extras.getLong(Expense.KEY_DATE);
			boolean credit = extras.getBoolean(Expense.KEY_CREDIT);

			mCalendar.setTimeInMillis(time);

			mValueText.setText(FORMATTER.format(value));
			mExpenseToggle.setChecked(credit);

			if (desc != null) {
				mDescText.setText(desc);
			}
		} else {
			shouldShow = true;
			mValueText.setText(FORMATTER.format(0.0));
		}

		confirmButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				Bundle bundle = new Bundle();

				bundle.putString(Expense.KEY_DESC, mDescText.getText()
						.toString());
				try {
					bundle.putDouble(Expense.KEY_VALUE, FORMATTER.parse(
							mValueText.getText().toString()).doubleValue());
				} catch (ParseException e) {
					throw new RuntimeException(e);
				}
				if (mRowId != null) {
					bundle.putLong(Expense.KEY_ROWID, mRowId);
				}

				bundle.putLong(Expense.KEY_DATE, mCalendar.getTimeInMillis());
				bundle.putBoolean(Expense.KEY_CREDIT, mExpenseToggle
						.isChecked());

				Intent mIntent = new Intent();
				mIntent.putExtras(bundle);
				setResult(RESULT_OK, mIntent);
				finish();
			}

		});

		deleteButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						ExpenseEdit.this);
				builder.setTitle("Delete this item?");
				builder.setItems(new String[] { "Yes", "No" },
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								if (which == 0) {
									delete();
								}
							}
						});

				builder.show();
			}
		});

		mDateText.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(DATE_DIALOG_ID);
			}
		});

		mDateText.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					Toast.makeText(ExpenseEdit.this,
							"Click or touch again to edit", Toast.LENGTH_SHORT)
							.show();
					mIMM.hideSoftInputFromWindow(mDateText.getWindowToken(), 0);
				}
			}
		});

		mIMM = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

		updateDisplay();
	}

	@Override
	public void setAmount(double d) {
		mValueText.setText(FORMATTER.format(d));
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DATE_DIALOG_ID:
			return new DatePickerDialog(this, mDateSetListener, mCalendar
					.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
					mCalendar.get(Calendar.DATE));
		case TIME_DIALOG_ID:
			return new TimePickerDialog(this, mTimeSetListener, mCalendar
					.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE),
					true);
		}
		return null;
	}

	private void updateDisplay() {
		mDateText.setText(Formatter.format(mCalendar.getTime()));
	}

	private void delete() {
		Bundle bundle = new Bundle();
		bundle.putLong(Expense.KEY_ROWID, mRowId);
		Intent mIntent = new Intent();
		mIntent.putExtras(bundle);
		setResult(RESULT_DELETE, mIntent);
		finish();
	}

}
