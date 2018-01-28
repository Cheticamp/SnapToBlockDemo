package com.example.snaptoblockdemo;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

// Demonstration project for SnapToBlock

public class MainActivity extends AppCompatActivity
    implements SettingsComplete, SnapToBlock.SnapBlockCallback {

    // From settings dialog
    private int mSpans;
    private int mScrollAxis;
    private int mRows;
    private int mColumns;
    private int mMaxFlingPages;
    private int mLayoutDirection;
    private boolean mHasStartMargin;
    private boolean mHasEndMargin;
    private boolean mHasFirstLastMargins;
    private boolean mHasStartDecoration;
    private boolean mHasEndDecoration;
    private boolean mHasFirstLastDecorations;
    private boolean mIsSnapEnabled;

    private int mDefaultMargin;
    private int mDecorationWidth;
    private int mCellWidth;
    private int mCellHeight;
    private int mRecyclerViewMaxWidth;
    private int mRecyclerViewMaxHeight;
    private RecyclerView mRecyclerView;

    private int mSnapTarget = RecyclerView.NO_POSITION;
    private int mSnappedPosition = RecyclerView.NO_POSITION;

    // Count number of view holders created by type.
    private int[] mViewHolderCount = new int[6];

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        loadData();
        mDefaultMargin = (int) getResources().getDimension(R.dimen.margin_width);
        mDecorationWidth = (int) getResources().getDimension(R.dimen.decoration_width);

        // Determine the maximum width/height of the RecyclerView. This code is dependent
        // upon the structure of activity_main.xml.
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.getViewTreeObserver()
            .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    mRecyclerViewMaxWidth = mRecyclerView.getWidth();
                    // Let the RecyclerView fill the space not used by the table.
                    mRecyclerViewMaxHeight = findViewById(R.id.mainLayout).getHeight() -
                        mRecyclerView.getTop() - findViewById(R.id.recyclerViewParams).getBottom();
                    createViews();
                }
            });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                editSettings();
                return true;

            case R.id.action_scroll_left:
                int pos;

                if (mRecyclerView.getLayoutManager().getLayoutDirection()
                    == RecyclerView.LAYOUT_DIRECTION_LTR) {
                    pos = 0;
                } else {
                    pos = mRecyclerView.getLayoutManager().getItemCount() - 1;
                }
                mRecyclerView
                    .smoothScrollToPosition(pos);
                return true;

            case R.id.action_scroll_right:
                if (mRecyclerView.getLayoutManager().getLayoutDirection()
                    == RecyclerView.LAYOUT_DIRECTION_RTL) {
                    pos = 0;
                } else {
                    pos = mRecyclerView.getLayoutManager().getItemCount() - 1;
                }
                mRecyclerView
                    .smoothScrollToPosition(pos);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSettingsComplete(Bundle response) {
        unpackBundle(response);
        storeData();
        createViews();
    }

    @Override
    public void onBlockSnap(int snapTarget) {
        mSnapTarget = snapTarget;
        mSnappedPosition = RecyclerView.NO_POSITION;
        // notifyItemChanged() causes flashing
        //        mRecyclerView.getAdapter().notifyItemChanged(snapTarget);
        TextView textView = (TextView) mRecyclerView.getLayoutManager()
            .findViewByPosition(snapTarget);
        if (textView != null) {
            textView.setText(getString(R.string.snap_target, snapTarget));
        }
    }

    @Override
    public void onBlockSnapped(int snappedPosition) {
        mSnappedPosition = snappedPosition;
        mSnapTarget = RecyclerView.NO_POSITION;
        // notifyItemChanged() causes flashing
        // mRecyclerView.getAdapter().notifyItemChanged(snappedPosition);
        TextView textView = (TextView) mRecyclerView.getLayoutManager()
            .findViewByPosition(snappedPosition);
        if (textView != null) {
            textView.setText(getString(R.string.snapped_position, snappedPosition));
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void createViews() {
        setContentView(R.layout.activity_main);
        populateTable();

        mSnapTarget = RecyclerView.NO_POSITION;
        mSnappedPosition = RecyclerView.NO_POSITION;
        mRecyclerView = findViewById(R.id.recyclerView);

        // SnapToBlock will need to set the fling listener.
        mRecyclerView.setOnFlingListener(null);

        // Need to set the layout manager prior to setting the item decorations.
        mSpans = (mScrollAxis == RecyclerView.HORIZONTAL) ? mRows : mColumns;
        if (mSpans == 1) {
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this, mScrollAxis, false));
        } else {
            mRecyclerView.setLayoutManager(new GridLayoutManager(this, mSpans, mScrollAxis, false));
        }

        if (mHasStartDecoration) {
            mRecyclerView.addItemDecoration(
                new ItemDecorations(ContextCompat.getColor(this, android.R.color.holo_orange_light),
                                    mDecorationWidth, true, mHasFirstLastDecorations));
        }
        if (mHasEndDecoration) {
            mRecyclerView.addItemDecoration(
                new ItemDecorations(ContextCompat.getColor(this, android.R.color.holo_green_light),
                                    mDecorationWidth, false, mHasFirstLastDecorations));
        }

        mRecyclerView.setLayoutDirection(mLayoutDirection);

        // Define the width and height of the RecyclerView.
        int recyclerWidth;
        int recyclerHeight;

        // Extra space to allocate to the RecyclerView to create an item on the display that is
        // partially displayed (for testing).
        int excessWidthOrHeight = 0;
        int marginDecorationSpace = 0;
        if (mHasStartDecoration) {
            marginDecorationSpace += mDecorationWidth;
        }
        if (mHasEndDecoration) {
            marginDecorationSpace += mDecorationWidth;
        }
        if (mHasStartMargin) {
            marginDecorationSpace += mDefaultMargin;
        }
        if (mHasEndMargin) {
            marginDecorationSpace += mDefaultMargin;
        }
        if (mScrollAxis == RecyclerView.HORIZONTAL) {
            mCellWidth = (mRecyclerViewMaxWidth - excessWidthOrHeight -
                marginDecorationSpace * mColumns) / mColumns;
            recyclerWidth = (mCellWidth + marginDecorationSpace) * mColumns + excessWidthOrHeight;
            //noinspection SuspiciousNameCombination
            mCellHeight = mRecyclerViewMaxHeight / mRows;
            float aspectRatio = (float) mCellWidth / mCellHeight;
            if (aspectRatio < PREFERRED_ASPECT_RATIO) {
                mCellHeight = (int) ((float) mCellWidth / PREFERRED_ASPECT_RATIO);
            }
            recyclerHeight = mCellHeight * mRows;
        } else {
            // Set up for vertical scrolling
            mCellHeight = (mRecyclerViewMaxHeight - excessWidthOrHeight
                - marginDecorationSpace * mRows) / mRows;
            recyclerHeight = (mCellHeight + marginDecorationSpace) * mRows + excessWidthOrHeight;
            mCellWidth = mRecyclerViewMaxWidth / mColumns;
            recyclerWidth = mCellWidth * mColumns;
        }
        ConstraintLayout.LayoutParams lp =
            (ConstraintLayout.LayoutParams) mRecyclerView.getLayoutParams();
        lp.width = recyclerWidth;
        lp.height = recyclerHeight;
        mRecyclerView.setAdapter(new RecyclerViewAdapter());

        if (mIsSnapEnabled) {
            final SnapToBlock snapToBlock = new SnapToBlock(mMaxFlingPages);
            snapToBlock.setSnapBlockCallback(MainActivity.this);
            snapToBlock.attachToRecyclerView(mRecyclerView);
        }
    }

    private void editSettings() {
        SettingsFragment.newInstance(packBundle()).show(getSupportFragmentManager(),
                                                        SettingsFragment.TAG);
    }

    void loadData() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        mScrollAxis = prefs.getInt(KEY_SCROLL_AXIS, RecyclerView.HORIZONTAL);
        mRows = prefs.getInt(KEY_ROWS, 3);
        mColumns = prefs.getInt(KEY_COLUMNS, 3);
        mMaxFlingPages = prefs.getInt(KEY_MAX_FLING_PAGES, 1);
        mLayoutDirection = prefs.getInt(KEY_LAYOUT_DIRECTION, RecyclerView.LAYOUT_DIRECTION_LTR);
        mHasStartMargin = prefs.getBoolean(KEY_HAS_START_MARGIN, false);
        mHasEndMargin = prefs.getBoolean(KEY_HAS_END_MARGIN, false);
        mHasStartDecoration = prefs.getBoolean(KEY_HAS_START_DECORATION, false);
        mHasEndDecoration = prefs.getBoolean(KEY_HAS_END_DECORATION, false);
        mHasFirstLastMargins = prefs.getBoolean(KEY_HAS_FIRST_LAST_MARGINS, false);
        mHasFirstLastDecorations = prefs.getBoolean(KEY_HAS_FIRST_LAST_DECORATIONS, false);
        mIsSnapEnabled = prefs.getBoolean(KEY_IS_SNAP_ENABLED, true);
    }

    private Bundle packBundle() {
        Bundle fragArgs = new Bundle();

        fragArgs.putInt(KEY_SCROLL_AXIS, mScrollAxis);
        fragArgs.putInt(KEY_ROWS, mRows);
        fragArgs.putInt(KEY_COLUMNS, mColumns);
        fragArgs.putInt(KEY_MAX_FLING_PAGES, mMaxFlingPages);
        fragArgs.putInt(KEY_LAYOUT_DIRECTION, mLayoutDirection);
        fragArgs.putBoolean(KEY_HAS_START_MARGIN, mHasStartMargin);
        fragArgs.putBoolean(KEY_HAS_END_MARGIN, mHasEndMargin);
        fragArgs.putBoolean(KEY_HAS_START_DECORATION, mHasStartDecoration);
        fragArgs.putBoolean(KEY_HAS_END_DECORATION, mHasEndDecoration);
        fragArgs.putBoolean(KEY_HAS_FIRST_LAST_MARGINS, mHasFirstLastMargins);
        fragArgs.putBoolean(KEY_HAS_FIRST_LAST_DECORATIONS, mHasFirstLastDecorations);
        fragArgs.putBoolean(KEY_IS_SNAP_ENABLED, mIsSnapEnabled);

        return fragArgs;
    }

    private void populateTable() {
        ((TextView) findViewById(R.id.scrollAxis))
            .setText(mScrollAxis == RecyclerView.HORIZONTAL
                         ? getString(R.string.horizontal)
                         : getString(R.string.vertical));
        ((TextView) findViewById(R.id.rows)).setText(String.valueOf(mRows));
        ((TextView) findViewById(R.id.columns)).setText(String.valueOf(mColumns));
        ((TextView) findViewById(R.id.maxFlingPages)).setText(String.valueOf(mMaxFlingPages));
        ((TextView) findViewById(R.id.layoutDirection))
            .setText(mLayoutDirection == RecyclerView.LAYOUT_DIRECTION_LTR
                         ? getString(R.string.direction_ltr)
                         : getString(R.string.direction_rtl));
        ((TextView) findViewById(R.id.isSnapEnabled))
            .setText(mIsSnapEnabled ? getString(R.string.yes) : getString(R.string.no));
    }

    void storeData() {
        SharedPreferences.Editor editor = this.getPreferences(MODE_PRIVATE).edit();

        editor.putInt(KEY_SCROLL_AXIS, mScrollAxis);
        editor.putInt(KEY_ROWS, mRows);
        editor.putInt(KEY_COLUMNS, mColumns);
        editor.putInt(KEY_MAX_FLING_PAGES, mMaxFlingPages);
        editor.putInt(KEY_LAYOUT_DIRECTION, mLayoutDirection);
        editor.putBoolean(KEY_HAS_START_MARGIN, mHasStartMargin);
        editor.putBoolean(KEY_HAS_END_MARGIN, mHasEndMargin);
        editor.putBoolean(KEY_HAS_START_DECORATION, mHasStartDecoration);
        editor.putBoolean(KEY_HAS_END_DECORATION, mHasEndDecoration);
        editor.putBoolean(KEY_HAS_FIRST_LAST_MARGINS, mHasFirstLastMargins);
        editor.putBoolean(KEY_HAS_FIRST_LAST_DECORATIONS, mHasFirstLastDecorations);
        editor.putBoolean(KEY_IS_SNAP_ENABLED, mIsSnapEnabled);
        editor.apply();
    }

    private void unpackBundle(Bundle bundle) {
        mScrollAxis = bundle.getInt(KEY_SCROLL_AXIS);
        mRows = bundle.getInt(KEY_ROWS);
        mColumns = bundle.getInt(KEY_COLUMNS);
        mMaxFlingPages = bundle.getInt(KEY_MAX_FLING_PAGES);
        mLayoutDirection = bundle.getInt(KEY_LAYOUT_DIRECTION);
        mHasStartMargin = bundle.getBoolean(KEY_HAS_START_MARGIN);
        mHasEndMargin = bundle.getBoolean(KEY_HAS_END_MARGIN);
        mHasStartDecoration = bundle.getBoolean(KEY_HAS_START_DECORATION);
        mHasEndDecoration = bundle.getBoolean(KEY_HAS_END_DECORATION);
        mHasFirstLastMargins = bundle.getBoolean(KEY_HAS_FIRST_LAST_MARGINS);
        mHasFirstLastDecorations = bundle.getBoolean(KEY_HAS_FIRST_LAST_DECORATIONS);
        mIsSnapEnabled = bundle.getBoolean(KEY_IS_SNAP_ENABLED);
    }

    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final LayoutInflater inflater = LayoutInflater.from(MainActivity.this);

        RecyclerViewAdapter() {
            setHasStableIds(true);
            mViewHolderCount = new int[mViewHolderCount.length];

            // Stop flashing when text is changes on views.
            ((SimpleItemAnimator) mRecyclerView.getItemAnimator())
                .setSupportsChangeAnimations(false);

            // These lines can be removed or the limits lowered, but the display may become
            // a little jerky with higher view counts especially when scrolling to the top
            // of the dataset. Smooth scrolling can cause excessive creation of view holders
            // with API 27. Other APIs may also exhibit this behavior but have not been tested.
            final int endViewEachType = (mSpans + 1) / 2;
            final int middleViewEachTyp = mRows * mColumns;
            final RecyclerView.RecycledViewPool pool = mRecyclerView.getRecycledViewPool();

            pool.setMaxRecycledViews(VIEW_IS_SKY_BLUE_FIRST, endViewEachType);
            pool.setMaxRecycledViews(VIEW_IS_SKY_BLUE_LAST, endViewEachType);
            pool.setMaxRecycledViews(VIEW_IS_SKY_BLUE_MIDDLE, middleViewEachTyp);
            pool.setMaxRecycledViews(VIEW_IS_VERMILLION_MIDDLE, middleViewEachTyp);
            pool.setMaxRecycledViews(VIEW_IS_SKY_BLUE_LAST, endViewEachType);
            pool.setMaxRecycledViews(VIEW_IS_SKY_BLUE_LAST, endViewEachType);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            TextView textView = (TextView) holder.itemView;
//            Log.i(TAG, "<<<<onBindViewHolder: " + position);

            StringBuilder sb = new StringBuilder(String.valueOf(position));
            if (position == mSnapTarget) {
                sb.append(" T");
            } else if (position == mSnappedPosition) {
                sb.append(" S");
            }
            textView.setText(sb.toString());
            // Set text color of potential target views to white.
            if (position % (mColumns * mRows) == 0) {
                textView.setTextColor(Color.WHITE);
            } else {
                textView.setTextColor(Color.BLACK);
            }
        }

        @Override
        public int getItemCount() {
            // Guarantee 20 pages of views
            return 20 * mRows * mColumns;
        }

        @Override
        public long getItemId(int position) {
            // Assume content of RecyclerView does not change positions - no adds, deletes, etc.
            return (long) position;
        }

        @Override
        public int getItemViewType(int position) {

            if (position < mSpans) {
                return (position % 2) == 0 ? VIEW_IS_SKY_BLUE_FIRST : VIEW_IS_VERMILLION_FIRST;
            }

            if (position >= (mRecyclerView.getAdapter().getItemCount() - mSpans)) {
                return (position % 2) == 0 ? VIEW_IS_SKY_BLUE_LAST : VIEW_IS_VERMILLION_LAST;
            }

            return position % 2 == 0 ? VIEW_IS_SKY_BLUE_MIDDLE : VIEW_IS_VERMILLION_MIDDLE;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ++mViewHolderCount[viewType];
            TextView textView = (TextView) inflater.inflate(android.R.layout.simple_list_item_1,
                                                            parent, false);
//            String msg =
//                String.format(Locale.US, "<<<<onCreateViewHolder(%d) count= %d", viewType,
//                              mViewHolderCount[viewType]);
//            Log.i(TAG, msg);
            textView.setPadding(0, 0, 0, 0);
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                textView, 6, 36, 2, TypedValue.COMPLEX_UNIT_SP);
            RecyclerView.ViewHolder vh = new RecyclerView.ViewHolder(textView) {
            };

            int startMargin = 0;
            int endMargin = 0;
            int backgroundResource = R.drawable.sky_blue_box;
            switch (viewType) {
                case VIEW_IS_VERMILLION_FIRST:
                    backgroundResource = R.drawable.vermillion_box;
                    // Fall through
                case VIEW_IS_SKY_BLUE_FIRST:
                    if (mHasFirstLastMargins && mHasStartMargin) {
                        startMargin = mDefaultMargin;
                    }
                    if (mHasEndMargin) {
                        endMargin = mDefaultMargin;
                    }
                    break;

                case VIEW_IS_VERMILLION_MIDDLE:
                    backgroundResource = R.drawable.vermillion_box;
                    // Fall through
                case VIEW_IS_SKY_BLUE_MIDDLE:
                    if (mHasEndMargin) {
                        endMargin = mDefaultMargin;
                    }
                    if (mHasStartMargin) {
                        startMargin = mDefaultMargin;
                    }
                    break;

                case VIEW_IS_VERMILLION_LAST:
                    backgroundResource = R.drawable.vermillion_box;
                    // Fall through
                case VIEW_IS_SKY_BLUE_LAST:
                    if (mHasFirstLastMargins && mHasEndMargin) {
                        endMargin = mDefaultMargin;
                    }
                    if (mHasStartMargin) {
                        startMargin = mDefaultMargin;
                    }
                    break;

                default:
                    break;
            }

            textView.setBackgroundResource(backgroundResource);
            textView.setGravity(Gravity.CENTER);

            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) textView.getLayoutParams();
            lp.height = mCellHeight;
            lp.width = mCellWidth;
            LinearLayoutManager lm = (LinearLayoutManager) mRecyclerView.getLayoutManager();
            if (mScrollAxis == RecyclerView.HORIZONTAL) {
                if (lm.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                    lp.setMargins(endMargin, 0, startMargin, 0);
                } else {
                    lp.setMargins(startMargin, 0, endMargin, 0);
                }
            } else {
                lp.setMargins(0, startMargin, 0, endMargin);

            }
            textView.setLayoutParams(lp);

            return vh;
        }
    }

    @SuppressWarnings("unused")
    private final static String TAG = "MainActivity";

    private static final float PREFERRED_ASPECT_RATIO = 1.0f / 2.0f;

    private static final int VIEW_IS_SKY_BLUE_FIRST = 0;
    private static final int VIEW_IS_VERMILLION_FIRST = 1;
    private static final int VIEW_IS_SKY_BLUE_MIDDLE = 2;
    private static final int VIEW_IS_VERMILLION_MIDDLE = 3;
    private static final int VIEW_IS_SKY_BLUE_LAST = 4;
    private static final int VIEW_IS_VERMILLION_LAST = 5;

    public static final String KEY_SCROLL_AXIS = "scrollAxis";
    public static final String KEY_ROWS = "rows";
    public static final String KEY_COLUMNS = "columns";
    public static final String KEY_MAX_FLING_PAGES = "maxFlingPages";
    public static final String KEY_LAYOUT_DIRECTION = "layoutDirection";
    public static final String KEY_HAS_START_MARGIN = "hasStartMargin";
    public static final String KEY_HAS_END_MARGIN = "hasEndMargin";
    public static final String KEY_HAS_START_DECORATION = "hasStartDecoration";
    public static final String KEY_HAS_END_DECORATION = "hasEndDecoration";
    public static final String KEY_HAS_FIRST_LAST_MARGINS = "hasFirstLastMargins";
    public static final String KEY_HAS_FIRST_LAST_DECORATIONS = "hasFirstLastDecorations";
    public static final String KEY_IS_SNAP_ENABLED = "isSnapEnabled";
}