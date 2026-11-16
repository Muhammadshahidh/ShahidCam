package com.shahid.cam.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.shahid.cam.MainActivity;
import com.shahid.cam.R;

/**
 * Clean White Claymorphism preview screen for the upcoming Video Studio tab (Editor / Soon).
 */
public class EditorComingSoonFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_editor_coming_soon, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View btnOpenMedia = view.findViewById(R.id.btn_editor_open_media);
        View btnBackHome = view.findViewById(R.id.btn_editor_back_home);

        if (btnOpenMedia != null) {
            btnOpenMedia.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchFragment(1, true);
                }
            });
        }
        if (btnBackHome != null) {
            btnBackHome.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchFragment(0, true);
                }
            });
        }
    }
}
