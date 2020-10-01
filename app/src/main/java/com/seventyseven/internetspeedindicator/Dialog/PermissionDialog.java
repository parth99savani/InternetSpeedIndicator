package com.seventyseven.internetspeedindicator.Dialog;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.seventyseven.internetspeedindicator.R;
import com.seventyseven.internetspeedindicator.SettingsActivity;

public class PermissionDialog extends BottomSheetDialogFragment implements View.OnClickListener {

    public static PermissionDialog newInstance() {
        PermissionDialog fragment = new PermissionDialog();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        View contentView = View.inflate(getContext(), R.layout.dialog_permission, null);
        dialog.setContentView(contentView);
        ((View) contentView.getParent()).setBackgroundColor(getResources().getColor(android.R.color.transparent));
        Button allowButton = (Button) contentView.findViewById(R.id.btnAllow);
        allowButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        startActivity(new Intent(getActivity(), SettingsActivity.class));
        dismiss();
    }
}
