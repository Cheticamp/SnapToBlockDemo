package com.example.snaptoblockdemo;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;

public class SettingsFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private Context mContext;
    private SettingsComplete mCallback;
    private View mDialogView;

    public SettingsFragment() {
        // null constructor required by Android
    }

    public static SettingsFragment newInstance(Bundle fragArgs) {
        SettingsFragment settingsFragment = new SettingsFragment();
        settingsFragment.setArguments(fragArgs);
        return settingsFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mCallback = (SettingsComplete) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                                             + " must implement SettingsComplete");
        }
        mContext = context;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder;

        if (getResources().getBoolean(R.bool.show_settings_fullscreen)) {
            builder = new AlertDialog.Builder(
                mContext, android.R.style.Theme_Material_Light_NoActionBar);
        } else {
            builder = new AlertDialog.Builder(mContext);
        }

        return builder.setView(createView())
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, this)
            .setTitle(getString(R.string.settings))
            .create();
    }

    @SuppressWarnings("ConstantConditions")
    @SuppressLint("InflateParams")
    public View createView() {
        Bundle fragArgs = getArguments();

        int scrollAxis = fragArgs.getInt(MainActivity.KEY_SCROLL_AXIS, RecyclerView.HORIZONTAL);
        int rows = fragArgs.getInt(MainActivity.KEY_ROWS, 1);
        int columns = fragArgs.getInt(MainActivity.KEY_COLUMNS, 4);
        int maxFlingsPages = fragArgs.getInt(MainActivity.KEY_MAX_FLING_PAGES, 1);
        int rtlDirection =
            fragArgs.getInt(MainActivity.KEY_LAYOUT_DIRECTION, RecyclerView.LAYOUT_DIRECTION_LTR);
        boolean hasStartMargin = fragArgs.getBoolean(MainActivity.KEY_HAS_START_MARGIN, false);
        boolean hasEndMargin = fragArgs.getBoolean(MainActivity.KEY_HAS_END_MARGIN, false);
        boolean hasStartDecoration =
            fragArgs.getBoolean(MainActivity.KEY_HAS_START_DECORATION, false);
        boolean hasEndDecoration = fragArgs.getBoolean(MainActivity.KEY_HAS_END_DECORATION, false);
        boolean hasFirstLastMargins =
            fragArgs.getBoolean(MainActivity.KEY_HAS_FIRST_LAST_MARGINS, false);
        boolean hasFirstLastDecorations =
            fragArgs.getBoolean(MainActivity.KEY_HAS_FIRST_LAST_DECORATIONS, false);
        boolean isSnapEnabled = fragArgs.getBoolean(MainActivity.KEY_IS_SNAP_ENABLED, true);

        mDialogView = getActivity().getLayoutInflater().inflate(R.layout.settings, null);

        int radioBtnToCheck = (scrollAxis == RecyclerView.HORIZONTAL)
            ? R.id.rbHorizontal
            : R.id.rbVerticalAxis;
        ((RadioButton) mDialogView.findViewById(radioBtnToCheck)).setChecked(true);

        NumberPicker np = mDialogView.findViewById(R.id.rowsPicker);
        np.setMinValue(1);
        np.setMaxValue(10);
        np.setValue(rows);

        np = mDialogView.findViewById(R.id.colsPicker);
        np.setMinValue(1);
        np.setMaxValue(10);
        np.setValue(columns);

        np = mDialogView.findViewById(R.id.maxFlingPagesPicker);
        np.setMinValue(1);
        np.setMaxValue(10);
        np.setValue(maxFlingsPages);

        Switch rtlSwitch = mDialogView.findViewById(R.id.layoutDirectionSwitch);
        rtlSwitch.setChecked(rtlDirection == RecyclerView.LAYOUT_DIRECTION_RTL);

        CheckBox cb = mDialogView.findViewById(R.id.doMarginStart);
        cb.setChecked(hasStartMargin);

        cb = mDialogView.findViewById(R.id.doMarginEnd);
        cb.setChecked(hasEndMargin);

        cb = mDialogView.findViewById(R.id.doDecorationStart);
        cb.setChecked(hasStartDecoration);

        cb = mDialogView.findViewById(R.id.doDecorationEnd);
        cb.setChecked(hasEndDecoration);

        cb = mDialogView.findViewById(R.id.doFirstLastMargins);
        cb.setChecked(hasFirstLastMargins);

        cb = mDialogView.findViewById(R.id.doFirstLastDecorations);
        cb.setChecked(hasFirstLastDecorations);

        cb = mDialogView.findViewById(R.id.isSnapEnabled);
        cb.setChecked(isSnapEnabled);

        return mDialogView;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_NEGATIVE) {
            dialog.cancel();
            return;
        }

        // Else the "OK" button.
        Bundle response = new Bundle();

        response.putInt(MainActivity.KEY_SCROLL_AXIS,
                        (((RadioGroup) mDialogView.findViewById(R.id.rgScrollingAxis))
                            .getCheckedRadioButtonId() == R.id.rbHorizontal)
                            ? RecyclerView.HORIZONTAL
                            : RecyclerView.VERTICAL);

        response.putInt(MainActivity.KEY_ROWS, ((NumberPicker) mDialogView
            .findViewById(R.id.rowsPicker)).getValue());
        response.putInt(MainActivity.KEY_COLUMNS, ((NumberPicker) mDialogView
            .findViewById(R.id.colsPicker)).getValue());
        response.putInt(MainActivity.KEY_MAX_FLING_PAGES, ((NumberPicker) mDialogView
            .findViewById(R.id.maxFlingPagesPicker)).getValue());
        response.putInt(MainActivity.KEY_LAYOUT_DIRECTION,
                        ((Switch) mDialogView.findViewById(R.id.layoutDirectionSwitch)).isChecked()
                            ? RecyclerView.LAYOUT_DIRECTION_RTL :
                            RecyclerView.LAYOUT_DIRECTION_LTR);
        response.putBoolean(MainActivity.KEY_HAS_START_MARGIN, ((CheckBox) mDialogView
            .findViewById(R.id.doMarginStart)).isChecked());
        response.putBoolean(MainActivity.KEY_HAS_END_MARGIN, ((CheckBox) mDialogView
            .findViewById(R.id.doMarginEnd)).isChecked());
        response.putBoolean(MainActivity.KEY_HAS_START_DECORATION, ((CheckBox) mDialogView
            .findViewById(R.id.doDecorationStart)).isChecked());
        response.putBoolean(MainActivity.KEY_HAS_END_DECORATION, ((CheckBox) mDialogView
            .findViewById(R.id.doDecorationEnd)).isChecked());
        response.putBoolean(MainActivity.KEY_HAS_FIRST_LAST_MARGINS, ((CheckBox) mDialogView
            .findViewById(R.id.doFirstLastMargins)).isChecked());
        response.putBoolean(MainActivity.KEY_HAS_FIRST_LAST_DECORATIONS, ((CheckBox) mDialogView
            .findViewById(R.id.doFirstLastDecorations)).isChecked());
        response.putBoolean(MainActivity.KEY_IS_SNAP_ENABLED, ((CheckBox) mDialogView
            .findViewById(R.id.isSnapEnabled)).isChecked());
        mCallback.onSettingsComplete(response);
        dialog.dismiss();
    }

    public static final String TAG = "SettingsFragment";

}

interface SettingsComplete {
    void onSettingsComplete(Bundle response);
}
