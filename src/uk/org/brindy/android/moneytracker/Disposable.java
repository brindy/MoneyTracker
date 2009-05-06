package uk.org.brindy.android.moneytracker;

import java.util.concurrent.atomic.AtomicLong;

public class Disposable {

	private AtomicLong value = new AtomicLong();

	public void setValue(double d) {
		value.set(Double.doubleToLongBits(d));
	}

	public Double getValue() {
		return Double.longBitsToDouble(value.get());
	}

}
