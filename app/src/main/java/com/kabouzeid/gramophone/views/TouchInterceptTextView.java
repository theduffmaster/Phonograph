package com.kabouzeid.gramophone.views;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.AttributeSet;

/**
 * @author Lincoln (theduffmaster)
 *
 * TextView that automatically does exactly what android:ellipsize="end" does, except this works in a TouchInterceptHorizontalScrollViews.
 * Truncates the string so it doesn't get cuttoff in the TouchInterceptHorizontalScrollView
 * and puts an ellipsis at the end of it.
 * Must be used within a TouchInterceptHorizontalScrollview or it won't work
 */
public class TouchInterceptTextView extends AppCompatTextView {

    public static final String TAG = TouchInterceptTextView.class.getSimpleName();

    private static final int RETRUNCATE_DELAY = 600;

    // Invisible character used as a marker indicating whether a string has undergone truncation
    private static final String TRUNCATED_MARKER = "\u202F";

    // Invisible character used as a marker indicating whether a string is untruncated
    private static final String MARKER_UNTRUNCATED = "\uFEFF";

    private String text;
    private String truncatedText;

    public TouchInterceptTextView(Context context) {
        super(context);

        init();
    }

    public TouchInterceptTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public TouchInterceptTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        setTag(TouchInterceptTextView.TAG);

        // Enable long clicking when touching the text
        setLongClickable(true);

        // Blocks clicks from passing through this view
        setClickable(true);

        // Use this instead of maxlines
        setSingleLine();
    }

    /**
     * @return Returns the {@link TouchInterceptFrameLayout} inside this layout.
     */
    public TouchInterceptFrameLayout getTouchInterceptFrameLayout() {
        return (TouchInterceptFrameLayout) getRootView().findViewWithTag(TouchInterceptFrameLayout.TAG);
    }

    /**
     * @return Returns the parent {@link TouchInterceptHorizontalScrollView}.
     */
    public TouchInterceptHorizontalScrollView getTouchInterceptHorizontalScrollView() {
        return (TouchInterceptHorizontalScrollView) getParent();
    }

    /**
     * The text undergoes truncation here. {@link #onMeasure} is immediately called after
     * {@link #setText} and has a reference to the parent's bounds. The bounds are used for setting
     * the length of the truncated text, ensuring that the text does not get visibly cut off.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        String fittedText = getText().toString();

        final int textBoundsWidth = MeasureSpec.getSize(widthMeasureSpec);
        final boolean isUntruncated = fittedText.endsWith(MARKER_UNTRUNCATED);

        if (!fittedText.endsWith(TRUNCATED_MARKER) && !isUntruncated) {
            this.text = fittedText;
        }

        if (!isUntruncated && (getWidth() == 0 | textBoundsWidth < getPaint().measureText(fittedText))) {
            // Mimics behavior of `android:ellipsize="end"`, except it works in a HorizontalScrollView.
            // Truncates the string so it doesn't get cut off in the HorizontalScrollView with an
            // ellipsis at the end of it.
            fittedText = TextUtils.ellipsize(fittedText,
                    getPaint(),
                    (float) textBoundsWidth,
                    TextUtils.TruncateAt.END).toString()
                    + TRUNCATED_MARKER;
        }

        setText(fittedText);
        initiateTruncateText(text, fittedText);
    }

    /**
     * Takes the string that's undergone truncation and based on whether it's been truncated or not
     * set whether it should be scrollable or not and what to do when the user finishes scrolling.
     *
     * @param originalText  The string before truncation
     * @param truncatedText The string after truncation
     */
    public void initiateTruncateText(final String originalText, final String truncatedText) {
        post(new Runnable() {
            @Override
            public void run() {
                if (!originalText.endsWith(TRUNCATED_MARKER)) text = originalText;
                TouchInterceptTextView.this.truncatedText = truncatedText;

                final TouchInterceptHorizontalScrollView scrollView = getTouchInterceptHorizontalScrollView();

                if (isTextTruncated(truncatedText)) {
                    if (originalText.equals(truncatedText) && !truncatedText.endsWith(MARKER_UNTRUNCATED)) {
                        scrollView.setScrollable(false);
                    } else {
                        scrollView.setScrollable(true);

                        scrollView.setOnEndScrollListener(new TouchInterceptHorizontalScrollView.OnEndScrollListener() {
                            @Override
                            public void onEndScroll() {
                                reTruncateScrollText(truncatedText, scrollView, TouchInterceptTextView.this);
                            }
                        });
                    }
                } else if (!truncatedText.endsWith(MARKER_UNTRUNCATED)) {
                    scrollView.setScrollable(false);
                }
            }
        });
    }

    /**
     * Checks whether a string was truncated at some point.
     *
     * @param text The string to check.
     * @return Returns whether the text has been truncated or not.
     */
    public boolean isTextTruncated(String text) {
        return text.endsWith("…" + TRUNCATED_MARKER);
    }

    /**
     * Untruncates and sets the text.
     */
    public void unTruncateText() {
        String untrunucatedText = text + MARKER_UNTRUNCATED;
        setText(untrunucatedText);
    }

    /**
     * @return Returns the truncated text.
     */
    public String getTruncatedText() {
        return this.truncatedText;
    }

    /**
     * @return Returns the untruncated text.
     */
    public String getUntruncatedText() {
        return this.text;
    }

    /**
     * Retruncates the text and animates it scrolling back to the start poosition.
     */
    public void reTruncateScrollText(final String truncatedString,
                                     final TouchInterceptHorizontalScrollView scrollView,
                                     final TouchInterceptTextView textView) {
        ObjectAnimator.ofInt(scrollView, "scrollX", 0)
                .setDuration(RETRUNCATE_DELAY)
                .start();

        scrollView.slidingPanelSetTouchEnabled(true);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                textView.setText(truncatedString);
            }
        }, RETRUNCATE_DELAY);
    }
}
