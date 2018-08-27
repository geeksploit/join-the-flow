package me.geeksploit.jointheflow;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An activity representing a single Flow detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link FlowListActivity}.
 */
public class FlowDetailActivity extends AppCompatActivity {

    private String mFlowId;
    private DatabaseReference mFlowsDatabaseReference;
    private ValueEventListener mTimestampEventListener;

    private boolean mJoined;
    private String mUserId;
    @BindView(R.id.fab) FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flow_detail);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFab.setEnabled(false);
                if (mJoined) {
                    leaveTheFlow(mFlowId, mUserId);
                } else {
                    joinTheFlow(mFlowId, mUserId);
                }
            }
        });

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        mFlowId = getIntent().getStringExtra(FlowDetailFragment.ARG_ITEM_ID);
        mUserId =  FirebaseAuth.getInstance().getUid();

        mFlowsDatabaseReference = FirebaseDatabase.getInstance().getReference()
                .child(getString(R.string.db_node_flows))
                .child(mFlowId)
                .child(getString(R.string.db_node_joined))
                .child(mUserId);

        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(FlowDetailFragment.ARG_ITEM_ID, mFlowId);
            FlowDetailFragment fragment = new FlowDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.flow_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpTo(this, new Intent(this, FlowListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void joinTheFlow(String flowId, String userId) {
        mFlowsDatabaseReference.setValue(System.currentTimeMillis());
    }

    private void leaveTheFlow(String flowId, String userId) {
        mFlowsDatabaseReference.removeValue();
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
                mJoined = (timestamp != null);
                updateFab();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mFlowsDatabaseReference.addValueEventListener(mTimestampEventListener);
    }

    private void updateFab() {
        mFab.setEnabled(true);
        if (mJoined) {
            mFab.setImageResource(R.drawable.ic_star_white_24dp);
            mFab.setContentDescription(getString(R.string.button_leave));
        } else {
            mFab.setImageResource(R.drawable.ic_star_border_white_24dp);
            mFab.setContentDescription(getString(R.string.button_join));
        }
    }

}
