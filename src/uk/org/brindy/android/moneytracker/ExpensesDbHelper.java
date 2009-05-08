package uk.org.brindy.android.moneytracker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.List;

import android.content.Context;
import android.util.Log;

public class ExpensesDbHelper {

	private static final String APP_VERSION = "1.4";

	private ExpensesMgr expenses;

	private Disposable disp;

	private Context mCtx;

	public ExpensesDbHelper(Context ctx) {
		this.mCtx = ctx;
		
		if (!APP_VERSION.equals(findVersion())) {
			createFileName(ExpensesMgr.class.getName(), ctx).delete();
			createFileName(Disposable.class.getName(), ctx).delete();

			// delete the old stuff...
			File db4oDir = ctx.getDir("db4o", Context.MODE_PRIVATE);
			new File(db4oDir, "expenses.yap").delete();
			db4oDir.delete();
		}

		// load required objects
		if (null == (expenses = loadObject(ExpensesMgr.class))) {
			saveObject(expenses = new ExpensesMgr());
		}

		if (null == (disp = loadObject(Disposable.class))) {
			saveObject(disp = new Disposable());
		}
	}

	private String findVersion() {
		File dir = mCtx.getDir("objects", Context.MODE_PRIVATE);
		File ver = new File(dir, "version.txt");
		if (!ver.exists()) {
			PrintWriter out = null;
			try {
				out = new PrintWriter(new FileOutputStream(ver));
				out.println(APP_VERSION);
				out.close();
				return APP_VERSION;
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			} finally {
				if (out != null) {
					out.close();
				}
			}
		}

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(ver));
			return reader.readLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (null != reader) {
				try {
					reader.close();
				} catch (IOException e) {
					Log.e(getClass().getName(), e.getMessage());
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T loadObject(Class<T> cls) {
		Context ctx = mCtx;
		File f = createFileName(cls.getName(), ctx);

		if (f.exists()) {
			ObjectInputStream in = null;
			try {
				in = new ObjectInputStream(new FileInputStream(f));
				return (T) in.readObject();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			} finally {
				if (null != in) {
					try {
						in.close();
					} catch (IOException e) {
						Log.e(getClass().getName(), e.getMessage());
					}
				}
			}
		}

		return null;
	}

	private File createFileName(String name, Context ctx) {
		File f = new File(ctx.getDir("objects", Context.MODE_PRIVATE), name
				+ ".obj");
		return f;
	}

	private void saveObject(Object o) {
		Context ctx = mCtx;
		File f = createFileName(o.getClass().getName(), ctx);

		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(new FileOutputStream(f));
			out.writeObject(o);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (null != out) {
				try {
					out.close();
				} catch (IOException e) {
					// ignore
					Log.e(getClass().getName(), e.getMessage());
				}
			}
		}
	}

	public long createExpense(Expense expense) {
		expenses.saveExpense(expense);
		saveObject(expenses);
		return expense.getId();
	}

	public boolean deleteExpense(long expenseId) {
		boolean result = expenses
				.removeExpense(expenses.findExpense(expenseId));

		if (result) {
			saveObject(expenses);
		}

		return result;
	}

	public List<Expense> fetchAllExpenses() {
		return expenses.getAllExpenses();
	}

	public Expense findExpenseById(long expenseId) {
		return expenses.findExpense(expenseId);
	}

	public boolean updateExpense(Expense exp) {
		expenses.saveExpense(exp);
		saveObject(expenses);
		return true;
	}

	public void deleteAllExpenses() {
		expenses.clear();
		saveObject(expenses);
	}

	public void setDisposable(double disposable) {
		disp.setValue(disposable);
		saveObject(disp);
	}

	public double getDisposable() {
		return disp.getValue();
	}
}
