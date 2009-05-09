package uk.org.brindy.android.moneytracker;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class MoneyTrackerWidgetProvider extends AppWidgetProvider {

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
	}

	@Override
	public void onEnabled(Context context) {
		// broadcast an update
		context.sendBroadcast(new Intent(
				AppWidgetManager.ACTION_APPWIDGET_UPDATE));
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		updateWidget(context, appWidgetManager);
	}

	private void updateWidget(Context context, AppWidgetManager appWidgetManager) {
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.widget);

		Intent defineIntent = new Intent(Intent.ACTION_MAIN, null, context,
				MoneyTracker.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
				defineIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
		views.setOnClickPendingIntent(R.id.thewidget, pendingIntent);

		double remaining = new ExpensesDbHelper(context).remaining();

		NumberFormat fmt = DecimalFormat.getCurrencyInstance();
		views.setTextViewText(R.id.disposable, fmt.format(remaining));

		appWidgetManager.updateAppWidget(new ComponentName(context,
				MoneyTrackerWidgetProvider.class), views);
	}
}
