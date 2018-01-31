package com.example.snaptoblockdemo;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.view.View;

/*
    The number of items in the RecyclerView should be a multiple of block size; otherwise, the
    extra item views will not be positioned on a block boundary and the final item will fail
    to snap when the end of the data is reached. Pad out with empty item views or simply add
    padding at the end of the RecyclerView.

    Items in the RecyclerView should all have the same size, but small variations in size due to
    decorations and margins are probably OK. The block size to snap to is determined by counting
    full item views, including margins and decorations, that can fit within the on-screen boundaries
    of the RecyclerView.

    General Flow

	          SnapHelper  SnapToBlock			     SnapHelper
    [fling] > onFling() > findTargetSnapPosition() > [starts scroll]

                       SnapHelper               SnapToBlock      SnapToBlock
    ---> [scroll idle] onScrollStateChanged() > findSnapView() > [starts scroll if view doesn't exist]
         ^                                                       SnapHelper
         |                                                     > [scrolls if view does exist]
    [non-fling scroll]
 */

public class SnapToBlock extends SnapHelper {
    private RecyclerView mRecyclerView;

    // Total number of items in a block of views in the RecyclerView (page size.)
    private int mBlocksize;

    // Maxim blocks to move during most vigorous fling.
    private final int mMaxFlingBlocks;

    // Maximum number of positions to move on a fling. (blocksize * max block to move)
    private int mMaxPositionsToMove;

    // Width of a RecyclerView item if orientation is horizonal; height of the item if vertical.
    // This dimension includes the view, margins and decorations.
    private int mItemDimension;

    // When snapping, used to determine direction of snap.
    private int mPriorFirstPosition = RecyclerView.NO_POSITION;

    // Offset within prior position
    private int mPriorPositionOffset;

    // Horizontal/vertical layout helper
    private OrientationHelper mOrientationHelper;

    // Callback interface when blocks are snapped.
    private SnapBlockCallback mSnapBlockCallback;

    // Parameterless constructor that permits only one page to be scroll per fling.
    @SuppressWarnings("unused")
    SnapToBlock() {
        this(1);
    }

    SnapToBlock(int maxFlingBlocks) {
        super();
        mMaxFlingBlocks = maxFlingBlocks;
    }

    @Override
    public void attachToRecyclerView(@Nullable final RecyclerView recyclerView) {
        if (recyclerView == null) {
            mRecyclerView = null;
            mOrientationHelper = null;
            mItemDimension = 0;
            mSnapBlockCallback = null;
        } else if (recyclerView != mRecyclerView) {
            mRecyclerView = recyclerView;
            mOrientationHelper = null;
            mItemDimension = 0;
            initItemDimensionIfNeeded();
        }
        super.attachToRecyclerView(recyclerView);
    }

