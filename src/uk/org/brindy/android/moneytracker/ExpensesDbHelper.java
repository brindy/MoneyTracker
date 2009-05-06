package uk.org.brindy.android.moneytracker;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.config.Configuration;

public class ExpensesDbHelper {

	private static ObjectContainer oc;

	private Disposable disp;

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public ExpensesDbHelper(Context ctx) {
		openDB(ctx);

		ObjectSet<Disposable> set = oc.query(Disposable.class);
		if (set.size() == 0) {
			disp = new Disposable();
			setDisposable(0.0);
		} else {
			disp = set.get(0);
		}
	}

	private static void openDB(final Context ctx) {
		if (oc == null || oc.ext().isClosed()) {
			// configure the db
			Configuration conf = Db4o.newConfiguration();
			conf.updateDepth(5);

			// open the file
			File dbFile = ctx.getDir("db4o", Context.MODE_PRIVATE);
			dbFile = new File(dbFile, "expenses.yap");
			try {
				oc = Db4o.openFile(conf, dbFile.getCanonicalPath());
			} catch (Exception e) {
				Log.e(ExpensesDbHelper.class.getName(), e.toString(), e);
				throw new RuntimeException(e);
			}
		}
	}

	public long createExpense(Expense expense) {
		oc.store(expense);
		oc.commit();

		expense.setId(oc.ext().getID(expense));
		oc.store(expense);
		oc.commit();

		return expense.getId();
	}

	public boolean deleteExpense(long rowId) {
		Expense expense = oc.ext().getByID(rowId);
		if (null != expense) {
			oc.delete(expense);
			oc.commit();
			return true;
		}
		return false;
	}

	public List<Expense> fetchAllExpenses() {
		return oc.query(Expense.class);
	}

	public Expense findExpenseById(long rowId) {
		return oc.ext().getByID(rowId);
	}

	public boolean updateExpense(Expense exp) {
		oc.store(exp);
		oc.commit();
		return true;
	}

	public void deleteAllExpenses() {
		for (Expense exp : fetchAllExpenses()) {
			oc.delete(exp);
		}
		oc.commit();
	}

	public void setDisposable(double disposable) {
		disp.setValue(disposable);
		oc.store(disp);
		oc.commit();
	}

	public double getDisposable() {
		return disp.getValue();
	}
}
