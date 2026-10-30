package com.eventapp.app.ui.admin.event.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.eventapp.app.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Reason + note dialog for FR-A07 dismiss and FR-A14 hide/keep/request-edit. The selected reason
 * code is only required when the caller asks for it (A14-R01/A14-R04).
 */
public class ModerationReasonDialog extends DialogFragment {

    public static final String REQUEST_KEY = "moderation_reason_request";
    public static final String RESULT_ACTION = "action";
    public static final String RESULT_ITEM_ID = "item_id";
    public static final String RESULT_CODE = "code";
    public static final String RESULT_NOTE = "note";

    public static final String ACTION_DISMISS_FLAG = "dismiss_flag";
    public static final String ACTION_HIDE_REVIEW = "hide_review";
    public static final String ACTION_KEEP_REVIEW = "keep_review";
    public static final String ACTION_REQUEST_EDIT_REVIEW = "request_edit_review";

    private static final String ARG_ACTION = "arg_action";
    private static final String ARG_ITEM_ID = "arg_item_id";
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_MESSAGE = "arg_message";
    private static final String ARG_CONFIRM = "arg_confirm";
    private static final String ARG_REQUIRE_CODE = "arg_require_code";

    private ChipGroup codeGroup;
    private View codeBlock;
    private TextInputLayout noteLayout;
    private TextInputEditText noteInput;
    private TextView errorView;

    public static ModerationReasonDialog newInstance(String action, long itemId,
                                                     @StringRes int titleRes,
                                                     @StringRes int messageRes,
                                                     @StringRes int confirmRes,
                                                     boolean requireCode) {
        ModerationReasonDialog dialog = new ModerationReasonDialog();
        Bundle args = new Bundle();
        args.putString(ARG_ACTION, action);
        args.putLong(ARG_ITEM_ID, itemId);
        args.putInt(ARG_TITLE, titleRes);
        args.putInt(ARG_MESSAGE, messageRes);
        args.putInt(ARG_CONFIRM, confirmRes);
        args.putBoolean(ARG_REQUIRE_CODE, requireCode);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_moderation_reason, null);
        codeGroup = view.findViewById(R.id.chipGroupReasonCodes);
        codeBlock = view.findViewById(R.id.layoutReasonCode);
        noteLayout = view.findViewById(R.id.layoutReasonNote);
        noteInput = view.findViewById(R.id.etReasonNote);
        errorView = view.findViewById(R.id.tvReasonError);

        boolean requireCode = args.getBoolean(ARG_REQUIRE_CODE);
        codeBlock.setVisibility(requireCode ? View.VISIBLE : View.GONE);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(args.getInt(ARG_TITLE))
                .setMessage(args.getInt(ARG_MESSAGE))
                .setView(view)
                .setNegativeButton(R.string.hc_mod_cancel, null)
                .setPositiveButton(args.getInt(ARG_CONFIRM), null)
                .create();
        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> submit()));
        return dialog;
    }

    private void submit() {
        errorView.setVisibility(View.GONE);
        noteLayout.setError(null);

        String code = null;
        if (codeBlock.getVisibility() == View.VISIBLE) {
            int checkedId = codeGroup.getCheckedChipId();
            if (checkedId == View.NO_ID) {
                showError(R.string.hc_mod_code_required);
                return;
            }
            Chip chip = codeGroup.findViewById(checkedId);
            if (chip == null || chip.getTag() == null) {
                showError(R.string.hc_mod_code_required);
                return;
            }
            code = chip.getTag().toString();
        }

        String note = noteInput.getText() == null ? "" : noteInput.getText().toString().trim();
        if (note.length() < 3) {
            noteLayout.setError(getString(R.string.hc_mod_note_required));
            noteInput.requestFocus();
            return;
        }

        Bundle args = requireArguments();
        Bundle result = new Bundle();
        result.putString(RESULT_ACTION, args.getString(ARG_ACTION));
        result.putLong(RESULT_ITEM_ID, args.getLong(ARG_ITEM_ID));
        result.putString(RESULT_CODE, code);
        result.putString(RESULT_NOTE, note);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

    private void showError(@StringRes int messageRes) {
        errorView.setText(messageRes);
        errorView.setVisibility(View.VISIBLE);
    }
}
