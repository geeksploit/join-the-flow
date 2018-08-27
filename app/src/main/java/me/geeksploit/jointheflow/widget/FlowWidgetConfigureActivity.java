package me.geeksploit.jointheflow.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import me.geeksploit.jointheflow.R;
import me.geeksploit.jointheflow.data.Flow;

/**
 * The configuration screen for the {@link FlowWidget FlowWidget} AppWidget.
 */
public class FlowWidgetConfigureActivity extends Activity {

    private static final String PREFS_NAME = "me.geeksploit.jointheflow.widget.FlowWidget";
    private static final String PREF_PREFIX_KEY = "appwidget_";
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private SimpleItemRecyclerViewAdapter mFlowsAdapter;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mFlowsDatabaseReference;
    private ChildEventListener mFlowsChildEventListener;

    public FlowWidgetConfigureActivity() {
        super();
    }

    // Write the prefix to the SharedPreferences object for this widget
    static void saveTitlePref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, text);
        prefs.apply();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static String loadTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String titleValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        if (titleValue != null) {
            return titleValue;
        } else {
            return context.getString(R.string.appwidget_text);
        }
    }

    static void deleteTitlePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.apply();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.flow_widget_configure);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFlowsDatabaseReference = mFirebaseDatabase.getReference().child(getString(R.string.db_node_flows));

        View recyclerView = findViewById(R.id.flow_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);
    }


    private void detachDatabaseReadListener() {
        if (mFlowsChildEventListener == null) return;

        mFlowsDatabaseReference.removeEventListener(mFlowsChildEventListener);
        mFlowsChildEventListener = null;
    }

    private void attachDatabaseReadListener() {
        if (mFlowsChildEventListener != null) return;

        mFlowsChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Flow flow = dataSnapshot.getValue(Flow.class);
                flow.setKey(dataSnapshot.getKey());
                mFlowsAdapter.add(flow);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Flow flow = dataSnapshot.getValue(Flow.class);
                flow.setKey(dataSnapshot.getKey());
                mFlowsAdapter.update(flow);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };

        mFlowsDatabaseReference.orderByChild(getString(R.string.db_node_title)).addChildEventListener(mFlowsChildEventListener);
    }


    @Override
    protected void onResume() {
        super.onResume();
        attachDatabaseReadListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        detachDatabaseReadListener();
        mFlowsAdapter.clear();
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        mFlowsAdapter = new SimpleItemRecyclerViewAdapter(this, new ArrayList<Flow>(), mAppWidgetId);
        recyclerView.setAdapter(mFlowsAdapter);
    }

    public static class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<FlowWidgetConfigureActivity.SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final FlowWidgetConfigureActivity mParentActivity;
        private final List<Flow> mValues;
        private final int mAppWidgetId;
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Context context = mParentActivity;

                Flow flow = (Flow) view.getTag();

                // When the button is clicked, store the string locally
                String widgetText = flow.getKey();
                saveTitlePref(context, mAppWidgetId, widgetText);

                // It is the responsibility of the configuration activity to update the app widget
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                FlowWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

                // Make sure we pass back the original appWidgetId
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                mParentActivity.setResult(RESULT_OK, resultValue);
                mParentActivity.finish();
            }
        };

        SimpleItemRecyclerViewAdapter(FlowWidgetConfigureActivity parent,
                                      List<Flow> items,
                                      int widgetId) {
            mValues = items;
            mParentActivity = parent;
            mAppWidgetId = widgetId;
        }

        @Override
        public FlowWidgetConfigureActivity.SimpleItemRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.flow_widget_configure_list_content, parent, false);
            return new FlowWidgetConfigureActivity.SimpleItemRecyclerViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final FlowWidgetConfigureActivity.SimpleItemRecyclerViewAdapter.ViewHolder holder, int position) {
            Flow flow = mValues.get(position);
            holder.mContentView.setText(flow.getTitle());

            holder.itemView.setTag(flow);
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public void add(Flow flow) {
            mValues.add(flow);
            notifyDataSetChanged();
        }

        public void update(Flow flow) {
            mValues.set(mValues.indexOf(flow), flow);
            notifyDataSetChanged();
        }

        public void clear() {
            mValues.clear();
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mContentView;

            ViewHolder(View view) {
                super(view);
                mContentView = view.findViewById(R.id.content);
            }
        }
    }
}
