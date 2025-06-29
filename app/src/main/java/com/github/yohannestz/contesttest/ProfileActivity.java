package com.github.yohannestz.contesttest;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {

    private static final int[] STICKER_ANGLES = {0, 30, 330, 180, 150, 210};
    private static final int STICKER_SIZE_DP = 32;
    private static final int STICKER_RADIUS_DP = 70;
    private static final int[] STICKER_RES_IDS = {
            R.raw.sticker_one, R.raw.sticker_two, R.raw.sticker_three,
            R.raw.sticker_four, R.raw.sticker_five, R.raw.sticker_six
    };

    private final List<StickerInfo> stickerInfos = new ArrayList<>(6);
    private LinearLayout collapsedHeader, buttonRow, nameContainerView;
    private TextView collapsedName, collapsedStatus, nameView, statusView;
    private boolean isHeaderCollapsed = false;
    private final int[] originalNameLocation = new int[2];
    private final int[] originalStatusLocation = new int[2];
    private ScrollView scrollView;
    private ShapeableImageView profilePicture;
    private ImageView nameStickerView, collapsedNameStickerView;
    private int originalButtonRowHeight = -1;
    private ViewTreeObserver.OnScrollChangedListener scrollListener;

    private static final int GEM_COUNT = 20;
    private static final float GEM_MIN_RADIUS_DP = 60f;
    private static final float GEM_MAX_RADIUS_DP = 100f;
    private final List<StickerInfo> gemInfos = new ArrayList<>(GEM_COUNT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupTabs();
        setupRecyclerView();
        setupStickersAndAnimations();
    }

    private void initializeViews() {
        scrollView = findViewById(R.id.scroll_view);
        profilePicture = findViewById(R.id.profile_picture);
        nameView = findViewById(R.id.name);
        statusView = findViewById(R.id.status);
        collapsedHeader = findViewById(R.id.collapsed_header);
        collapsedName = findViewById(R.id.collapsed_name);
        collapsedStatus = findViewById(R.id.collapsed_status);
        nameStickerView = findViewById(R.id.nameStickerView);
        buttonRow = findViewById(R.id.button_row);
        nameContainerView = findViewById(R.id.nameContainer);
        collapsedNameStickerView = findViewById(R.id.collapsed_name_imageview);

        collapsedName.setText(nameView.getText());
        collapsedStatus.setText(statusView.getText());
    }

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        String[] tabs = {"Posts", "Media", "Files", "Links", "Voice", "Music", "GIFs", "Groups"};

        for (String tabName : tabs) {
            TabLayout.Tab tab = tabLayout.newTab();
            tab.setText(tabName);
            tabLayout.addTab(tab);
        }
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.post_grid);

        List<Integer> imageList = Arrays.asList(
                R.drawable.post_image_1, R.drawable.post_image_2, R.drawable.post_image_4,
                R.drawable.post_image_2, R.drawable.post_image_1, R.drawable.post_image_3,
                R.drawable.post_image_1, R.drawable.post_image_5, R.drawable.post_image_1
        );

        recyclerView.setAdapter(new PostAdapter(imageList));
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
    }

    private void setupStickersAndAnimations() {
        ConstraintLayout container = (ConstraintLayout) profilePicture.getParent();

        profilePicture.post(() -> {
            nameContainerView.getLocationOnScreen(originalNameLocation);
            statusView.getLocationOnScreen(originalStatusLocation);

            addStickers(container, profilePicture);
            //addGems(container, profilePicture);
            setupScrollAnimations();
        });

        setNameSticker(nameStickerView);
        setNameSticker(collapsedNameStickerView);
    }

    private void setupScrollAnimations() {
        // Remove previous listener if exists
        if (scrollListener != null) {
            scrollView.getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
        }

        scrollListener = () -> {
            float scrollY = scrollView.getScrollY();
            float collapseRange = profilePicture.getTop();
            float progress = Math.min(scrollY / collapseRange, 1.0f);

            if (originalButtonRowHeight == -1) {
                originalButtonRowHeight = buttonRow.getHeight();
            }

            // Only update layout if height changes significantly
            int newHeight = (int) (originalButtonRowHeight * (1 - progress));
            if (Math.abs(buttonRow.getHeight() - newHeight) > 2) {
                buttonRow.getLayoutParams().height = newHeight;
                buttonRow.requestLayout();
            }

            // Only update alpha if change is significant
            float newAlpha = 1 - progress;
            if (Math.abs(buttonRow.getAlpha() - newAlpha) > 0.01f) {
                buttonRow.setAlpha(newAlpha);
            }

            // Visibility changes only when crossing thresholds
            if (progress >= 0.8f && buttonRow.getVisibility() != View.INVISIBLE) {
                buttonRow.setVisibility(View.INVISIBLE);
            } else if (progress < 0.8f && buttonRow.getVisibility() != View.VISIBLE) {
                buttonRow.setVisibility(View.VISIBLE);
            }

            animateProfileAndStickers(progress);

            if (progress > 0.3f && progress < 0.8f) {
                float transitionProgress = (progress - 0.3f) / 0.5f;
                animateNameAndStatusTransition(transitionProgress);
            } else {
                handleHeaderCollapseState(progress);
            }
        };

        scrollView.getViewTreeObserver().addOnScrollChangedListener(scrollListener);
    }

   /* private void animateProfileAndStickers(float progress) {
        float scale = 1f - progress;

        if (Math.abs(profilePicture.getScaleX() - scale) > 0.01f) {
            profilePicture.setScaleX(scale);
            profilePicture.setScaleY(scale);
            profilePicture.setAlpha(scale);
        }

        float centerX = profilePicture.getX() + profilePicture.getWidth() / 2f;
        float centerY = profilePicture.getY() + profilePicture.getHeight() / 2f;

        for (StickerInfo info : stickerInfos) {
            float dx = centerX - info.originalX;
            float dy = centerY - info.originalY;

            float suctionFactor = (float) Math.pow(progress, 1.5); // starts slow, pulls fast

            float newX = info.originalX + dx * suctionFactor;
            float newY = info.originalY + dy * suctionFactor;

            float stickerScale = 1f - suctionFactor * 0.7f;

            if (Math.abs(info.view.getX() - newX) > 0.5f || Math.abs(info.view.getY() - newY) > 0.5f) {
                info.view.setX(newX);
                info.view.setY(newY);
            }

            if (Math.abs(info.view.getScaleX() - stickerScale) > 0.01f) {
                info.view.setScaleX(stickerScale);
                info.view.setScaleY(stickerScale);
                info.view.setAlpha(stickerScale);
            }
        }
    }

    private void animateProfileAndStickers(float progress) {
        float scale = 1f - progress;

        if (Math.abs(profilePicture.getScaleX() - scale) > 0.01f) {
            profilePicture.setScaleX(scale);
            profilePicture.setScaleY(scale);
            profilePicture.setAlpha(scale);
        }

        float centerX = profilePicture.getX() + profilePicture.getWidth() / 2f;
        float centerY = profilePicture.getY() + profilePicture.getHeight() / 2f;

        List<StickerInfo> all = new ArrayList<>();
        all.addAll(stickerInfos);
        all.addAll(gemInfos);

        for (StickerInfo info : all) {
            float dx = centerX - info.originalX;
            float dy = centerY - info.originalY;

            float suctionFactor = (float) Math.pow(progress, 1.5); // faster pull-in

            float newX = info.originalX + dx * suctionFactor;
            float newY = info.originalY + dy * suctionFactor;

            float stickerScale = 1f - suctionFactor * 0.7f;

            if (Math.abs(info.view.getX() - newX) > 0.5f || Math.abs(info.view.getY() - newY) > 0.5f) {
                info.view.setX(newX);
                info.view.setY(newY);
            }

            if (Math.abs(info.view.getScaleX() - stickerScale) > 0.01f) {
                info.view.setScaleX(stickerScale);
                info.view.setScaleY(stickerScale);
                info.view.setAlpha(stickerScale);
            }
        }
    } */

    private void animateProfileAndStickers(float progress) {
        float profileScale = 1f - progress;
        if (Math.abs(profilePicture.getScaleX() - profileScale) > 0.01f) {
            profilePicture.setScaleX(profileScale);
            profilePicture.setScaleY(profileScale);
            profilePicture.setAlpha(profileScale);
        }

        float centerX = profilePicture.getX() + profilePicture.getWidth() / 2f;
        float centerY = profilePicture.getY() + profilePicture.getHeight() / 2f;

        // This curve makes sure suction finishes before profile scale reaches 0.3
        float suctionProgress = Math.min(progress / 0.7f, 1f);

        List<StickerInfo> all = new ArrayList<>();
        all.addAll(stickerInfos);
        all.addAll(gemInfos);

        for (StickerInfo info : all) {
            float dx = centerX - info.originalX;
            float dy = centerY - info.originalY;

            float suctionFactor = (float) Math.pow(suctionProgress, 1.5f);

            float newX = info.originalX + dx * suctionFactor;
            float newY = info.originalY + dy * suctionFactor;

            float stickerScale = 1f - suctionFactor * 0.7f;

            if (Math.abs(info.view.getX() - newX) > 0.5f || Math.abs(info.view.getY() - newY) > 0.5f) {
                info.view.setX(newX);
                info.view.setY(newY);
            }

            if (Math.abs(info.view.getScaleX() - stickerScale) > 0.01f) {
                info.view.setScaleX(stickerScale);
                info.view.setScaleY(stickerScale);
                info.view.setAlpha(stickerScale);
            }
        }
    }

    private void animateNameAndStatusTransition(float progress) {
        int[] fromNameLoc = new int[2];
        int[] toNameLoc = new int[2];
        nameContainerView.getLocationOnScreen(fromNameLoc);
        collapsedName.getLocationOnScreen(toNameLoc);

        float nameDx = toNameLoc[0] - fromNameLoc[0];
        float nameDy = toNameLoc[1] - fromNameLoc[1];

        // Only update if change is significant
        if (Math.abs(nameContainerView.getTranslationX() - nameDx * progress) > 0.5f ||
                Math.abs(nameContainerView.getTranslationY() - nameDy * progress) > 0.5f) {
            nameContainerView.setTranslationX(nameDx * progress);
            nameContainerView.setTranslationY(nameDy * progress);
        }

        float newScale = 1 - 0.2f * progress;
        if (Math.abs(nameContainerView.getScaleX() - newScale) > 0.01f) {
            nameContainerView.setScaleX(newScale);
            nameContainerView.setScaleY(newScale);
        }

        // Status animation (unchanged from previous)
        int[] fromStatusLoc = new int[2];
        int[] toStatusLoc = new int[2];
        statusView.getLocationOnScreen(fromStatusLoc);
        collapsedStatus.getLocationOnScreen(toStatusLoc);

        float statusDx = toStatusLoc[0] - fromStatusLoc[0];
        float statusDy = toStatusLoc[1] - fromStatusLoc[1];

        statusView.setTranslationX(statusDx * progress);
        statusView.setTranslationY(statusDy * progress);
        statusView.setScaleX(1 - 0.3f * progress);
        statusView.setScaleY(1 - 0.3f * progress);

        collapsedHeader.setAlpha(0f);
        collapsedHeader.setVisibility(View.INVISIBLE);
    }

    private void handleHeaderCollapseState(float progress) {
        if (progress >= 0.8f) {
            if (!isHeaderCollapsed) {
                isHeaderCollapsed = true;
                animateHeaderCollapse();
            }
        } else {
            if (isHeaderCollapsed) {
                isHeaderCollapsed = false;
                animateHeaderExpand();
            }
            resetHeaderViewsPosition();
        }
    }

    private void animateHeaderCollapse() {
        nameContainerView.animate()
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> nameContainerView.setVisibility(View.INVISIBLE))
                .start();

        statusView.animate()
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> statusView.setVisibility(View.INVISIBLE))
                .start();

        collapsedHeader.setAlpha(0f);
        collapsedHeader.setVisibility(View.VISIBLE);
        collapsedHeader.animate()
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void animateHeaderExpand() {
        nameContainerView.setVisibility(View.VISIBLE);
        statusView.setVisibility(View.VISIBLE);
        nameContainerView.setAlpha(0f);
        statusView.setAlpha(0f);

        nameContainerView.animate().alpha(1f).setDuration(200).start();
        statusView.animate().alpha(1f).setDuration(200).start();

        collapsedHeader.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> collapsedHeader.setVisibility(View.INVISIBLE))
                .start();
    }

    private void resetHeaderViewsPosition() {
        nameContainerView.setTranslationX(0);
        nameContainerView.setTranslationY(0);
        nameContainerView.setScaleX(1f);
        nameContainerView.setScaleY(1f);

        statusView.setTranslationX(0);
        statusView.setTranslationY(0);
        statusView.setScaleX(1f);
        statusView.setScaleY(1f);
    }

    private void setNameSticker(ImageView nameStickerView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            new Thread(() -> {
                try (InputStream is = getResources().openRawResource(R.raw.sticker_profile)) {
                    final Drawable drawable = Drawable.createFromStream(is, null);
                    runOnUiThread(() -> {
                        nameStickerView.setImageDrawable(drawable);
                        if (drawable instanceof AnimatedImageDrawable) {
                            ((AnimatedImageDrawable) drawable).start();
                        }
                    });
                } catch (IOException e) {
                    runOnUiThread(() -> nameStickerView.setImageResource(R.raw.sticker_profile));
                }
            }).start();
        } else {
            nameStickerView.setImageResource(R.raw.sticker_profile);
        }
    }

    private void addGems(ConstraintLayout container, View centerView) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            float minRadiusPx = dpToPx(GEM_MIN_RADIUS_DP);
            float maxRadiusPx = dpToPx(GEM_MAX_RADIUS_DP);
            int sizePx = (int) dpToPx(20f); // small gem size

            int centerX = (int) (centerView.getX() + centerView.getWidth() / 2f);
            int centerY = (int) (centerView.getY() + centerView.getHeight() / 2f);

            Random random = new Random();
            List<Runnable> uiTasks = new ArrayList<>(GEM_COUNT);

            for (int i = 0; i < GEM_COUNT; i++) {
                float radius = minRadiusPx + random.nextFloat() * (maxRadiusPx - minRadiusPx);
                double angle = Math.toRadians(random.nextInt(360));

                float dx = (float) (radius * Math.cos(angle));
                float dy = (float) (radius * Math.sin(angle));
                float x = centerX + dx - sizePx / 2f;
                float y = centerY + dy - sizePx / 2f;

                float distanceFromCenter = (float) Math.sqrt(dx * dx + dy * dy);

                uiTasks.add(() -> {
                    ImageView gemView = new ImageView(this);
                    ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(sizePx, sizePx);
                    gemView.setLayoutParams(params);
                    gemView.setX(x);
                    gemView.setY(y);
                    gemView.setZ(5); // behind stickers
                    gemView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    gemView.setImageResource(R.drawable.ic_gem);
                    gemView.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
                    gemView.setAlpha(0.2f); // 20% opacity

                    container.addView(gemView);
                    gemInfos.add(new StickerInfo(gemView, x, y, distanceFromCenter));
                });
            }

            runOnUiThread(() -> uiTasks.forEach(Runnable::run));
            executor.shutdown();
        });
    }


    private void addStickers(ConstraintLayout container, View centerView) {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int i = 0; i < STICKER_ANGLES.length; i++) {
            final int angle = STICKER_ANGLES[i];
            final int resId = STICKER_RES_IDS[i % STICKER_RES_IDS.length];

            executor.execute(() -> {
                float radiusForThisSticker = (angle == 0 || angle == 180) ? STICKER_RADIUS_DP * 1.6f : STICKER_RADIUS_DP;
                float radiusPx = dpToPx(radiusForThisSticker);
                int sizePx = (int) dpToPx(STICKER_SIZE_DP);

                int centerX = (int) (centerView.getX() + centerView.getWidth() / 2f);
                int centerY = (int) (centerView.getY() + centerView.getHeight() / 2f);

                double radians = Math.toRadians(angle);
                final float x = (float) (centerX + radiusPx * Math.cos(radians)) - sizePx / 2f;
                final float y = (float) (centerY + radiusPx * Math.sin(radians)) - sizePx / 2f;

                runOnUiThread(() -> {
                    ImageView stickerView = new ImageView(this);
                    ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(sizePx, sizePx);
                    stickerView.setLayoutParams(params);
                    stickerView.setX(x);
                    stickerView.setY(y);
                    stickerView.setZ(10);
                    stickerView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        new Thread(() -> {
                            try (InputStream is = getResources().openRawResource(resId)) {
                                final Drawable drawable = Drawable.createFromStream(is, null);
                                runOnUiThread(() -> {
                                    stickerView.setImageDrawable(drawable);
                                    if (drawable instanceof AnimatedImageDrawable) {
                                        ((AnimatedImageDrawable) drawable).start();
                                    }
                                });
                            } catch (IOException e) {
                                runOnUiThread(() -> stickerView.setImageResource(resId));
                            }
                        }).start();
                    } else {
                        stickerView.setImageResource(resId);
                    }

                    container.addView(stickerView);
                    stickerInfos.add(new StickerInfo(stickerView, x, y));
                });
            });
        }
        executor.shutdown();
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scrollView != null && scrollListener != null) {
            scrollView.getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
        }
    }

    private static class StickerInfo {
        final View view;
        final float originalX, originalY;
        final float distanceFromCenter; // NEW

        StickerInfo(View view, float x, float y) {
            this.view = view;
            this.originalX = x;
            this.originalY = y;
            this.distanceFromCenter = 0f;
        }

        StickerInfo(View view, float x, float y, float distanceFromCenter) {
            this.view = view;
            this.originalX = x;
            this.originalY = y;
            this.distanceFromCenter = distanceFromCenter;
        }
    }
}