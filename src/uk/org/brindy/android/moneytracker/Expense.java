package uk.org.brindy.android.moneytracker;

import java.io.Serializable;

public class Expense implements Serializable {

	private static final long serialVersionUID = Double.doubleToLongBits(1.4);

	public static String KEY_DESC = "desc";
	public static String KEY_VALUE = "value";
	public static String KEY_ROWID = "id";

	private long id;

	private double value;

	private String description;

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

}
