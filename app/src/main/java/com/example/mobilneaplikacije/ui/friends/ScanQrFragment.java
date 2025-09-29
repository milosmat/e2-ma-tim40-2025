package com.example.mobilneaplikacije.ui.friends;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.repository.FriendsRepository;
import com.google.zxing.Result;
import com.journeyapps.barcodescanner.CompoundBarcodeView;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;

public class ScanQrFragment extends Fragment {
    private CompoundBarcodeView barcodeView; // ovo startuje kameru
    private boolean handled = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_scan_qr, container, false);
        barcodeView = v.findViewById(R.id.barcode_view);
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (handled) return;
                String txt = result.getText();
                if (txt != null && txt.startsWith("uid:")) {
                    handled = true;
                    String foundUser = txt.substring(4);
                    new FriendsRepository().sendFriendRequest(foundUser, new FriendsRepository.Callback<Void>() {
                        @Override public void onSuccess(Void d) {
                            Toast.makeText(getContext(), "Zahtev poslat", Toast.LENGTH_SHORT).show();
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                        @Override public void onError(Exception e) {
                            Toast.makeText(getContext(), "Greska: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                    });
                }
            }
        });
        return v;
    }

    @Override public void onResume() { super.onResume(); if (barcodeView != null) barcodeView.resume(); }
    @Override public void onPause() { if (barcodeView != null) barcodeView.pause(); super.onPause(); }
}
