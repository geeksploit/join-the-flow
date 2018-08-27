package me.geeksploit.jointheflow;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.geeksploit.jointheflow.data.Flow;

/**
 * An activity representing a list of Flows. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link FlowDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class FlowListActivity extends AppCompatActivity {

    public static final int RC_SIGN_IN = 1;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mFlowsDatabaseReference;
    private ChildEventListener mFlowsChildEventListener;
    private SimpleItemRecyclerViewAdapter mFlowsAdapter;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseUser mUser;

    private AppCompatDialog mSuggestNewFlowDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flow_list);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFlowsDatabaseReference = mFirebaseDatabase.getReference().child("flows");

        mFirebaseAuth = FirebaseAuth.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    onSignedOutCleanup();
                } else {
                    onSignedInInitialise(user);
                }
            }
        };

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSuggestNewFlowDialog.show();
            }
        });

        if (findViewById(R.id.flow_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        View recyclerView = findViewById(R.id.flow_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);

        setupSuggestNewFlowDialog();
    }

    private void onSignedOutCleanup() {
        mUser = null;
        mFlowsAdapter.clear();
        detachDatabaseReadListener();
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false, true)
                        .setAvailableProviders(Arrays.asList(
                                new AuthUI.IdpConfig.EmailBuilder().build()))
                        .build(),
                RC_SIGN_IN);
    }

    private void onSignedInInitialise(FirebaseUser user) {
        if (mUser == null) mUser = user;
        attachDatabaseReadListener();
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
                flow.setIsJoined(dataSnapshot.child("joined").hasChild(mUser.getUid()));
                mFlowsAdapter.add(flow);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Flow flow = dataSnapshot.getValue(Flow.class);
                flow.setKey(dataSnapshot.getKey());
                flow.setIsJoined(dataSnapshot.child("joined").hasChild(mUser.getUid()));
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

        mFlowsDatabaseReference.addChildEventListener(mFlowsChildEventListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sign_out:
                FirebaseAuth.getInstance().signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void joinTheFlow(Flow flow) {
        mFlowsDatabaseReference.child(flow.getKey()).child("joined").child(mUser.getUid()).setValue(System.currentTimeMillis());
    }

    private void leaveTheFlow(Flow flow) {
        mFlowsDatabaseReference.child(flow.getKey()).child("joined").child(mUser.getUid()).removeValue();
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        mFlowsAdapter = new SimpleItemRecyclerViewAdapter(this, new ArrayList<Flow>(), mTwoPane);
        recyclerView.setAdapter(mFlowsAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != RC_SIGN_IN) return;

        switch (resultCode) {
            case RESULT_OK:
                mUser = mFirebaseAuth.getCurrentUser();
                Toast.makeText(this, getString(R.string.message_welcome, mUser.getDisplayName()), Toast.LENGTH_SHORT)
                        .show();
                break;
            case RESULT_CANCELED:
                Toast.makeText(this, R.string.message_signin_cancelled, Toast.LENGTH_SHORT)
                        .show();
                finish();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        detachDatabaseReadListener();
        mFlowsAdapter.clear();
    }

    public static class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final FlowListActivity mParentActivity;
        private final List<Flow> mValues;
        private final boolean mTwoPane;
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Flow item = (Flow) view.getTag();
                if (mTwoPane) {
                    Bundle arguments = new Bundle();
                    arguments.putString(FlowDetailFragment.ARG_ITEM_ID, item.getKey());
                    FlowDetailFragment fragment = new FlowDetailFragment();
                    fragment.setArguments(arguments);
                    mParentActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.flow_detail_container, fragment)
                            .commit();
                } else {
                    Context context = view.getContext();
                    Intent intent = new Intent(context, FlowDetailActivity.class);
                    intent.putExtra(FlowDetailFragment.ARG_ITEM_ID, item.getKey());

                    context.startActivity(intent);
                }
            }
        };

        private final View.OnClickListener mOnClickListenerJoin = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mParentActivity.joinTheFlow((Flow) view.getTag());
            }
        };

        private final View.OnClickListener mOnClickListenerLeave = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mParentActivity.leaveTheFlow((Flow) view.getTag());
            }
        };

        SimpleItemRecyclerViewAdapter(FlowListActivity parent,
                                      List<Flow> items,
                                      boolean twoPane) {
            mValues = items;
            mParentActivity = parent;
            mTwoPane = twoPane;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.flow_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            Flow flow = mValues.get(position);
            holder.mIdView.setText(String.valueOf(flow.getJoinedCount()));
            holder.mContentView.setText(flow.getTitle());

            holder.itemView.setTag(flow);
            holder.itemView.setOnClickListener(mOnClickListener);

            if (flow.getIsJoined()) {
                holder.mJoin.setVisibility(View.GONE);

                holder.mLeave.setTag(flow);
                holder.mLeave.setVisibility(View.VISIBLE);
                holder.mLeave.setOnClickListener(mOnClickListenerLeave);

                holder.itemView.setBackgroundColor(mParentActivity.getResources().getColor(android.R.color.holo_green_light));
            } else {
                holder.mLeave.setVisibility(View.GONE);

                holder.mJoin.setTag(flow);
                holder.mJoin.setVisibility(View.VISIBLE);
                holder.mJoin.setOnClickListener(mOnClickListenerJoin);

                holder.itemView.setBackgroundColor(mParentActivity.getResources().getColor(android.R.color.white));
            }

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
            final TextView mIdView;
            final TextView mContentView;
            final Button mJoin;
            final Button mLeave;

            ViewHolder(View view) {
                super(view);
                mIdView = view.findViewById(R.id.id_text);
                mContentView = view.findViewById(R.id.content);
                mJoin = view.findViewById(R.id.join);
                mLeave = view.findViewById(R.id.leave);
            }
        }
    }

    private void setupSuggestNewFlowDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        final LayoutInflater inflater = getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_flow_suggest, null))
                .setTitle(R.string.dialog_title)
                // Add action buttons
                .setPositiveButton(R.string.dialog_suggest, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText newFlowTitle = mSuggestNewFlowDialog.findViewById(R.id.dialog_new_flow_title);
                        if (newFlowTitle.getText().length() > 0) {
                            mFlowsDatabaseReference.push().child(getString(R.string.db_node_title))
                                    .setValue(newFlowTitle.getText().toString());
                        }
                    }
                });
        mSuggestNewFlowDialog = builder.create();
    }
}
