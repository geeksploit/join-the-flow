package me.geeksploit.jointheflow;

import android.app.Activity;
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
    private TextView mHeaderTextView;
    private TextView mTimerTextView;

    private DatabaseReference mFlowsDatabaseReference;

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

        mHeaderTextView = rootView.findViewById(R.id.flow_detail_header);
        mTimerTextView = rootView.findViewById(R.id.flow_detail_timer);

        return rootView;
    }
}
