package eaglechat.eaglechat;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;


public class FakeMessageFragment extends DialogFragment {
    private static final String CONTACT_ID = "contact_id";

    private long mContactId;
    private EditText mTextMessage;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param contactId The contact to use as sender of messages.
     * @return A new instance of fragment FakeMessageFragment.
     */
    public static FakeMessageFragment newInstance(long contactId) {
        FakeMessageFragment fragment = new FakeMessageFragment();
        Bundle args = new Bundle();
        args.putLong(CONTACT_ID, contactId);
        fragment.setArguments(args);
        return fragment;
    }

    public FakeMessageFragment() {
        // Required empty public constructor
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        return builder
                .setTitle("Enter message from contact")

                .create();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mContactId = getArguments().getLong(CONTACT_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_fake_message, container, false);
        v.findViewById(R.id.button_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        mTextMessage = (EditText) v.findViewById(R.id.text_message);
        AlertDialog dialog = (AlertDialog) getDialog();
        dialog.setView(v);
        return null;
    }

    private void sendMessage() {
        if (mTextMessage.getText().length() > 0) {
            String message = mTextMessage.getText().toString();

            ContentValues values = new ContentValues();
            values.put(MessagesTable.COLUMN_RECEIVER, 0);
            values.put(MessagesTable.COLUMN_SENDER, mContactId);
            values.put(MessagesTable.COLUMN_CONTENT, message);

            getActivity().getContentResolver().insert(DatabaseProvider.MESSAGES_URI, values);
            mTextMessage.setText("");

            dismiss();
        }
    }


}
