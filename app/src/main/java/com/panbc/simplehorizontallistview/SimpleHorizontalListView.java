package com.panbc.simplehorizontallistview;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by panbc on 15/8/17.<br/>
 * SimpleHorizontalListView basically imitates the official {@link ListView} whereas it only horizontally layouts child views properly, lacking features like classifying
 * children by VIEW_TYPE
 */
public class SimpleHorizontalListView extends AdapterView<ListAdapter> {

    private RecycleBin mRecycler = new RecycleBin();

    protected ListAdapter mAdapter;

    private DataSetObserver mDataObserver = new MyDataSetObserver();

    private boolean mDataChanged;

    /**
     * Indicates that this view is currently being laid out.
     */
    boolean mInLayout = false;

    /**
     * When set to true, calls to requestLayout() will not propagate up the parent hierarchy.
     * This is used to layout the children during a layout pass.
     */
    boolean mBlockLayoutRequests = false;

    private int mFirstPosition = 0;

    private int mFirstItemLeft = Integer.MIN_VALUE;

    private int mItemCount;

    private int mHeightMeasureSpec;

    private Paint mCenterPaint;

    private boolean mShouldStopFling;

    private GestureDetector mGestureDetector;

    private FlingRunnable mFlingRunnable = new FlingRunnable();

    private OnItemClickListener mOnItemClicked;

    private OnScrollListener mOnScrollListener;

    public SimpleHorizontalListView(Context context) {
        super(context);
        initMyGallery();
    }

