package com.eventapp.app.ui.admin.category;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.eventapp.app.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Editor for FR-A09 guidance content (FAQ used by the policy assistant). */
public class FaqEditorDialog extends DialogFragment {

    public static final String REQUEST_KEY = "faq_editor_request";
    public static final String RESULT_ID = "id";
    public static final String RESULT_TITLE = "title";
    public static final String RESULT_CONTENT = "content";
    public static final String RESULT_VISIBLE = "visible";

    private static final String ARG_ID = "arg_id";
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_CONTENT = "arg_content";
    private static final String ARG_VISIBLE = "arg_visible";

    private TextInputLayout titleLayout;
    private TextInputLayout contentLayout;
    private TextInputEditText titleInput;
    private TextInputEditText contentInput;
    private MaterialSwitch visibleSwitch;

    public static FaqEditorDialog newInstance(long id, String title, String content,
                                             boolean visible) {
        FaqEditorDialog dialog = new FaqEditorDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, id);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_CONTENT, content);
        args.putBoolean(ARG_VISIBLE, visible);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        View view = getLayoutInflater()
                .inflate(R.layout.dialog_faq_editor, null);

        titleLayout = view.findViewById(R.id.layoutFaqTitle);
        contentLayout = view.findViewById(R.id.layoutFaqContent);
        titleInput = view.findViewById(R.id.etFaqTitle);
        contentInput = view.findViewById(R.id.etFaqContent);
        visibleSwitch = view.findViewById(R.id.switchFaqVisible);
        TextView errorView = view.findViewById(R.id.tvFaqFormError);

        titleInput.setText(args.getString(ARG_TITLE, ""));
        contentInput.setText(args.getString(ARG_CONTENT, ""));
        visibleSwitch.setChecked(args.getBoolean(ARG_VISIBLE, true));

        boolean editing = args.getLong(ARG_ID, 0L) != 0L;
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(editing ? R.string.hc_cat_faq_edit : R.string.hc_cat_faq_add)
                .setView(view)
                .setNegativeButton(R.string.hc_cat_cancel, null)
                .setPositiveButton(R.string.hc_cat_save, null)
                .create();
        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    titleLayout.setError(null);
                    contentLayout.setError(null);
                    errorView.setVisibility(View.GONE);

                    String title = text(titleInput);
                    String content = text(contentInput);
                    if (title.length() < 5 || title.length() > 100) {
                        titleLayout.setError(getString(R.string.hc_cat_faq_error));
                        return;
                    }
                    if (content.length() < 10 || content.length() > 2000) {
                        contentLayout.setError(getString(R.string.hc_cat_faq_error));
                        return;
                    }

                    Bundle result = new Bundle();
                    result.putLong(RESULT_ID, args.getLong(ARG_ID, 0L));
                    result.putString(RESULT_TITLE, title);
                    result.putString(RESULT_CONTENT, content);
                    result.putBoolean(RESULT_VISIBLE, visibleSwitch.isChecked());
                    getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                    dismiss();
                }));
        return dialog;
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
