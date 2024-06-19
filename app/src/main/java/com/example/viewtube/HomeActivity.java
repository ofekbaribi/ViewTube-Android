package com.example.viewtube;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.viewtube.managers.CurrentUserManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements VideoList.VideoItemClickListener {

    private static final String TAG = "HomeActivity";

    // UI components
    private RecyclerView videoRecyclerView;
    private VideoList videoList;
    private ImageView searchButton;
    private TextInputEditText searchBar;
    private BottomNavigationView bottomNavBar;
    private TextInputLayout searchInputLayout;
    private NavigationView sideBar;
    private ImageView logoImageView;
    private Uri profilePicture;
    private SharedPreferences sharedPreferences;
    private VideoViewModel videoViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved theme preferences
        sharedPreferences = getSharedPreferences("theme_preferences", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize UI components
        sideBar = findViewById(R.id.navigation_view);
        searchButton = findViewById(R.id.search_button);
        videoRecyclerView = findViewById(R.id.video_feed_recycler_view);
        searchBar = findViewById(R.id.search_bar);
        bottomNavBar = findViewById(R.id.bottomNavigationView);
        searchInputLayout = findViewById(R.id.search_input_layout);
        logoImageView = findViewById(R.id.youtube_logo);

        // Initialize ViewModel
        videoViewModel = new ViewModelProvider.AndroidViewModelFactory(getApplication()).create(VideoViewModel.class);
        videoViewModel.getVideoItems().observe(this, videoItems -> {
            videoList.setVideoItems(videoItems);
        });

        // Initialize side bar header views
        View headerView = sideBar.getHeaderView(0);
        ImageView profileImageView = headerView.findViewById(R.id.current_profile);
        TextView usernameView = headerView.findViewById(R.id.current_user);

        // Check if there is a current user and update the UI accordingly
        if (CurrentUserManager.getInstance().getCurrentUser() == null) {
            bottomNavBar.getMenu().findItem(R.id.nav_login).setVisible(true);
            bottomNavBar.getMenu().findItem(R.id.nav_upload).setVisible(false);
            profileImageView.setImageResource(R.drawable.ic_profile_foreground);
            usernameView.setText("Guest");
        } else {
            bottomNavBar.getMenu().findItem(R.id.nav_login).setVisible(false);
            bottomNavBar.getMenu().findItem(R.id.nav_upload).setVisible(true);
            String profilePictureUriString = CurrentUserManager.getInstance().getCurrentUser().getProfilePictureUri();
            if (profilePictureUriString != null && !profilePictureUriString.isEmpty()) {
                profilePicture = Uri.parse(profilePictureUriString);
                profileImageView.setImageURI(profilePicture); // Set profile image using URI
            } else {
                profileImageView.setImageResource(R.drawable.ic_profile_foreground); // Set default profile image
            }
            usernameView.setText(CurrentUserManager.getInstance().getCurrentUser().getUsername());
        }

        // Setup RecyclerView
        videoList = new VideoList(this, this); // Pass the context and the listener
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        videoRecyclerView.setLayoutManager(layoutManager);
        videoRecyclerView.setAdapter(videoList);

        // Handle search button click to toggle search bar visibility
        searchButton.setOnClickListener(view -> {
            Animation slideDown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down);
            if (searchInputLayout.getVisibility() == View.GONE) {
                searchInputLayout.setVisibility(View.VISIBLE);
                searchInputLayout.startAnimation(slideDown);
            } else {
                searchInputLayout.setVisibility(View.GONE);
                hideKeyboard();
                searchBar.setText("");
                filter("");
            }
        });

        // Handle bottom navigation bar item selection
        bottomNavBar.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                // Render all videos when the home button is clicked
                videoList.setVideoItems(videoViewModel.getVideoItems().getValue());
                searchBar.setText("");
                searchBar.clearFocus();
                videoRecyclerView.smoothScrollToPosition(0);
                return true;
            } else if (itemId == R.id.nav_login) {
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivity(loginIntent);
                finish();
            } else if (itemId == R.id.nav_upload) {
                Intent uploadIntent = new Intent(HomeActivity.this, UploadActivity.class);
                uploadIntent.putExtra("maxId", getMaxId(videoViewModel.getVideoItems().getValue()));
                startActivityForResult(uploadIntent, UPLOAD_REQUEST_CODE); // Start UploadActivity for result
            }
            return false;
        });

        // Handle search bar actions
        searchBar.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filter(v.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });

        // Handle text changes and clear button click
        searchInputLayout.setEndIconOnClickListener(view -> {
            searchBar.setText("");
            searchBar.clearFocus();
            filter("");
            hideKeyboard();
        });

        // Implement the dark mode toggle logic
        MenuItem darkModeItem = sideBar.getMenu().findItem(R.id.nav_dark_mode);
        SwitchCompat darkModeSwitch = (SwitchCompat) darkModeItem.getActionView();
        darkModeSwitch.setChecked(isDarkMode);
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleTheme(isChecked);
        });

        // Handle log out menu item click
        MenuItem logOutItem = sideBar.getMenu().findItem(R.id.nav_logout);
        logOutItem.setOnMenuItemClickListener(item -> {
            if (CurrentUserManager.getInstance().getCurrentUser() != null) {
                CurrentUserManager.getInstance().logout();
                startActivity(getIntent());
                finish();
            } else {
                Toast.makeText(this, "You are not logged in", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    private static final int UPLOAD_REQUEST_CODE = 1001;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UPLOAD_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Retrieve the uploaded video item from the UploadActivity result
            VideoItem uploadedVideoItem = data.getParcelableExtra("uploadedVideoItem");
            if (uploadedVideoItem != null) {
                // Add the uploaded video item to the list and refresh the RecyclerView
                videoViewModel.addVideoItem(uploadedVideoItem);
                // Show a success message
                Toast.makeText(this, "Video uploaded successfully", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Hide the keyboard
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
        }
    }

    // Filter video items based on the query
    private void filter(String query) {
        List<VideoItem> filteredList = new ArrayList<>();
        for (VideoItem videoItem : videoViewModel.getVideoItems().getValue()) {
            if (videoItem.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(videoItem);
            }
        }
        videoList.setVideoItems(filteredList);
    }

    // Toggle theme and save preference
    private void toggleTheme(boolean isDarkMode) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("dark_mode", isDarkMode);
        editor.apply();

        if (isDarkMode) {
            getWindow().setWindowAnimations(R.style.WindowAnimationFadeInOutDark);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            getWindow().setWindowAnimations(R.style.WindowAnimationFadeInOutLight);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    @Override
    public void onVideoItemClick(VideoItem videoItem) {
        // Handle video item click
        String videoResourceName = videoItem.getVideoURL();
        String videoTitle = videoItem.getTitle();
        String videoDescription = videoItem.getDescription();
        String author = videoItem.getAuthor();
        int videoLikes = videoItem.getLikes();
        int videoViews = videoItem.getViews();
        int videoId = videoItem.getId();
        String videoDate = videoItem.getDate();
        Intent moveToWatch = new Intent(this, VideoWatchActivity.class);
        moveToWatch.putExtra("video_resource_name", videoResourceName);
        moveToWatch.putExtra("video_title", videoTitle);
        moveToWatch.putExtra("video_description", videoDescription);
        moveToWatch.putExtra("video_likes", videoLikes);
        moveToWatch.putExtra("video_date", videoDate);
        moveToWatch.putExtra("video_views", videoViews);
        moveToWatch.putExtra("video_id", videoId);
        moveToWatch.putExtra("video_author", author);
        startActivity(moveToWatch);
    }

    // Get the maximum ID from the list of video items
    private int getMaxId(List<VideoItem> videoItems) {
        int max = 0;
        for (VideoItem v : videoItems) {
            max = Math.max(max, v.getId());
        }
        return max;
    }
}