    public SimpleHorizontalListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        initMyGallery();
    }

    public SimpleHorizontalListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initMyGallery();
    }

    @Override
    public ListAdapter getAdapter() {
        return mAdapter;
    }


    @Override
    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClicked = listener;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataObserver);
        }
        mAdapter = adapter;
        mAdapter.registerDataSetObserver(mDataObserver);
        if (mAdapter != null) {
            mItemCount = mAdapter.getCount();
        }
        reset();
    }

    private void initMyGallery() {
        mCenterPaint = new Paint();
        mCenterPaint.setColor(Color.YELLOW);
        mGestureDetector = new GestureDetector(getContext(), mOnGestureListener);
        bindGestureDetector();
        setWillNotDraw(false);
    }

    private void bindGestureDetector() {
        // Generic touch listener that can be applied to any view that needs to process gestures
        final OnTouchListener gestureListenerHandler = new OnTouchListener() {
            @Override
            public boolean onTouch(final View v, final MotionEvent event) {
                // Delegate the touch event to our gesture detector
                return mGestureDetector.onTouchEvent(event);
            }
        };

        setOnTouchListener(gestureListenerHandler);
    }

    public void setFirstPosition(int position) {
        mFirstPosition = position;
    }


    public void setFirstItemLeft(int left) {
        mFirstItemLeft = left;
    }


    private synchronized void reset() {
        initMyGallery();
        removeAllViewsInLayout();
        invalidate();
    }


    @Override
    public void requestLayout() {
        if (!mBlockLayoutRequests && !mInLayout) {
            super.requestLayout();
        }
    }

    @Override
    public View getSelectedView() {
        return null;
    }

    @Override
    public void setSelection(int position) {
        //// TODO: 16/3/3
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Sets up mListPadding
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mHeightMeasureSpec = heightMeasureSpec;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mItemCount == 0) {
            reset();
            return;
        }
        mInLayout = true;
        if (changed) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).forceLayout();
            }
        }
        fillList();
        mFirstItemLeft = getChildAt(0).getLeft();
        mInLayout = false;

    }

    private void fillList() {
        final int childCount = getChildCount();
        final boolean dataChanged = mDataChanged;
        if (dataChanged) {
            for (int i = 0; i < childCount; i++) {
                mRecycler.addScrapView(getChildAt(i), mFirstPosition + i);
            }
        } else {
            mRecycler.fillActiveViews(childCount, mFirstPosition);
        }
        int firstLeft = getPaddingLeft();
        if (childCount != 0) {
            firstLeft = getChildAt(0).getLeft();
            detachAllViewsFromParent();
        }
        fillListRight(mFirstPosition, mFirstItemLeft == Integer.MIN_VALUE ? firstLeft : mFirstItemLeft);
        correctTooLeft();
        mDataChanged = false;
        mRecycler.scrapActiveViews();
        invalidate();


    }

    private void correctTooLeft() {
        final int childCount = getChildCount();
        if (mFirstPosition + childCount == mItemCount && childCount > 0) {
            int end = getRight() - getLeft() - getPaddingRight();
            View lastChild = getChildAt(getChildCount() - 1);
            View firstChild = getChildAt(0);
            int rightOffset = end - lastChild.getRight();
            if (rightOffset > 0 && (mFirstPosition > 0 || getPaddingLeft() - firstChild.getLeft() > 0)) {
                if (mFirstPosition == 0) {
                    rightOffset = Math.min(rightOffset, getPaddingLeft() - firstChild.getLeft());
                }
                for (int i = 0; i < childCount; i++) {
                    final View v = getChildAt(i);
                    v.offsetLeftAndRight(rightOffset);
                }
                if (mFirstPosition > 0) {
                    fillListLeft(mFirstPosition - 1, firstChild.getLeft());
                }
            }
        }
    }

    private void fillListLeft(int startPos, int startLeft) {
        int start = getPaddingLeft();
        int nextLeft = startLeft;
        int childIndex = startPos;
        while (nextLeft >= start && childIndex >= 0) {
            View child = obtainView(childIndex, nextLeft, false);
            nextLeft = child.getLeft();
            childIndex--;
        }
        mFirstPosition = childIndex + 1;

    }

    private void correctTooRight() {
        final int childCount = getChildCount();
        if (mFirstPosition == 0 && childCount > 0) {
            int start = getPaddingLeft();
            View lastChild = getChildAt(getChildCount() - 1);
            View firstChild = getChildAt(0);
            int leftOffset = start - firstChild.getLeft();
            if (leftOffset < 0) {
                for (int i = 0; i < childCount; i++) {
                    final View v = getChildAt(i);
                    v.offsetLeftAndRight(leftOffset);
                }
                if (mFirstPosition + childCount < mItemCount) {
                    fillListRight(mFirstPosition + childCount + 1, lastChild.getRight());
                }
            }
        }
    }

    private void fillListRight(int startPos, int startRight) {
        int end = getRight() - getLeft() - getPaddingRight();
        int nextRight = startRight;
        int childIndex = startPos;
        while (nextRight <= end && childIndex <= mAdapter.getCount() - 1) {
            View child = obtainView(childIndex, nextRight, true);
            nextRight = child.getRight();
            childIndex++;
        }
    }

    private void setupChild(View child, int edge, boolean toRight, boolean isRecycled) {
        LayoutParams p = (LayoutParams) child.getLayoutParams();
        if (p == null) {
            p = new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.MATCH_PARENT);
        }
        if (isRecycled && !child.isLayoutRequested()) {
            attachViewToParent(child, toRight ? -1 : 0, p);

            int left = toRight ? edge : edge - child.getMeasuredWidth();
            child.offsetLeftAndRight(left - child.getLeft());
        } else {
            addViewInLayout(child, toRight ? -1 : 0, p, true);

            int childHeightSpec = ViewGroup.getChildMeasureSpec(mHeightMeasureSpec,
                    getPaddingTop() + getPaddingBottom(), p.height);
            int lpWidth = p.width;
            int childWidthSpec;
            if (lpWidth > 0) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(lpWidth, MeasureSpec.EXACTLY);
            } else {
                childWidthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            }
            child.measure(childWidthSpec, childHeightSpec);
            int left = toRight ? edge : edge - child.getMeasuredWidth();
            int right = left + child.getMeasuredWidth();
            int top = getPaddingTop();
            int bottom = top + child.getMeasuredHeight();
            child.layout(left, top, right, bottom);
        }
    }

    private View obtainView(int position, int edge, boolean toRight) {
        View child = mRecycler.getActiveView(position);
        boolean isRecycle = true;
        if (child == null) {
            View scrapView = mRecycler.getScrapView(position);
            if (scrapView != null) {
                child = mAdapter.getView(position, scrapView, this);
                if (child != scrapView) {
                    isRecycle = false;
                    mRecycler.addScrapView(scrapView, position);
                }
            } else {
                isRecycle = false;
                child = mAdapter.getView(position, null, this);
            }
        }
        setupChild(child, edge, toRight, isRecycle);
        return child;
    }

    public boolean trackMotionScroll(int deltaX) {
        return trackMotionScroll(deltaX, true);
    }

    /**
     * @param deltaX
     * @param notify true to call mOnScrollListener.onScroll(), false otherwise
     * @return
     */
    public boolean trackMotionScroll(int deltaX, boolean notify) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
        final int firstPosition = mFirstPosition;
        final int lastPosition = mFirstPosition + getChildCount() - 1;

        //see whether to scroll or not
        if (firstPosition == 0 && deltaX > 0) {
            View firstChild = getChildAt(0);
            int leftOffset = getPaddingLeft() - firstChild.getLeft();
            if (leftOffset <= 0) {
                mShouldStopFling = true;
                return false;
            }
            deltaX = Math.min(leftOffset, deltaX);
        } else if (lastPosition == mItemCount - 1 && deltaX < 0) {
            View lastChild = getChildAt(lastPosition - firstPosition);
            int rightOffset = getRight() - getLeft() - lastChild.getRight() - getPaddingRight();
            if (rightOffset >= 0) {
                mShouldStopFling = true;
                return false;
            }
            deltaX = -Math.min(Math.abs(rightOffset), Math.abs(deltaX));
        }

        removeNonVisibleView(deltaX);

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View v = getChildAt(i);
            v.offsetLeftAndRight(deltaX);
        }
        if (deltaX < 0) {
            //gesture is moving towards left
            fillListRight(lastPosition + 1, getChildAt(childCount - 1).getRight());
            correctTooLeft();
        } else if (deltaX > 0) {
            //gesture is moving towards right
            fillListLeft(firstPosition - 1, getChildAt(0).getLeft());
            correctTooRight();
        }
        mFirstItemLeft = getChildAt(0).getLeft();
        if (notify && mOnScrollListener != null) {
            mOnScrollListener.onScroll(this, deltaX, mFirstPosition, mFirstItemLeft);
        }
        invalidate();
        return true;
    }

    private void removeNonVisibleView(int deltaX) {
        if (deltaX < 0) {
            int start = getPaddingLeft() - deltaX;
            int childIndex = mFirstPosition;
            while (childIndex < mItemCount) {
                View child = getChildAt(0);
                if (child.getRight() >= start) {
                    break;
                }
                mRecycler.addScrapView(child, childIndex);
                detachViewFromParent(child);
                childIndex++;
            }
            mFirstPosition = childIndex;
        } else if (deltaX > 0) {
            int end = getRight() - getLeft() - getPaddingRight() - deltaX;
            int childIndex = mFirstPosition + getChildCount() - 1;
            while (childIndex >= 0) {
                View child = getChildAt(getChildCount() - 1);
                if (child.getLeft() <= end) {
                    break;
                }
                mRecycler.addScrapView(child, childIndex);
                detachViewFromParent(child);
                childIndex--;
            }
        }
    }

    class MyDataSetObserver extends DataSetObserver {

        private Parcelable mInstanceState = null;

        @Override
        public void onChanged() {
            mDataChanged = true;
            mItemCount = getAdapter().getCount();

            // Detect the case where a cursor that was previously invalidated has
            // been repopulated with new data.
            if (getAdapter().hasStableIds() && mInstanceState != null && mItemCount > 0) {
                onRestoreInstanceState(mInstanceState);
                mInstanceState = null;
            }
            requestLayout();
        }

        @Override
        public void onInvalidated() {
            mDataChanged = true;

            if (getAdapter().hasStableIds()) {
                // Remember the current state for the case where our hosting activity is being
                // stopped and later restarted
                mInstanceState = onSaveInstanceState();
            }

            // Data is invalid so we should reset our state
            mItemCount = 0;
            requestLayout();
        }

    }


    private class FlingRunnable implements Runnable {
        /**
         * Tracks the decay of a fling scroll
         */
        private Scroller mScroller;

        /**
         * X value reported by mScroller on the previous fling
         */
        private int mLastFlingX;

        public FlingRunnable() {
            mScroller = new Scroller(getContext());
        }

        private void startCommon() {
            // Remove any pending flings
            removeCallbacks(this);
        }

        public void startUsingVelocity(int initialVelocity) {
            if (initialVelocity == 0) return;

            startCommon();

            int initialX = initialVelocity < 0 ? Integer.MAX_VALUE : 0;
            mLastFlingX = initialX;
            mScroller.fling(initialX, 0, initialVelocity, 0,
                    0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
            post(this);
        }


        public void stop() {
            removeCallbacks(this);
            endFling();
        }

        private void endFling() {
            /*
             * Force the scroller's status to finished (without setting its
             * position to the end)
             */
            mScroller.forceFinished(true);

        }

        @Override
        public void run() {

            if (mItemCount == 0) {
                endFling();
                return;
            }

            mShouldStopFling = false;

            final Scroller scroller = mScroller;
            boolean more = scroller.computeScrollOffset();
            final int x = scroller.getCurrX();

            // Flip sign to convert finger direction to list items direction
            // (e.g. finger moving down means list is moving towards the top)
            int delta = mLastFlingX - x;

            // Pretend that each frame of a fling scroll is a touch scroll
            if (delta > 0) {
                // Don't fling more than 1 screen
                delta = Math.min(getWidth() - getPaddingLeft() - getPaddingRight() - 1, delta);
            } else {
                // Don't fling more than 1 screen
                delta = Math.max(-(getWidth() - getPaddingLeft() - getPaddingRight() - 1), delta);
            }

            trackMotionScroll(delta);

            if (more && !mShouldStopFling) {
                mLastFlingX = x;
                post(this);
            } else {
                endFling();
            }
        }

    }


    public static class LayoutParams extends ViewGroup.LayoutParams {
        int scrappedFromPosition;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }


        public LayoutParams(int w, int h) {
            super(w, h);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }


    protected boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        // Fling the gallery!
        mFlingRunnable.startUsingVelocity((int) -velocityX);
        return true;
    }


    private GestureDetector.OnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            mFlingRunnable.stop();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return SimpleHorizontalListView.this.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return trackMotionScroll(-(int) distanceX);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Rect viewRect = new Rect();
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                int left = child.getLeft();
                int right = child.getRight();
                int top = child.getTop();
                int bottom = child.getBottom();
                viewRect.set(left, top, right, bottom);
                if (viewRect.contains((int) e.getX(), (int) e.getY())) {
                    if (mOnItemClicked != null) {
                        mOnItemClicked.onItemClick(SimpleHorizontalListView.this, child, mFirstPosition + i, mAdapter.getItemId(mFirstPosition + i));
                    }
                    return true;
                }
            }
            return false;
        }

    };

    public void setOnScrollListener(OnScrollListener listener) {
        mOnScrollListener = listener;
    }

    public interface OnScrollListener {
        void onScroll(View view, int deltaX, int firstPosition, int firstItemLeft);
    }

    public interface RecyclerListener {
        /**
         * Indicates that the specified View was moved into the recycler's scrap heap.
         * The view is not displayed on screen any more and any expensive resource
         * associated with the view should be discarded.
         *
         * @param view
         */
        void onMovedToScrapHeap(View view);
    }

    public void setRecyclerListener(RecyclerListener listener) {
        mRecycler.mRecyclerListener = listener;
    }

    class RecycleBin {
        private RecyclerListener mRecyclerListener;

        /**
         * The position of the first view stored in mActiveViews.
         */
        private int mFirstActivePosition;

        /**
         * Views that were on screen at the start of layout. This array is populated at the start of
         * layout, and at the end of layout all view in mActiveViews are moved to mScrapViews.
         * Views in mActiveViews represent a contiguous range of Views, with position of the first
         * view store in mFirstActivePosition.
         */
        private View[] mActiveViews = new View[0];


        private ArrayList<View> mCurrentScrap;

        public RecycleBin() {
            mCurrentScrap = new ArrayList<View>();

        }

        public void markChildrenDirty() {
            final ArrayList<View> scrap = mCurrentScrap;
            final int scrapCount = scrap.size();
            for (int i = 0; i < scrapCount; i++) {
                scrap.get(i).forceLayout();
            }
        }

        /**
         * Clears the scrap heap.
         */
        void clear() {
            final ArrayList<View> scrap = mCurrentScrap;
            final int scrapCount = scrap.size();
            for (int i = 0; i < scrapCount; i++) {
                removeDetachedView(scrap.remove(scrapCount - 1 - i), false);
            }
        }


        /**
         * Fill ActiveViews with all of the children of the AbsListView.
         *
         * @param childCount          The minimum number of views mActiveViews should hold
         * @param firstActivePosition The position of the first view that will be stored in
         *                            mActiveViews
         */
        void fillActiveViews(int childCount, int firstActivePosition) {
            if (mActiveViews.length < childCount) {
                mActiveViews = new View[childCount];
            }
            mFirstActivePosition = firstActivePosition;

            final View[] activeViews = mActiveViews;
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                SimpleHorizontalListView.LayoutParams lp = (SimpleHorizontalListView.LayoutParams) child.getLayoutParams();
                // Don't put header or footer views into the scrap heap
                if (lp != null) {
                    // Note:  We do place AdapterView.ITEM_VIEW_TYPE_IGNORE in active views.
                    //        However, we will NOT place them into scrap views.
                    activeViews[i] = child;
                }
            }
        }

        /**
         * Get the view corresponding to the specified position. The view will be removed from
         * mActiveViews if it is found.
         *
         * @param position The position to look up in mActiveViews
         * @return The view if it is found, null otherwise
         */
        View getActiveView(int position) {
            int index = position - mFirstActivePosition;
            final View[] activeViews = mActiveViews;
            if (index >= 0 && index < activeViews.length) {
                final View match = activeViews[index];
                activeViews[index] = null;
                return match;
            }
            return null;
        }

        /**
         * @return A view from the ScrapViews collection. These are unordered.
         */
        View getScrapView(int position) {
            return retrieveFromScrap(mCurrentScrap, position);
        }

        /**
         * Put a view into the ScapViews list. These views are unordered.
         *
         * @param scrap The view to add
         */
        void addScrapView(View scrap, int position) {
            SimpleHorizontalListView.LayoutParams lp = (SimpleHorizontalListView.LayoutParams) scrap.getLayoutParams();
            if (lp == null) {
                return;
            }
            lp.scrappedFromPosition = position;
            mCurrentScrap.add(scrap);

            if (mRecyclerListener != null) {
                mRecyclerListener.onMovedToScrapHeap(scrap);
            }
        }

        /**
         * Move all views remaining in mActiveViews to mScrapViews.
         */
        void scrapActiveViews() {
            final View[] activeViews = mActiveViews;
            final boolean hasListener = mRecyclerListener != null;

            ArrayList<View> scrapViews = mCurrentScrap;
            final int count = activeViews.length;
            for (int i = count - 1; i >= 0; i--) {
                final View victim = activeViews[i];
                if (victim != null) {
                    final SimpleHorizontalListView.LayoutParams lp
                            = (SimpleHorizontalListView.LayoutParams) victim.getLayoutParams();

                    activeViews[i] = null;


                    lp.scrappedFromPosition = mFirstActivePosition + i;
                    scrapViews.add(victim);

                    if (hasListener) {
                        mRecyclerListener.onMovedToScrapHeap(victim);
                    }
                }
            }

            pruneScrapViews();
        }

        /**
         * Makes sure that the size of mScrapViews does not exceed the size of mActiveViews.
         * (This can happen if an adapter does not recycle its views).
         */
        private void pruneScrapViews() {
            final int maxViews = mActiveViews.length;
            final ArrayList<View> scrapPile = mCurrentScrap;
            int size = scrapPile.size();
            final int extras = size - maxViews;
            size--;
            for (int j = 0; j < extras; j++) {
                removeDetachedView(scrapPile.remove(size--), false);
            }
        }

        /**
         * Puts all views in the scrap heap into the supplied list.
         */
        void reclaimScrapViews(List<View> views) {
            views.addAll(mCurrentScrap);
        }

        /**
         * Updates the cache color hint of all known views.
         *
         * @param color The new cache color hint.
         */

    }

    static View retrieveFromScrap(ArrayList<View> scrapViews, int position) {
        int size = scrapViews.size();
        if (size > 0) {
            // See if we still have a view for this position.
            for (int i = 0; i < size; i++) {
                View view = scrapViews.get(i);
                if (((SimpleHorizontalListView.LayoutParams) view.getLayoutParams())
                        .scrappedFromPosition == position) {
                    scrapViews.remove(i);
                    return view;
                }
            }
            return scrapViews.remove(size - 1);
        } else {
            return null;
        }
    }


}
