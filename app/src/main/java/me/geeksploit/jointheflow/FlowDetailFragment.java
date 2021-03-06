package me.geeksploit.jointheflow;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * A fragment representing a single Flow detail screen.
 * This fragment is either contained in a {@link FlowListActivity}
 * in two-pane mode (on tablets) or a {@link FlowDetailActivity}
 * on handsets.
 */
public class FlowDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    private String mFlowTitle;
    private String mUserTitle;
    @BindView(R.id.flow_detail_header) TextView mHeaderTextView;
    @BindView(R.id.flow_detail_timer) TextView mTimerTextView;
    private long mStartTime;

    private DatabaseReference mFlowsDatabaseReference;
    private ValueEventListener mTimestampEventListener;
    private TimerUpdateTask mAsyncTask;

    private Unbinder mUnbinder;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FlowDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            String flowId = getArguments().getString(ARG_ITEM_ID);
            String userId = FirebaseAuth.getInstance().getUid();

            mUserTitle = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();

            FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
            mFlowsDatabaseReference = firebaseDatabase.getReference().child(getString(R.string.db_node_flows))
                    .child(flowId).child(getString(R.string.db_node_joined)).child(userId);

            firebaseDatabase.getReference().child(getString(R.string.db_node_flows)).child(flowId)
                    .child(getString(R.string.db_node_title))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    mFlowTitle = dataSnapshot.getValue(String.class);

                    Activity activity = FlowDetailFragment.this.getActivity();
                    CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
                    if (appBarLayout != null) {
                        appBarLayout.setTitle(mFlowTitle);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.flow_detail, container, false);
        mUnbinder = ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        attachDatabaseListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        detachDatabaseListener();
        flowLeaveCleanup();
    }

    private void flowLeaveCleanup() {
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
            mAsyncTask = null;
        }
        mTimerTextView.setText("");
        mHeaderTextView.setText(getString(R.string.flow_detail_intro, mUserTitle, mFlowTitle));
    }

    private void detachDatabaseListener() {
        if (mTimestampEventListener == null) return;
        mFlowsDatabaseReference.removeEventListener(mTimestampEventListener);
        mTimestampEventListener = null;
    }

    private void attachDatabaseListener() {
        if (mTimestampEventListener != null) return;

        mTimestampEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Long timestamp = dataSnapshot.getValue(Long.class);
                if (timestamp == null) {
                    flowLeaveCleanup();
                } else {
                    mStartTime = timestamp;
                    if (mAsyncTask != null) {
                        mAsyncTask.cancel(true);
                    }
                    mAsyncTask = new TimerUpdateTask();
                    mAsyncTask.execute(mStartTime);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mFlowsDatabaseReference.addValueEventListener(mTimestampEventListener);
    }

    private void updateViews ()
    {
        long interval = System.currentTimeMillis() - mStartTime;

        final long hr = TimeUnit.MILLISECONDS.toHours(interval);
        final long min = TimeUnit.MILLISECONDS.toMinutes(interval - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(interval - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));

        int headerFormat;
        if (hr > 8)
            headerFormat = R.string.flow_detail_duration_too_long;
        else if (hr > 0)
            headerFormat = R.string.flow_detail_duration_long;
        else if (min > 0)
            headerFormat = R.string.flow_detail_duration_medium;
        else
            headerFormat = R.string.flow_detail_duration_short;

        String header = getString(headerFormat, mUserTitle, mFlowTitle);
        String timer = getString(R.string.flow_detail_timer, hr, min, sec);

        mHeaderTextView.setText(header);
        mTimerTextView.setText(timer);
    }

    private class TimerUpdateTask extends AsyncTask<Long, String, Void> {

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            updateViews();
        }

        @Override
        protected Void doInBackground(Long... longs) {
            while (true) {
                publishProgress();
                if (isCancelled()) break;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            }
            return null;
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }
}
