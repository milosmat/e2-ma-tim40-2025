package com.example.mobilneaplikacije.ui.category;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilneaplikacije.R;

public class ColorPickerDialogFragment extends DialogFragment {

    public interface OnColorPickedListener {
        void onColorPicked(String hex);
    }

    private OnColorPickedListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof OnColorPickedListener) {
            listener = (OnColorPickedListener) getParentFragment();
        } else if (context instanceof OnColorPickedListener) {
            listener = (OnColorPickedListener) context;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_color_picker, null, false);
        RecyclerView rv = view.findViewById(R.id.recyclerColors);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 6));

        String[] hexColors = getResources().getStringArray(R.array.category_color_hex);
        rv.setAdapter(new ColorAdapter(hexColors, hex -> {
            if (listener != null) listener.onColorPicked(hex);
            dismiss();
        }));

        return new AlertDialog.Builder(requireContext())
                .setTitle("Izaberi boju")
                .setView(view)
                .setNegativeButton("Otkaži", (d, w) -> dismiss())
                .create();
    }

    static class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.VH> {
        private final String[] colors;
        private final OnClick onClick;

        interface OnClick { void pick(String hex); }

        ColorAdapter(String[] colors, OnClick onClick) {
            this.colors = colors;
            this.onClick = onClick;
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color_swatch, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) { h.bind(colors[pos], onClick); }
        @Override public int getItemCount() { return colors.length; }

        static class VH extends RecyclerView.ViewHolder {
            final View swatch;
            final ImageView check;

            VH(@NonNull View itemView) {
                super(itemView);
                swatch = itemView.findViewById(R.id.viewSwatch);
                check = itemView.findViewById(R.id.ivCheck);
            }
            void bind(String hex, OnClick onClick) {
                try {
                    swatch.getBackground().setTint(android.graphics.Color.parseColor(hex));
                } catch (Exception ignored) { swatch.getBackground().setTint(0xFFCCCCCC); }
                itemView.setOnClickListener(v -> onClick.pick(hex));
            }
        }
    }
}
