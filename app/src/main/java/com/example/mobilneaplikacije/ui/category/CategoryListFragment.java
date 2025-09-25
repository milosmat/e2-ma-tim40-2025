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

import java.util.ArrayList;
import java.util.List;

public class CategoryListFragment extends Fragment {

    private RecyclerView recyclerCategories;
    private CategoryAdapter adapter;
    private CategoryRepository repo;
    private final List<Category> inMemory = new ArrayList<>();

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

        adapter = new CategoryAdapter(
                inMemory,
                category -> {
                    // Edit – sada koristimo idHash
                    AddCategoryFragment fragment = AddCategoryFragment.newInstanceForEdit(category.getIdHash());
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                },
                () -> {
                    // Dodaj novu
                    AddCategoryFragment fragment = new AddCategoryFragment();
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                });

        recyclerCategories.setAdapter(adapter);

        loadCategories();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCategories();
    }

    private void loadCategories() {
        repo.getAllCategories(new CategoryRepository.Callback<List<Category>>() {
            @Override public void onSuccess(List<Category> data) {
                inMemory.clear();
                if (data != null) inMemory.addAll(data);
                adapter.updateData(inMemory);
            }
            @Override public void onError(Exception e) {
                Toast.makeText(getContext(), "Greška pri učitavanju: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
