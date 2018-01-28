package com.example.snaptoblockdemo;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Adds start/end decorations for horizontal layouts and top/bottom decorations for vertical
 * layouts. The layout manager for the RecyclerView must be a LinearLayout manager or descendent.
 * <p>
 * Within this class, "start" refers to the start of a view for horizontal orientations
 * (left for LTR, right for RTL) and the top of the view for vertical orientation. "End"
 * is either the end of the view for horizontal orientations (right for LTR, left for RTL)
 * or the bottom of the view for vertical orientations.
 */
public class ItemDecorations extends RecyclerView.ItemDecoration {
    // How wide, in pixels, the decoration is.
    private final int mWidth;

    // True if this is a decoration at the start of the view (or top.)
    private final boolean mIsStartDecoration;

    // True of the decoration will be applied to the first/last item in the RecyclerView.
    private final boolean mIsAppliedToFirstLast;

    private final Rect mRect = new Rect();
    private final Paint mPaint = new Paint();

    /**
     * Creates an ItemDecoration.
     *
     * @param color                The color for the decoration
     * @param isStartDecoration    true if the decoration is to be applied to the start of the
     *                             view for horizontal orientations or to the top of the view
     *                             for vertical orientations. false if the decoration is to be
     *                             applied to the end of the view (horizontal orientation) or to
     *                             the bottom of the view (vertical orientation.)
     * @param isAppliedToFirstLast true of the first item in the RecyclerView should have a start
     *                             decoration applied and the last view should have an end
     *                             decoration applied.
     */
    public ItemDecorations(int color, int width, boolean isStartDecoration, boolean isAppliedToFirstLast) {
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mWidth = width;
        mPaint.setColor(color);
        mIsStartDecoration = isStartDecoration;
        mIsAppliedToFirstLast = isAppliedToFirstLast;
    }

    /**
     * Draws horizontal or vertical decorations onto the parent RecyclerView.
     *
     * @param canvas The Canvas onto which decorations will be drawn
     * @param parent The RecyclerView containing the views to be decorated.
     * @param state  The current RecyclerView.State of the RecyclerView
     */
    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        final LinearLayoutManager llm = (LinearLayoutManager) parent.getLayoutManager();
        int last = parent.getChildCount();
        int i = 0;

        if (!mIsAppliedToFirstLast) {
            if (mIsStartDecoration) {
                if (llm.getPosition(parent.getChildAt(0)) == 0) {
                    i = 1;
                }
            } else if (llm.getPosition(parent.getChildAt(last - 1)) == llm.getItemCount() - 1) {
                last--;
            }
        }

        final int orientation = llm.getOrientation();
        View child;
        for (; i < last; i++) {
            child = parent.getChildAt(i);

            if (orientation == RecyclerView.HORIZONTAL) {
                if (mIsStartDecoration) {
                    getStartBounds(child, llm, mRect);
                } else {
                    getEndBounds(child, llm, mRect);
                }

            } else {
                if (mIsStartDecoration) {
                    getTopBounds(child, mRect);
                } else {
                    getBottomBounds(child, mRect);
                }
            }
            canvas.drawRect(mRect, mPaint);
        }
    }

    /**
     * Determine the offsets for the decorations.
     *
     * @param outRect The Rect of offsets to be added around the child view
     * @param view    The child view to be decorated
     * @param parent  The RecyclerView containing the views to be decorated
     * @param state   The current RecyclerView.State of the RecyclerView
     */
    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        final LinearLayoutManager llm;

        try {
            llm = (LinearLayoutManager) parent.getLayoutManager();
        } catch (ClassCastException e) {
            throw new ClassCastException(ERROR_WRONG_LAYOUTMANAGER);
        }
        if (llm == null) {
            throw new IllegalStateException(ERROR_LAYOUTMANAGER_CANNOT_BE_NULL);
        }
        final int position = llm.getPosition(view);

        super.getItemOffsets(outRect, view, parent, state);

        final int spans =
            (llm instanceof GridLayoutManager) ? ((GridLayoutManager) llm).getSpanCount() : 1;
        final int orientation = llm.getOrientation();
        boolean hasAnOffset;

        if (mIsAppliedToFirstLast) {
            // All items have this decoration
            hasAnOffset = true;
        } else if (position < spans) {
            // First column if orientation horizontal or first row for vertical orientation
            hasAnOffset = !mIsStartDecoration;
        } else {
            // Last column if orientation horizontal or last row for vertical orientation
            // Start decoration is always applied unless in items where (position < spans) when
            // its placement is conditional.
            hasAnOffset = position < (llm.getItemCount() - spans) || mIsStartDecoration;
        }

        if (!hasAnOffset) {
            return;
        }

        if (orientation == RecyclerView.HORIZONTAL) {
            if (mIsStartDecoration == (llm.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL)) {
                // Start decoration and layout is RTL or end decoration and layout is LTR.
                outRect.right = mWidth;
            } else {
                outRect.left = mWidth;
            }
        } else if (mIsStartDecoration) {
            outRect.top = mWidth;
        } else {
            outRect.bottom = mWidth;
        }
    }

    private void getStartBounds(View child, LinearLayoutManager llm, Rect rect) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

        if (llm.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            rect.left = child.getRight() + params.getMarginStart();
            rect.right = rect.left + mWidth;
        } else {
            rect.right = child.getLeft() - params.getMarginStart();
            rect.left = rect.right - mWidth;
        }
        rect.top = child.getTop();
        rect.bottom = child.getBottom();
    }

    private void getTopBounds(View child, Rect rect) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

        rect.left = child.getLeft();
        rect.right = child.getRight();
        rect.bottom = child.getTop() - params.topMargin;
        rect.top = rect.bottom - mWidth;
    }

    private void getEndBounds(View child, LinearLayoutManager llm, Rect rect) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

        if (llm.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            rect.right = child.getLeft() - params.getMarginEnd();
            rect.left = rect.right - mWidth;
        } else {
            rect.left = child.getRight() + params.getMarginEnd();
            rect.right = rect.left + mWidth;
        }
        rect.top = child.getTop();
        rect.bottom = child.getBottom();
    }

    private void getBottomBounds(View child, Rect rect) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

        rect.left = child.getLeft();
        rect.right = child.getRight();
        rect.top = child.getBottom() + params.bottomMargin;
        rect.bottom = rect.top + mWidth;
    }

    private static final String ERROR_LAYOUTMANAGER_CANNOT_BE_NULL
        = "RecyclerView layout manager must be set prior to setting decorations.";

    private static final String ERROR_WRONG_LAYOUTMANAGER
        = "RecyclerView layout manager must be instance of LinearLayoutManager.";
}