package uk.org.brindy.android.moneytracker;

import java.io.Serializable;
import java.util.Date;

public class Expense implements Serializable, Comparable<Expense> {

	private static final long serialVersionUID = Double.doubleToLongBits(1.4);

	public static String KEY_DESC = "desc";
	public static String KEY_VALUE = "value";
	public static String KEY_ROWID = "id";
	public static String KEY_DATE = "date";

	private long id;

	private double value;

	private String description;

	// initialise to historic date for legacy installations
	private Date date = new Date(0);

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public double getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Date getDate() {
		return date;
	}

	public int compareTo(Expense another) {
		return getDate().compareTo(another.getDate());
	}
}
