package uk.org.brindy.android.moneytracker;

import java.util.Calendar;
import java.util.Date;

import android.text.format.DateFormat;

public class Formatter {

	public static String format(Date date) {

		Calendar cal = Calendar.getInstance();
		int todayDate = cal.get(Calendar.DAY_OF_YEAR);
		int todayYear = cal.get(Calendar.YEAR);

		cal.setTime(date);
		int testDate = cal.get(Calendar.DAY_OF_YEAR);
		int testYear = cal.get(Calendar.YEAR);

		String text = null;
		if (todayYear == testYear) {
			if (todayDate == testDate) {
				text = "Today at";
			} else if (todayDate - 1 == testDate) {
				text = "Yesterday at";
			}
		}

		if (null == text) {
			text = DateFormat.format("MMMM dd, yyy", date).toString();
		}

		return text + " " + DateFormat.format("kk:mm", date).toString();
	}

}
