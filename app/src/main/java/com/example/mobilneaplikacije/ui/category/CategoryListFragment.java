package com.example.mobilneaplikacije.ui.category;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Category;
import com.example.mobilneaplikacije.data.repository.CategoryRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class CategoryListFragment extends Fragment {

    private RecyclerView recyclerCategories;
    private CategoryAdapter adapter;
    private CategoryRepository repo;

    public CategoryListFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_list, container, false);

        recyclerCategories = view.findViewById(R.id.recyclerCategories);
        recyclerCategories.setLayoutManager(new LinearLayoutManager(getContext()));

        repo = new CategoryRepository(requireContext());
        List<Category> categories = repo.getAllCategories();

        adapter = new CategoryAdapter(categories,
                category -> {
                    // Klik na postojeću kategoriju → edit
                    AddCategoryFragment fragment = AddCategoryFragment.newInstanceForEdit(category.getId());
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                },
                () -> {
                    // Klik na "Dodaj novu kategoriju"
                    AddCategoryFragment fragment = new AddCategoryFragment();
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                });

        recyclerCategories.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.updateData(repo.getAllCategories());
    }
}
