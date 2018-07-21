package com.aryan.bakingapp.Activities;

import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aryan.bakingapp.R;
import com.aryan.bakingapp.pojo.Recipe;
import com.aryan.bakingapp.pojo.Step;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import static com.aryan.bakingapp.Activities.MainActivity.SELECTED_INDEX;
import static com.aryan.bakingapp.Activities.MainActivity.SELECTED_RECIPES;
import static com.aryan.bakingapp.Activities.MainActivity.SELECTED_STEPS;

public class RecipeStepDetailFragment extends Fragment  {
    private SimpleExoPlayerView simpleExoPlayerView;
    private SimpleExoPlayer player;
    private BandwidthMeter bandwidthMeter;
    private ArrayList<Step> steps = new ArrayList<>();
    private int selectedIndex;
    private Handler mainHandler;
    ArrayList<Recipe> recipe;
    String recipeName;
    String videoURL;
    private DefaultTrackSelector trackSelector;

    // Saved instance state keys.
    private static final String KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters";
    private static final String KEY_WINDOW = "window";
    private static final String KEY_POSITION = "position";
    private static final String KEY_AUTO_PLAY = "auto_play";

    private boolean startAutoPlay;
    private int startWindow;
    private long startPosition;
    private boolean mPlayVideoWhenForegrounded;
    private long mLastPosition;
    private DefaultTrackSelector.Parameters trackSelectorParameters;

    public RecipeStepDetailFragment() {

    }

    private ListItemClickListener itemClickListener;

    public interface ListItemClickListener {
        void onListItemClick(List<Step> allSteps,int Index,String recipeName);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TextView textView;
        mainHandler = new Handler();
        bandwidthMeter = new DefaultBandwidthMeter();

        itemClickListener =(RecipeDetailActivity)getActivity();

        recipe = new ArrayList<>();

        if(savedInstanceState != null) {
            steps = savedInstanceState.getParcelableArrayList(SELECTED_STEPS);
            selectedIndex = savedInstanceState.getInt(SELECTED_INDEX);
            recipeName = savedInstanceState.getString("Title");
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY);
            startWindow = savedInstanceState.getInt(KEY_WINDOW);
            startPosition = savedInstanceState.getLong(KEY_POSITION);



        }
        else {
            steps =getArguments().getParcelableArrayList(SELECTED_STEPS);

            if (steps!=null) {
                steps =getArguments().getParcelableArrayList(SELECTED_STEPS);
                selectedIndex=getArguments().getInt(SELECTED_INDEX);
                recipeName=getArguments().getString("Title");
            }
            else {
                recipe =getArguments().getParcelableArrayList(SELECTED_RECIPES);
                steps=(ArrayList<Step>)recipe.get(0).getSteps();
                selectedIndex=0;
                clearStartPosition();
            }

        }



        View rootView = inflater.inflate(R.layout.recipe_step_detail_fragment_body_part, container, false);
        textView = (TextView) rootView.findViewById(R.id.recipe_step_detail_text);
        textView.setText(steps.get(selectedIndex).getDescription());
        textView.setVisibility(View.VISIBLE);

