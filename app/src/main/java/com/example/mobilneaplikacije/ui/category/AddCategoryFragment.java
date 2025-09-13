package com.example.mobilneaplikacije.ui.category;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Category;
import com.example.mobilneaplikacije.data.repository.CategoryRepository;

public class AddCategoryFragment extends Fragment {

    private static final String ARG_CATEGORY_ID = "category_id";
    private long editCategoryId = -1;

    private EditText etName, etColor;
    private Button btnSave;
    private CategoryRepository repo;

    public AddCategoryFragment() {}

    public static AddCategoryFragment newInstanceForEdit(long categoryId) {
        AddCategoryFragment fragment = new AddCategoryFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CATEGORY_ID, categoryId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_category, container, false);

        etName = view.findViewById(R.id.etCategoryName);
        etColor = view.findViewById(R.id.etCategoryColor);
        btnSave = view.findViewById(R.id.btnSaveCategory);
        repo = new CategoryRepository(requireContext());

        if (getArguments() != null && getArguments().containsKey(ARG_CATEGORY_ID)) {
            editCategoryId = getArguments().getLong(ARG_CATEGORY_ID);
            loadCategoryForEdit(editCategoryId);
        }

        btnSave.setOnClickListener(v -> saveCategory());

        return view;
    }

    private void loadCategoryForEdit(long id) {
        for (Category c : repo.getAllCategories()) {
            if (c.getId() == id) {
                etName.setText(c.getName());
                etColor.setText(c.getColor());
                break;
            }
        }
    }

    private void saveCategory() {
        String name = etName.getText().toString().trim();
        String color = etColor.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getContext(), "Unesi naziv kategorije!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(color)) {
            Toast.makeText(getContext(), "Unesi boju (npr. #FF0000)!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!color.startsWith("#") || (color.length() != 7 && color.length() != 9)) {
            Toast.makeText(getContext(), "Boja mora biti u HEX formatu (#RRGGBB)!", Toast.LENGTH_SHORT).show();
            return;
        }

        // zabrani duplikate boje
        for (Category c : repo.getAllCategories()) {
            if (c.getColor().equalsIgnoreCase(color) && c.getId() != editCategoryId) {
                Toast.makeText(getContext(), "Boja je već zauzeta!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (editCategoryId == -1) {
            // novi
            Category cat = new Category();
            cat.setName(name);
            cat.setColor(color);
            repo.insertCategory(cat);
            Toast.makeText(getContext(), "Kategorija dodata!", Toast.LENGTH_SHORT).show();
        } else {
            // update
            Category cat = new Category();
            cat.setId(editCategoryId);
            cat.setName(name);
            cat.setColor(color);
            repo.updateCategory(cat);
            Toast.makeText(getContext(), "Kategorija izmenjena!", Toast.LENGTH_SHORT).show();
        }

        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
