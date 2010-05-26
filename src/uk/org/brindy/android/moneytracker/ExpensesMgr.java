package uk.org.brindy.android.moneytracker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ExpensesMgr implements Serializable {

	private static final long serialVersionUID = Double.doubleToLongBits(1.4);

	private AtomicLong nextID = new AtomicLong(0);

	private List<Expense> expenses = new ArrayList<Expense>();

	void saveExpense(Expense expense) {
		if (0 == expense.getId()) {
			expense.setId(nextID.incrementAndGet());
			expenses.add(expense);
		}

		// otherwise, assume we're updating an existing object and do nothing

	}

	boolean removeExpense(Expense expense) {
		return expenses.remove(expense);
	}

	List<Expense> getAllExpenses() {
		Collections.sort(expenses);
		return Collections.unmodifiableList(expenses);
	}

	Expense findExpense(long id) {
		for (Expense exp : expenses) {
			if (exp.getId() == id) {
				return exp;
			}
		}
		return null;
	}

	void clear() {
		expenses.clear();
		nextID.set(1);
	}

	double remaining(double disposable) {
		double remaining = disposable;
		for (Expense expense : expenses) {
			if (expense.isCredit()) {
				remaining += expense.getValue();
			} else {
				remaining -= expense.getValue();
			}
		}
		return remaining;
	}

}
