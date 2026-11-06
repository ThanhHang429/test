package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.eventapp.app.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Price label with Vietnamese VND grouping. Kept independent from MoneyUtils so shared screens
 * can adopt it without changing the existing utility skeleton.
 */
public class PriceTextView extends AppCompatTextView {

    public PriceTextView(Context context) {
        this(context, null);
    }

    public PriceTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PriceTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        // setTextAppearance() ghi đè textColor từ XML, nên đọc lại màu tường minh (nếu có)
        // để card tối như hero vẫn giữ được chữ trắng.
        ColorStateList explicitColor = null;
        if (attrs != null) {
            TypedArray typed = getContext().obtainStyledAttributes(attrs,
                    new int[]{android.R.attr.textColor});
            explicitColor = typed.getColorStateList(0);
            typed.recycle();
        }
        setTextAppearance(R.style.TextAppearance_EventApp_Ds_Price);
        if (explicitColor != null) {
            setTextColor(explicitColor);
        }
        setGravity(Gravity.CENTER_VERTICAL);
        setIncludeFontPadding(false);
    }

    public void setPrice(long amount) {
        setText(formatVnd(amount));
        setContentDescription(formatVnd(amount));
    }

    public void setPriceFrom(long minAmount) {
        if (minAmount <= 0L) {
            setFree();
        } else {
            setText(getContext().getString(R.string.hc_event_price_from,
                    formatVnd(minAmount)));
            setContentDescription(getText());
        }
    }

    public void setFree() {
        setText(getContext().getString(R.string.hc_free));
        setContentDescription(getText());
    }

    public static String formatVnd(long amount) {
        return formatPlain(amount) + " đ";
    }

    public static String formatPlain(long amount) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        return format.format(amount);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
