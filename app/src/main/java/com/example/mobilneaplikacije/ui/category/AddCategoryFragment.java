package com.example.mobilneaplikacije.ui.category;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Category;
import com.example.mobilneaplikacije.data.repository.CategoryRepository;

public class AddCategoryFragment extends Fragment implements ColorPickerDialogFragment.OnColorPickedListener {

    private static final String ARG_CATEGORY_ID = "category_id_hash";
    private String editCategoryIdHash = null;

    private EditText etName, etColorHidden;
    private View viewColorPreview;
    private Button btnSave, btnPickColor;
    private CategoryRepository repo;

    public AddCategoryFragment() {}

    public static AddCategoryFragment newInstanceForEdit(String categoryIdHash) {
        AddCategoryFragment f = new AddCategoryFragment();
        Bundle b = new Bundle();
        b.putString(ARG_CATEGORY_ID, categoryIdHash);
        f.setArguments(b);
        return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add_category, container, false);

        etName = v.findViewById(R.id.etCategoryName);
        etColorHidden = v.findViewById(R.id.etCategoryColor);
        viewColorPreview = v.findViewById(R.id.viewColorPreview);
        btnPickColor = v.findViewById(R.id.btnPickColor);
        btnSave = v.findViewById(R.id.btnSaveCategory);

        repo = new CategoryRepository(requireContext());

        if (getArguments()!=null && getArguments().containsKey(ARG_CATEGORY_ID)) {
            editCategoryIdHash = getArguments().getString(ARG_CATEGORY_ID);

            // Učitaj kategoriju iz Firestore-a
            repo.getCategoryById(editCategoryIdHash, new CategoryRepository.Callback<Category>() {
                @Override public void onSuccess(@Nullable Category c) {
                    if (c == null) return;
                    etName.setText(c.getName());
                    etColorHidden.setText(c.getColor());
                    applyPreview(c.getColor());
                }
                @Override public void onError(Exception e) {
                    toast("Greška pri učitavanju: " + e.getMessage());
                }
            });
        }

        btnPickColor.setOnClickListener(x ->
                new ColorPickerDialogFragment().show(getChildFragmentManager(), "colorPicker"));

        btnSave.setOnClickListener(x -> save());

        return v;
    }

    @Override
    public void onColorPicked(String hex) {
        etColorHidden.setText(hex);
        applyPreview(hex);
    }

    private void applyPreview(String hex) {
        try { viewColorPreview.setBackgroundColor(android.graphics.Color.parseColor(hex)); }
        catch (Exception e) { viewColorPreview.setBackgroundColor(0xFFCCCCCC); }
    }

    private void save() {
        String name = etName.getText().toString().trim();
        String color = etColorHidden.getText().toString().trim();
        if (name.isEmpty()) { toast("Unesi naziv"); return; }
        if (color.isEmpty()) { toast("Izaberi boju"); return; }

        if (editCategoryIdHash == null) {
            Category c = new Category();
            c.setName(name);
            c.setColor(color);

            repo.insertCategory(c, new CategoryRepository.Callback<String>() {
                @Override public void onSuccess(@Nullable String id) {
                    toast("Kategorija dodata");
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
                @Override public void onError(Exception e) {
                    toast("Greška: " + e.getMessage());
                }
            });

        } else {
            Category c = new Category();
            c.setIdHash(editCategoryIdHash);
            c.setName(name);
            c.setColor(color);

            repo.updateCategory(c, new CategoryRepository.Callback<Void>() {
                @Override public void onSuccess(@Nullable Void v) {
                    toast("Kategorija izmenjena");
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
                @Override public void onError(Exception e) {
                    toast("Greška: " + e.getMessage());
                }
            });
        }
    }

    private void toast(String m) { Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show(); }
}
