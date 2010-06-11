package uk.org.brindy.android.moneytracker;

import java.text.NumberFormat;

import android.app.Dialog;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class CurrencyKeyboard implements OnClickListener {

	private EditText current;

	private Listener listener;

	private TextView amount;

	private Dialog dlg;

	private NumberFormat formatter;

	private String tmp;

	private double modifier;

	private Button done;

	private CurrencyKeyboard() {
		formatter = NumberFormat.getCurrencyInstance();
		modifier = Math.pow(10, formatter.getMaximumFractionDigits());
	}

	public void doShow(EditText text, CurrencyKeyboard.Listener listener) {
		this.listener = listener;

		if (null == dlg || text != current) {
			dlg = new Dialog(text.getContext());
			dlg.setContentView(R.layout.currencykeyboard);
			dlg.setTitle("Enter Currency Amount");

			addListener(dlg.findViewById(R.id.currency0));
			addListener(dlg.findViewById(R.id.currency1));
			addListener(dlg.findViewById(R.id.currency2));
			addListener(dlg.findViewById(R.id.currency3));
			addListener(dlg.findViewById(R.id.currency4));
			addListener(dlg.findViewById(R.id.currency5));
			addListener(dlg.findViewById(R.id.currency6));
			addListener(dlg.findViewById(R.id.currency7));
			addListener(dlg.findViewById(R.id.currency8));
			addListener(dlg.findViewById(R.id.currency9));
			addListener(dlg.findViewById(R.id.currencyC));

			amount = (TextView) dlg.findViewById(R.id.currencyValue);

			done = (Button) dlg.findViewById(R.id.currencyDone);
			done.setOnClickListener(this);
			done.setEnabled(false);

			Button cancel = (Button) dlg.findViewById(R.id.currencyCancel);
			cancel.setOnClickListener(this);
		}
		current = text;

		tmp = "";
		amount.setText(text.getText());

		dlg.show();
	}

	@Override
	public void onClick(View v) {

		Button b = (Button) v;

		if ("Cancel".equals(b.getText())) {
			dlg.dismiss();
		} else if ("Done".equals(b.getText().toString())) {
			current.setText(amount.getText());
			listener.setAmount(getAmount());
			dlg.dismiss();
		} else if ("C".equals(b.getText().toString())) {
			amount.setText(formatter.format(0));
			tmp = "";
			done.setEnabled(false);
		} else {
			addDigit(b.getText().toString());
		}

	}

	private void addDigit(String s) {

		done.setEnabled(true);
		tmp += s;
		amount.setText(formatter.format(getAmount()));

	}

	private double getAmount() {
		return Double.parseDouble(tmp) / modifier;
	}

	public static void show(EditText text, Listener listener) {
		instance.doShow(text, listener);
	}

	private void addListener(View v) {

		v.setOnClickListener(this);

	}

	private static final CurrencyKeyboard instance = new CurrencyKeyboard();

	public static interface Listener {
		public void setAmount(double d);
	}

}
