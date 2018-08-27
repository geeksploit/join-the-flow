package me.geeksploit.jointheflow;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;

public class FlowSuggestDialogFragment extends DialogFragment {

    public interface FlowSuggestDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
    }

    FlowSuggestDialogListener mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (FlowSuggestDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(getString(R.string.error_must_implement,
                    context.toString(),
                    FlowSuggestDialogListener.class.getSimpleName()));
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.dialog_flow_suggest, null))
                .setTitle(R.string.dialog_title)
                .setPositiveButton(R.string.dialog_suggest, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onDialogPositiveClick(FlowSuggestDialogFragment.this);
                    }
                });

        return builder.create();
    }
}