    // Called when the target view is available and we need to know how much more
    // to scroll to get the target lined up with the side of the RecyclerView.
    @NonNull
    @Override
    public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager lm,
                                              @NonNull View snapView) {
        final int[] out = getScrollToAlignView(lm, snapView);

        if (mSnapBlockCallback != null) {
            if (out[0] == 0 && out[1] == 0) {
                mSnapBlockCallback.onBlockSnapped(lm.getPosition(snapView));
            } else {
                mSnapBlockCallback.onBlockSnap(lm.getPosition(snapView));
            }
        }
        return out;
    }

    // SnapHelper calls findSnapView to find an existing view to scroll to, although our snap view
    // may not always be available. If null is returned, meaning that the view was not found,
    // SnapHelper does not snap at all. So, if the view is not found, we must initiate our own
    // scroll. If a view is returned, SnapHelper does the snap.
    @Override
    public View findSnapView(RecyclerView.LayoutManager lm) {
        initItemDimensionIfNeeded();
        int snapPos = findSnapPosition((LinearLayoutManager) lm);
        if (snapPos == RecyclerView.NO_POSITION) {
            return null;
        }
        View view = lm.findViewByPosition(snapPos);
        if (view != null) {
            return view;
        }
        RecyclerView.SmoothScroller smoothScroller = createScroller(lm);
        if (smoothScroller != null) {
            smoothScroller.setTargetPosition(snapPos);
            lm.startSmoothScroll(smoothScroller);
        }
        return null;
    }

    // We are flinging and need to know where we are heading.
    @Override
    public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager,
                                      int velocityX, int velocityY) {
        initItemDimensionIfNeeded();
        int[] scrollDistance = calculateScrollDistance(velocityX, velocityY);
        if (velocityX != 0) {
            return getTargetPosition(scrollDistance[0]);
        } else if (velocityY != 0) {
            return getTargetPosition(scrollDistance[1]);
        }
        return RecyclerView.NO_POSITION;
    }

    // Does heavy lifting for findSnapView.
    private int findSnapPosition(LinearLayoutManager llm) {
        int firstVisiblePos = llm.findFirstVisibleItemPosition();

        if (firstVisiblePos == RecyclerView.NO_POSITION) {
            return RecyclerView.NO_POSITION;
        }

        int firstVisibleOffset = llm.findViewByPosition(firstVisiblePos).getLeft();
        int snapPos;

        if (firstVisiblePos == mPriorFirstPosition) {
            if (firstVisibleOffset == mPriorPositionOffset) {
                snapPos = firstVisiblePos;
            } else if (isDirectionToBottom(llm, (mPriorPositionOffset - firstVisibleOffset) < 0)) {
                snapPos = roundUpToBlockSize(firstVisiblePos + 1);
            } else {
                snapPos = roundDownToBlockSize(firstVisiblePos);
            }
        } else if (firstVisiblePos > mPriorFirstPosition) {
            // Scrolling toward bottom of data
            snapPos = roundUpToBlockSize(firstVisiblePos);
        } else {
            // Scrolling toward top of data
            snapPos = roundDownToBlockSize(firstVisiblePos);
        }

        mPriorFirstPosition = firstVisiblePos;
        mPriorPositionOffset = firstVisibleOffset;
        return snapPos;
    }

    /*
     Calculate the amount of scroll needed to align the target view with the layout's edge.
    */
    int[] getScrollToAlignView(RecyclerView.LayoutManager lm, View targetView) {
        int[] scrollToAlign = new int[2];

        if (lm.canScrollHorizontally()) {
            if (lm.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                scrollToAlign[0] = getOrientationHelper(lm).getDecoratedEnd(targetView)
                    - mRecyclerView.getWidth();
            } else {
                scrollToAlign[0] = getOrientationHelper(lm).getDecoratedStart(targetView);
            }
        }

        if (lm.canScrollVertically()) {
            scrollToAlign[1] = getOrientationHelper(lm).getDecoratedStart(targetView);
        }

        return scrollToAlign;
    }

    /*
       Calculate the adapter position to scroll to in the RecyclerView given a scroll amount.
       This method is only invoked after a fling is initiated in #findTargetSnapPosition.
    */
    int getTargetPosition(int scroll) {
        int positionsToMove;
        final LinearLayoutManager llm = (LinearLayoutManager) mRecyclerView.getLayoutManager();

        positionsToMove = roundUpToBlockSize(Math.abs(scroll) / mItemDimension);

        if (positionsToMove < mBlocksize) {
            // Must move at least one block
            positionsToMove = mBlocksize;
        } else if (positionsToMove > mMaxPositionsToMove) {
            // Clamp number of positions to move.
            positionsToMove = mMaxPositionsToMove;
        }

        // Working with absolute value of scroll, so decide if we need to flip the sign
        // of the positions to move or not. We make positions to move negative if the
        // scroll < 0 or the orientation is horizontal and the direction of the layout is
        // RTL. If both these conditions are true, the value gets flipped twice, so there
        // is no change. Only flip if one condition is true by not both (conditions differ).
        if ((scroll < 0) != (llm.getOrientation() == RecyclerView.HORIZONTAL &&
            llm.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL)) {
            positionsToMove *= -1;
        }

        int targetPos;
        if (isDirectionToBottom(llm, scroll < 0)) {
            // Scrolling toward the bottom of data.
            targetPos = roundDownToBlockSize(llm.findFirstVisibleItemPosition()) + positionsToMove;
            if (targetPos >= llm.getItemCount()) {
                targetPos = llm.getItemCount() - llm.getItemCount() % mBlocksize;
            }
        } else {
            // Scrolling toward the top of the data.
            targetPos = roundDownToBlockSize(llm.findFirstVisibleItemPosition() + mBlocksize)
                + positionsToMove;
            if (targetPos < 0) {
                targetPos = 0;
            }
        }

        return targetPos;
    }

    private void initItemDimensionIfNeeded() {
        if (mItemDimension != 0 || mRecyclerView == null) {
            return;
        }

        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) {
            return;
        }

        View child = layoutManager.getChildAt(0);
        if (child == null) {
            return;
        }

        // Since mItemDimension and mBlockSize are set lazily, the RecyclerView may have
        // scrolled. Determine the scroll amount to align a block to edge of the RecyclerView
        // for calculations.
        int adj;
        Rect bounds = new Rect();
        layoutManager.getDecoratedBoundsWithMargins(child, bounds);
        if (layoutManager.canScrollHorizontally()) {
            if (layoutManager.getLayoutDirection() == RecyclerView.LAYOUT_DIRECTION_LTR) {
                adj = bounds.left;
            } else {
                adj = bounds.right - mRecyclerView.getWidth();
            }
        } else {
            adj = bounds.top;
        }
        // Look at adjusted child views that fully fit into the RecyclerView to determine the block
        // size and the greatest item dimension.
        mItemDimension = 0;
        for (int i = 0; i < layoutManager.getChildCount(); i++) {
            child = layoutManager.getChildAt(i);
            layoutManager.getDecoratedBoundsWithMargins(child, bounds);
            if (layoutManager.canScrollHorizontally()) {
                bounds.left -= adj;
                bounds.right -= adj;
                if (bounds.right <= mRecyclerView.getWidth()) {
                    mBlocksize++;
                    if ((bounds.right - bounds.left) > mItemDimension) {
                        mItemDimension = bounds.right - bounds.left;
                    }
                }
            } else if (layoutManager.canScrollVertically()) {
                bounds.top -= adj;
                bounds.bottom -= adj;
                if (bounds.bottom <= mRecyclerView.getHeight()) {
                    mBlocksize++;
                    if ((bounds.bottom - bounds.top) > mItemDimension) {
                        mItemDimension = bounds.bottom - bounds.top;
                    }
                }
            }
        }
        if (mBlocksize == 0) { // Must have at least one item in block.
            mBlocksize = 1;
        }
        mMaxPositionsToMove = mBlocksize * mMaxFlingBlocks;
    }

    private OrientationHelper getOrientationHelper(RecyclerView.LayoutManager layoutManager) {
        if (mOrientationHelper == null) {
            if (layoutManager.canScrollHorizontally()) {
                mOrientationHelper = OrientationHelper.createHorizontalHelper(layoutManager);
            } else if (layoutManager.canScrollVertically()) {
                mOrientationHelper = OrientationHelper.createVerticalHelper(layoutManager);
            } else {
                throw new IllegalStateException("RecyclerView must be scrollable");
            }
        }
        return mOrientationHelper;
    }

    private int roundDownToBlockSize(int trialPosition) {
        return trialPosition - trialPosition % mBlocksize;
    }

    private int roundUpToBlockSize(int trialPosition) {
        return roundDownToBlockSize(trialPosition + mBlocksize - 1);
    }

    public void setSnapBlockCallback(@Nullable SnapBlockCallback callback) {
        mSnapBlockCallback = callback;
    }

    boolean isDirectionToBottom(RecyclerView.LayoutManager layoutManager, boolean velocityNegative) {
        if (layoutManager.canScrollVertically()) {
            return !velocityNegative;
        }
        return layoutManager.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL == velocityNegative;
    }

    public interface SnapBlockCallback {
        void onBlockSnap(int snapTarget);

        void onBlockSnapped(int snappedPosition);

    }

    @SuppressWarnings("unused")
    private static final String TAG = "SnapToBlock";
}