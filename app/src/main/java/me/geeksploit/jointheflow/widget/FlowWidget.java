package me.geeksploit.jointheflow.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import me.geeksploit.jointheflow.FlowListActivity;
import me.geeksploit.jointheflow.R;
import me.geeksploit.jointheflow.data.Flow;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link FlowWidgetConfigureActivity FlowWidgetConfigureActivity}
 */
public class FlowWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, Flow flow) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.flow_widget);
        views.setTextViewText(R.id.appwidget_count, String.valueOf(flow.getJoinedCount()));
        views.setTextViewText(R.id.appwidget_text, flow.getTitle());

        // Construct click intent
        Intent launchApp = new Intent(context, FlowListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchApp, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.appwidget_root, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager,
                                final int appWidgetId) {

        final String flowKey = FlowWidgetConfigureActivity.loadTitlePref(context, appWidgetId);
        FirebaseDatabase.getInstance().getReference().child("flows").child(flowKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Flow flow = dataSnapshot.getValue(Flow.class);
                        if (flow == null) return;
                        flow.setKey(dataSnapshot.getKey());
                        updateAppWidget(context, appWidgetManager, appWidgetId, flow);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            FlowWidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

