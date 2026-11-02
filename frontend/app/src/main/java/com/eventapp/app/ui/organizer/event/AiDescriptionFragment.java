package com.eventapp.app.ui.organizer.event;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.Navigation;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.AiBadge;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * FR-O06 "Viết mô tả bằng AI". Shows condition chips, up to three variants behind tabs, the
 * post-check warnings and the missing-fields list. The text is handed back to the form through
 * the previous back-stack entry — this screen never saves or publishes anything (O06-R01).
 */
public class AiDescriptionFragment extends Fragment {

    /** Key written into the previous back-stack entry's SavedStateHandle. */
    public static final String RESULT_KEY = "hc_ai_description_text";

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm dd/MM",
            new Locale("vi", "VN"));

    private AiDescriptionViewModel viewModel;
    private ChipGroup toneGroup;
    private ChipGroup lengthGroup;
    private ChipGroup variantGroup;
    private TextView statusView;
    private LinearProgressIndicator progress;
    private TabLayout tabs;
    private View previewCard;
    private AiBadge badge;
    private TextView preview;
    private TextView summary;
    private View warningsCard;
    private LinearLayout warningsList;
    private View missingCard;
    private LinearLayout missingList;
    private View errorCard;
    private TextView errorTitle;
    private TextView errorMessage;
    private MaterialButton retryButton;
    private TextView emptyView;
    private MaterialButton undoButton;
    private MaterialButton regenerateButton;
    private MaterialButton useButton;

    private boolean bindingTabs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_event_ai_description, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AiDescriptionViewModel.class);
        bindViews(view);
        setupToolbar();
        setupConditions();
        setupActions();
        observe();

        Bundle args = getArguments();
        // Nav arg khai báo string[] nên phải đọc bằng getStringArray, không phải ArrayList.
        String[] points = args == null ? null : args.getStringArray("arg_key_points");
        List<String> keyPoints = points == null
                ? new ArrayList<>() : new ArrayList<>(java.util.Arrays.asList(points));
        viewModel.init(
                args == null ? 0L : args.getLong("arg_category_id", 0L),
                args == null ? "" : args.getString("arg_event_title", ""),
                args == null ? "" : args.getString("arg_format", ""),
                args == null ? "" : args.getString("arg_audience", ""),
                keyPoints);
    }

    private void bindViews(View view) {
        toneGroup = view.findViewById(R.id.chipGroupAiTone);
        lengthGroup = view.findViewById(R.id.chipGroupAiLength);
        variantGroup = view.findViewById(R.id.chipGroupAiVariants);
        statusView = view.findViewById(R.id.tvAiDescStatus);
        progress = view.findViewById(R.id.progressAiDesc);
        tabs = view.findViewById(R.id.tabsAiDesc);
        previewCard = view.findViewById(R.id.cardAiDescPreview);
        badge = view.findViewById(R.id.badgeAiDesc);
        preview = view.findViewById(R.id.tvAiDescPreview);
        summary = view.findViewById(R.id.tvAiDescSummary);
        warningsCard = view.findViewById(R.id.cardAiDescWarnings);
        warningsList = view.findViewById(R.id.layoutAiDescWarnings);
        missingCard = view.findViewById(R.id.cardAiDescMissing);
        missingList = view.findViewById(R.id.layoutAiDescMissing);
        errorCard = view.findViewById(R.id.cardAiDescError);
        errorTitle = view.findViewById(R.id.tvAiDescErrorTitle);
        errorMessage = view.findViewById(R.id.tvAiDescErrorMessage);
        retryButton = view.findViewById(R.id.btnAiDescRetry);
        emptyView = view.findViewById(R.id.tvAiDescEmpty);
        undoButton = view.findViewById(R.id.btnAiDescUndo);
        regenerateButton = view.findViewById(R.id.btnAiDescRegenerate);
        useButton = view.findViewById(R.id.btnAiDescUse);
    }

    private void setupToolbar() {
        requireView().findViewById(R.id.btnAiDescBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
    }

    private void setupConditions() {
        toneGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipAiToneFriendly) {
                viewModel.setTone(AiDescriptionUiModel.Tone.FRIENDLY);
            } else if (id == R.id.chipAiToneFormal) {
                viewModel.setTone(AiDescriptionUiModel.Tone.FORMAL);
            } else {
                viewModel.setTone(AiDescriptionUiModel.Tone.PROFESSIONAL);
            }
        });

        lengthGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipAiLengthShort) {
                viewModel.setLength(AiDescriptionUiModel.Length.SHORT);
            } else if (id == R.id.chipAiLengthLong) {
                viewModel.setLength(AiDescriptionUiModel.Length.LONG);
            } else {
                viewModel.setLength(AiDescriptionUiModel.Length.MEDIUM);
            }
        });

        variantGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipAiVariant3) {
                viewModel.setVariantCount(3);
            } else if (id == R.id.chipAiVariant2) {
                viewModel.setVariantCount(2);
            } else {
                viewModel.setVariantCount(1);
            }
        });

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!bindingTabs) {
                    viewModel.selectVariant(tab.getPosition());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupActions() {
        regenerateButton.setOnClickListener(v -> {
            if (viewModel.isRequesting()) {
                viewModel.cancel();
            } else {
                viewModel.generate();
            }
        });
        undoButton.setOnClickListener(v -> viewModel.undo());
        retryButton.setOnClickListener(v -> viewModel.retry());
        useButton.setOnClickListener(v -> sendResultBack());
    }

    private void observe() {
        viewModel.getStatus().observe(getViewLifecycleOwner(), status -> render());
        viewModel.getResult().observe(getViewLifecycleOwner(), result -> render());
        viewModel.getGeneration().observe(getViewLifecycleOwner(), generation -> render());
        viewModel.getSelectedVariant().observe(getViewLifecycleOwner(), index -> renderPreview());
    }

    private void render() {
        AiDescriptionUiModel.Status status = viewModel.getStatus().getValue();
        if (status == null) {
            status = AiDescriptionUiModel.Status.IDLE;
        }
        AiDescriptionUiModel.Result result = viewModel.getResult().getValue();
        boolean requesting = status == AiDescriptionUiModel.Status.REQUESTING;
        boolean hasResult = result != null && !result.variants.isEmpty();

        progress.setVisibility(requesting ? View.VISIBLE : View.GONE);

        switch (status) {
            case REQUESTING:
                statusView.setText(R.string.hc_aidesc_status_requesting);
                break;
            case SUCCESS:
                statusView.setText(getString(R.string.hc_aidesc_status_success,
                        result.variants.size(),
                        timeFormat.format(new Date(result.createdAtMs))));
                statusView.append("\u2022 " + getString(R.string.hc_aidesc_quota_used,
                        viewModel.getQuotaUsed()));
                break;
            case ERROR:
                statusView.setText(R.string.hc_aidesc_error_title);
                break;
            case BLOCKED:
                statusView.setText(R.string.hc_aidesc_blocked_title);
                break;
            case QUOTA:
                statusView.setText(R.string.hc_aidesc_quota_title);
                break;
            case IDLE:
            default:
                statusView.setText(R.string.hc_aidesc_status_idle);
                break;
        }

        boolean showResult = hasResult && !requesting;
        previewCard.setVisibility(showResult ? View.VISIBLE : View.GONE);
        emptyView.setVisibility(hasResult || requesting || status != AiDescriptionUiModel.Status.IDLE
                ? View.GONE : View.VISIBLE);
        undoButton.setEnabled(viewModel.canUndo() && !requesting);
        undoButton.setAlpha(undoButton.isEnabled() ? 1f : 0.5f);

        if (status == AiDescriptionUiModel.Status.REQUESTING) {
            regenerateButton.setText(R.string.hc_aidesc_cancel);
            regenerateButton.setEnabled(true);
        } else if (hasResult) {
            regenerateButton.setText(R.string.hc_aidesc_regenerate);
            regenerateButton.setEnabled(true);
        } else {
            regenerateButton.setText(R.string.hc_aidesc_generate);
            regenerateButton.setEnabled(status != AiDescriptionUiModel.Status.QUOTA);
        }
        regenerateButton.setAlpha(regenerateButton.isEnabled() ? 1f : 0.5f);

        useButton.setEnabled(showResult);
        useButton.setAlpha(showResult ? 1f : 0.5f);

        renderError(status);
        renderWarningsAndMissing(result, showResult);
        renderTabs(result, showResult);
        renderPreview();
    }

    private void renderError(AiDescriptionUiModel.Status status) {
        if (status == AiDescriptionUiModel.Status.ERROR) {
            errorCard.setVisibility(View.VISIBLE);
            errorTitle.setText(R.string.hc_aidesc_error_title);
            errorMessage.setText(R.string.hc_aidesc_error_message);
            retryButton.setVisibility(View.VISIBLE);
        } else if (status == AiDescriptionUiModel.Status.BLOCKED) {
            errorCard.setVisibility(View.VISIBLE);
            errorTitle.setText(R.string.hc_aidesc_blocked_title);
            errorMessage.setText(R.string.hc_aidesc_blocked_message);
            retryButton.setVisibility(View.GONE);
        } else if (status == AiDescriptionUiModel.Status.QUOTA) {
            errorCard.setVisibility(View.VISIBLE);
            errorTitle.setText(R.string.hc_aidesc_quota_title);
            errorMessage.setText(getString(R.string.hc_aidesc_quota_message,
                    viewModel.getQuotaWaitMinutes()));
            retryButton.setVisibility(View.GONE);
        } else {
            errorCard.setVisibility(View.GONE);
        }
    }

    private void renderWarningsAndMissing(@Nullable AiDescriptionUiModel.Result result,
                                          boolean visible) {
        List<AiDescriptionUiModel.Warning> warnings =
                result == null ? new ArrayList<>() : result.warnings;
        List<String> missing = result == null ? new ArrayList<>() : result.missingInfo;

        warningsList.removeAllViews();
        for (AiDescriptionUiModel.Warning warning : warnings) {
            addLine(warningsList, getString(R.string.hc_aidesc_warning_item,
                    warningText(warning)), R.style.TextAppearance_EventApp_Ds_Caption,
                    R.color.ds_danger_dark);
        }
        warningsCard.setVisibility(visible && !warnings.isEmpty() ? View.VISIBLE : View.GONE);

        missingList.removeAllViews();
        for (String item : missing) {
            addLine(missingList, getString(R.string.hc_aidesc_missing_item, item),
                    R.style.TextAppearance_EventApp_Ds_Caption, R.color.ds_warning_dark);
        }
        missingCard.setVisibility(visible && !missing.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String warningText(AiDescriptionUiModel.Warning warning) {
        switch (warning.kind) {
            case LINK:
                return getString(R.string.hc_aidesc_warning_link, warning.detail);
            case DATE:
                return getString(R.string.hc_aidesc_warning_date, warning.detail);
            case PHONE:
                return getString(R.string.hc_aidesc_warning_phone, warning.detail);
            case NUMBER:
            default:
                return getString(R.string.hc_aidesc_warning_number, warning.detail);
        }
    }

    private void renderTabs(@Nullable AiDescriptionUiModel.Result result, boolean visible) {
        int count = result == null || !visible ? 0 : result.variants.size();
        if (tabs.getTabCount() != count) {
            bindingTabs = true;
            tabs.removeAllTabs();
            for (int i = 0; i < count; i++) {
                tabs.addTab(tabs.newTab().setText(getString(R.string.hc_aidesc_tab, i + 1)), false);
            }
            bindingTabs = false;
            if (count > 0) {
                tabs.getTabAt(viewModel.getSelectedVariant().getValue() == null
                        ? 0 : viewModel.getSelectedVariant().getValue()).select();
            }
        }
        tabs.setVisibility(count > 1 ? View.VISIBLE : View.GONE);
    }

    private void renderPreview() {
        AiDescriptionUiModel.Result result = viewModel.getResult().getValue();
        if (result == null || result.variants.isEmpty()
                || viewModel.getStatus().getValue() == AiDescriptionUiModel.Status.REQUESTING) {
            return;
        }
        AiDescriptionUiModel.Variant variant =
                result.variantAt(viewModel.getSelectedVariant().getValue() == null
                        ? 0 : viewModel.getSelectedVariant().getValue());
        if (variant == null) {
            return;
        }
        preview.setText(highlightPlaceholders(variant.text));
        summary.setText(getString(R.string.hc_aidesc_summary, variant.shortSummary));
        badge.setReason(getString(R.string.hc_aidesc_generated_at,
                timeFormat.format(new Date(result.createdAtMs))));
    }

    /** Highlights every "[Cần bổ sung: …]" span so missing fields stand out (O06-R02). */
    private CharSequence highlightPlaceholders(String text) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        int start = text.indexOf("[Cần bổ sung");
        int color = ContextCompat.getColor(requireContext(), R.color.ds_warning_soft);
        int textColor = ContextCompat.getColor(requireContext(), R.color.ds_warning_dark);
        while (start >= 0) {
            int end = text.indexOf(']', start);
            if (end < 0) {
                break;
            }
            builder.setSpan(new BackgroundColorSpan(color), start, end + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new ForegroundColorSpan(textColor), start, end + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new StyleSpan(Typeface.BOLD), start, end + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = text.indexOf("[Cần bổ sung", end + 1);
        }
        return builder;
    }

    private void sendResultBack() {
        AiDescriptionUiModel.Result result = viewModel.getResult().getValue();
        if (result == null || result.variants.isEmpty()) {
            return;
        }
        AiDescriptionUiModel.Variant variant =
                result.variantAt(viewModel.getSelectedVariant().getValue() == null
                        ? 0 : viewModel.getSelectedVariant().getValue());
        if (variant == null) {
            return;
        }
        NavBackStackEntry previous =
                Navigation.findNavController(requireView()).getPreviousBackStackEntry();
        if (previous != null) {
            previous.getSavedStateHandle().set(RESULT_KEY, variant.text);
        }
        Navigation.findNavController(requireView()).popBackStack();
    }

    private void addLine(LinearLayout parent, CharSequence text, int styleRes, int colorRes) {
        TextView line = new TextView(requireContext());
        line.setText(text);
        line.setTextAppearance(styleRes);
        line.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(6);
        line.setLayoutParams(params);
        parent.addView(line);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}