        simpleExoPlayerView = (SimpleExoPlayerView) rootView.findViewById(R.id.playerView);
        simpleExoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);

        videoURL = steps.get(selectedIndex).getVideoURL();

        if (rootView.findViewWithTag("sw600dp-port-recipe_step_detail")!=null) {
            recipeName=((RecipeDetailActivity) getActivity()).recipeName;
            ((RecipeDetailActivity) getActivity()).getSupportActionBar().setTitle(recipeName);
        }

        String imageUrl=steps.get(selectedIndex).getThumbnailURL();
        if (imageUrl!="") {
            Uri builtUri = Uri.parse(imageUrl).buildUpon().build();
            ImageView thumbImage = (ImageView) rootView.findViewById(R.id.thumbImage);
            Picasso.with(getContext()).load(builtUri).into(thumbImage);
        }

        if (!videoURL.isEmpty()) {


            initializePlayer();

            if (rootView.findViewWithTag("sw600dp-land-recipe_step_detail")!=null) {
                getActivity().findViewById(R.id.fragment_container2).setLayoutParams(new LinearLayout.LayoutParams(-1,-2));
                simpleExoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
            }
            else if (isInLandscapeMode(getContext())){
                textView.setVisibility(View.GONE);
            }
        }
        else {
            player=null;
            simpleExoPlayerView.setForeground(ContextCompat.getDrawable(getContext(), R.drawable.ic_visibility_off_white_36dp));
            simpleExoPlayerView.setLayoutParams(new LinearLayout.LayoutParams(300, 300));
        }


        Button mPrevStep = (Button) rootView.findViewById(R.id.previousStep);
        Button mNextstep = (Button) rootView.findViewById(R.id.nextStep);

        mPrevStep.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (steps.get(selectedIndex).getId() > 0) {
                    if (player!=null){
                        player.stop();
                    }
                    itemClickListener.onListItemClick(steps,steps.get(selectedIndex).getId() - 1,recipeName);
                }
                else {
                    Toast.makeText(getActivity(),"You  are in the First step of the recipe", Toast.LENGTH_SHORT).show();

                }
            }});

        mNextstep.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                int lastIndex = steps.size()-1;
                if (steps.get(selectedIndex).getId() < steps.get(lastIndex).getId()) {
                    if (player!=null){
                        player.stop();
                    }
                    itemClickListener.onListItemClick(steps,steps.get(selectedIndex).getId() + 1,recipeName);
                }
                else {
                    Toast.makeText(getContext(),"You already are in the Last step of the recipe", Toast.LENGTH_SHORT).show();

                }
            }});




        return rootView;
    }

    private void initializePlayer() {
        if (player == null) {
            TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);
            DefaultTrackSelector trackSelector = new DefaultTrackSelector(mainHandler, videoTrackSelectionFactory);
            LoadControl loadControl = new DefaultLoadControl();

            player = ExoPlayerFactory.newSimpleInstance(getContext(), trackSelector, loadControl);
            simpleExoPlayerView.setPlayer(player);
            Uri mediaUri=Uri.parse(steps.get(selectedIndex).getVideoURL());

            String userAgent = Util.getUserAgent(getContext(), "Baking App");
            MediaSource mediaSource = new ExtractorMediaSource(mediaUri, new DefaultDataSourceFactory(getContext(), userAgent), new DefaultExtractorsFactory(), null, null);
            player.prepare(mediaSource);
            boolean haveStartPosition = startWindow != C.INDEX_UNSET;
            if (haveStartPosition) {
                player.seekTo(startWindow, startPosition);
            }
            player.prepare(mediaSource, !haveStartPosition, false);
            player.setPlayWhenReady(startAutoPlay);


        }
    }

    @Override
    public void onSaveInstanceState(Bundle currentState) {
        super.onSaveInstanceState(currentState);
        currentState.putParcelableArrayList(SELECTED_STEPS,steps);
        currentState.putInt(SELECTED_INDEX,selectedIndex);
        currentState.putString("Title",recipeName);

        updateStartPosition();
        updateTrackSelectorParameters();

        currentState.putBoolean(KEY_AUTO_PLAY, startAutoPlay);
        currentState.putInt(KEY_WINDOW, startWindow);
        currentState.putLong(KEY_POSITION, startPosition);

    }

    public boolean isInLandscapeMode( Context context ) {
        return (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (player!=null) {
            player.stop();
            player.release();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (player!=null) {
            player.stop();
            player.release();
            player=null;
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT <= 23) {
            updateStartPosition();
            releasePlayer();

        }
    }
    @Override
    public void onResume() {
        super.onResume();

        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer();
        }


    }

    private void updateStartPosition() {
        if (player != null) {
            startAutoPlay = player.getPlayWhenReady();
            startWindow = player.getCurrentWindowIndex();
            startPosition = Math.max(0, player.getCurrentPosition());
        }
    }
    private void updateTrackSelectorParameters() {
        if (trackSelector != null) {
            trackSelectorParameters = trackSelector.getParameters();
        }
    }
    private void releasePlayer() {
        if (player != null) {
            updateTrackSelectorParameters();
            updateStartPosition();
            player.release();
            player = null;
            trackSelector = null;
        }

    }
    private void clearStartPosition() {
        startAutoPlay = true;
        startWindow = C.INDEX_UNSET;
        startPosition = C.TIME_UNSET;
    }


}
