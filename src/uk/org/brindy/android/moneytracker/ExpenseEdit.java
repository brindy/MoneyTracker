package uk.org.brindy.android.moneytracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class ExpenseEdit extends Activity {

	private EditText mValueText;

	private EditText mDescText;

	private Long mRowId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_expense);

		mValueText = (EditText) findViewById(R.id.value);
		mDescText = (EditText) findViewById(R.id.description);

		Button confirmButton = (Button) findViewById(R.id.confirm);

		mRowId = null;
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			double value = extras.getDouble(Expense.KEY_VALUE);
			String desc = extras.getString(Expense.KEY_DESC);
			mRowId = extras.getLong(Expense.KEY_ROWID);

			mValueText.setText(Double.toString(value));

			if (desc != null) {
				mDescText.setText(desc);
			}
		}

		confirmButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				Bundle bundle = new Bundle();

				bundle.putString(Expense.KEY_DESC, mDescText.getText()
						.toString());
				bundle.putDouble(Expense.KEY_VALUE, Double
						.parseDouble(mValueText.getText().toString()));
				if (mRowId != null) {
					bundle.putLong(Expense.KEY_ROWID, mRowId);
				}

				Intent mIntent = new Intent();
				mIntent.putExtras(bundle);
				setResult(RESULT_OK, mIntent);
				finish();
			}

		});

	}

}
