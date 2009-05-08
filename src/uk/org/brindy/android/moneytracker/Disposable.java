package uk.org.brindy.android.moneytracker;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class Disposable implements Serializable {

	private static final long serialVersionUID = Double.doubleToLongBits(1.4);

	private AtomicLong value = new AtomicLong();

	public void setValue(double d) {
		value.set(Double.doubleToLongBits(d));
	}

	public Double getValue() {
		return Double.longBitsToDouble(value.get());
	}

}
