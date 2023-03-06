package com.grassroot.academy.whatsnew;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.grassroot.academy.R;
import com.grassroot.academy.base.BaseFragment;
import com.grassroot.academy.databinding.FragmentWhatsNewItemBinding;
import com.grassroot.academy.model.whatsnew.WhatsNewItemModel;
import com.grassroot.academy.util.UiUtils;

public class WhatsNewItemFragment extends BaseFragment {
    public static final String ARG_MODEL = "ARG_MODEL";

    private FragmentWhatsNewItemBinding binding;

    public static WhatsNewItemFragment newInstance(@NonNull WhatsNewItemModel model) {
        final WhatsNewItemFragment fragment = new WhatsNewItemFragment();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_MODEL, model);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWhatsNewItemBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Bundle args = getArguments();
        final WhatsNewItemModel model = args.getParcelable(ARG_MODEL);

        binding.title.setText(escapePlatformName(model.getTitle()));
        binding.message.setText(escapePlatformName(model.getMessage()));
        binding.message.setMovementMethod(new ScrollingMovementMethod());

        @DrawableRes final int imageRes = UiUtils.INSTANCE.getDrawable(requireContext(), model.getImage());
        binding.image.setImageResource(imageRes);
        // We need different scale types for portrait and landscape images
        final Drawable drawable = UiUtils.INSTANCE.getDrawable(requireContext(), imageRes);
        if (drawable != null) {
            if (drawable.getIntrinsicHeight() > drawable.getIntrinsicWidth()) {
                binding.image.setScaleType(AppCompatImageView.ScaleType.FIT_END);
            } else {
                binding.image.setScaleType(AppCompatImageView.ScaleType.FIT_CENTER);
            }
        }
    }

    private String escapePlatformName(@NonNull String input) {
        return input.replaceAll("platform_name", getString(R.string.platform_name));
    }
}
