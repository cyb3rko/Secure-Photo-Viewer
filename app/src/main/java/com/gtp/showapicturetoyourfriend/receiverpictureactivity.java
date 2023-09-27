package com.gtp.showapicturetoyourfriend;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;

public class receiverpictureactivity extends AppCompatActivity {

    //to make sure back button doesn't open old images
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish();
        startActivity(intent);
    }

    Handler handly;
    Runnable goahead;
    int page = 0;
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //makes Window Fullscreen and show ontop of the Lockscreen
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_receiverpictureactivity);
        Window wind = this.getWindow();
        wind.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        wind.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("ready", false)) {
                page = savedInstanceState.getInt("pageItem", 0);
                screenislocked();
            }
        }

        //RateThisApp.onCreate(this);
        //RateThisApp.showRateDialogIfNeeded(this);

        //periodically checks if the screen is locked, if it is calls screenislocked()
        handly = new Handler();
        goahead = new Runnable() {
            @Override
            public void run() {
                KeyguardManager myKM = (KeyguardManager) getApplication().getSystemService(Context.KEYGUARD_SERVICE);
                if (myKM != null) {
                    if( myKM.inKeyguardRestrictedInputMode()) {
                        screenislocked();
                    } else {
                        handly.postDelayed(goahead, 40);
                    }
                } else {
                    handly.postDelayed(goahead, 40);
                }
            }
        };
        goahead.run();

    }

    public void buttonpressed(View view) { //called when button is pressed
        screenislocked();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mViewPager != null) {
            outState.putInt("pageItem", mViewPager.getCurrentItem());
            outState.putBoolean("ready", true);
        } else {
            outState.putBoolean("ready", false);
        }
    }

    public void screenislocked() {

        if (handly != null) {
            handly.removeCallbacks(goahead);
        }

        PowerManager.WakeLock screenLock = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
        screenLock.acquire(1);

        screenLock.release();
        //removes handler, wakes up screen and realeases Wakelock immediately

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        setContentView(R.layout.activity_receivemultiple);

        ArrayList<Uri> imageUris = null;

        if(Intent.ACTION_SEND.equals(action)) { //puts Uris into an array, whether there is one or multiple
            Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            imageUris = new ArrayList<>();
            imageUris.add(imageUri);
        } else if(Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        } //puts Uris into an array, whether there is one or multiple

        DemoCollectionPagerAdapter.setCounts(imageUris.size());
        DemoCollectionPagerAdapter.setUris(imageUris, this);

        PagerAdapter mDemoCollectionPagerAdapter = new DemoCollectionPagerAdapter(getSupportFragmentManager());
        DemoCollectionPagerAdapter.setAdapter(mDemoCollectionPagerAdapter);
        mViewPager = findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setAdapter(mDemoCollectionPagerAdapter);

        mViewPager.setCurrentItem(page);
    }

    @Override
    protected void onDestroy() {
        handly.removeCallbacks(goahead);
        super.onDestroy();
    }

    public static class DemoCollectionPagerAdapter extends FragmentStatePagerAdapter {
        public DemoCollectionPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        static ArrayList<Uri> uris;
        static Context context;

        public static void recreateafterpermission() { //this is called when the user gives permission to view file
            atp.notifyDataSetChanged();
        }

        public static void setUris(ArrayList<Uri> muri, Context c) {
            uris=muri;
            context = c;
        }

        public static void setAdapter(PagerAdapter adapter) {
            atp = adapter;
        }
        static PagerAdapter atp;

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new DemoObjectFragment();
            Bundle args = new Bundle();

            Uri uri = uris.get(i);
            String stringuri = "";
            if(uri != null) {
                stringuri = uri.toString();
            } else {
                Toast.makeText(context, R.string.invalid, Toast.LENGTH_LONG).show();
            }

            args.putString("Uri",stringuri);
            fragment.setArguments(args);
            return fragment;
        }

        static int count;

        @Override
        public int getCount() {
            return count;
        }

        public static void setCounts(int mcount) {
            count = mcount;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "OBJECT " + (position + 1);
        }
    }

    public static class DemoObjectFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View rootView = null;

            Bundle args = getArguments();
            String forUri = args.getString("Uri");
            Uri urinormal = Uri.parse(forUri);

            String type = null;
            String extension = MimeTypeMap.getFileExtensionFromUrl(forUri.replace("~",""));
            if (extension != null) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }

            String startwith = getActivity().getContentResolver().getType(urinormal);

            if(startwith!=null) {
                if (startwith.startsWith("image/")) {
                    rootView = inflater.inflate(R.layout.adapterimage, container, false);
                    pictureSet((TouchImageView) rootView.findViewById(R.id.touchImageView), urinormal);
                } else if (startwith.startsWith("video/")) {
                    rootView = inflater.inflate(R.layout.adaptervideo, container, false);
                    videoSet((VideoView) rootView.findViewById(R.id.videoview), urinormal);
                }
            } else {
                if(type!=null) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        if (getActivity().checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            rootView=typeMethod(rootView,urinormal,container,type,inflater);
                        } else {
                            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                            Toast.makeText(getActivity(), R.string.permission, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        rootView=typeMethod(rootView,urinormal,container,type,inflater);
                    }
                }
            }

            if(viewvisibleinoncreate) {
                viewnowvisible(true);
            }

            return rootView;

        }

        private View typeMethod(View rootView, Uri urinormal, ViewGroup container, String type,LayoutInflater inflater) {
            if (type.startsWith("image/")) {
                rootView = inflater.inflate(R.layout.adapterimage, container, false);
                pictureSetFile((TouchImageView) rootView.findViewById(R.id.touchImageView), urinormal);
            } else if (type.startsWith("video/")) {
                rootView = inflater.inflate(R.layout.adaptervideo, container, false);
                videoSet((VideoView) rootView.findViewById(R.id.videoview), urinormal);
            }
            return rootView;
        }

        private void pictureSet(final TouchImageView imageset, Uri urinormal) {

            imageset.setMaxZoom(30);

            Glide.with(getContext())
                    .load(urinormal)
                    .override(2000, 2000)
                    //.override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .into(new DrawableImageViewTarget(imageset) {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            super.onResourceReady(resource, transition);
                            imageset.setZoom(1);
                        }
                    })
            ;
        }

        private void pictureSetFile(final TouchImageView imageset, Uri urinormal) {
            imageset.setMaxZoom(30);

            Glide.with(getContext())
                    .load(urinormal)
                    .override(2000, 2000)
                    //.override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .into(new DrawableImageViewTarget(imageset) {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            super.onResourceReady(resource, transition);
                            imageset.setZoom(1);
                        }
                    })
            ;
        }

        private void videoSet(VideoView video, Uri urinormal) {
            video.setVideoURI(urinormal);
            video.seekTo(1);
            controller = new MediaController(getActivity());
            videow = video;
            isvideo = true;
        }

        VideoView videow;
        MediaController controller;
        Boolean viewvisibleinoncreate = false;
        Boolean isvideo = false;
        Boolean iscontrollershowing = false;

        @Override
        public void setUserVisibleHint(boolean isVisibleToUser) {
            super.setUserVisibleHint(isVisibleToUser);
            if (getView() != null) {
                viewnowvisible(isVisibleToUser);
            } else {
                viewvisibleinoncreate = isVisibleToUser;
            }
        }

        public void viewnowvisible(boolean isVisibleToUser) {
            if (isvideo) {
                if(isVisibleToUser) {
                    Log.d("r","VIDEO ON");
                    if(iscontrollershowing) {
                        controller.show();
                    } else {
                        controller.setAnchorView(videow);
                        controller.setMediaPlayer(videow);
                        videow.setMediaController(controller);
                        iscontrollershowing = true;
                    }
                    videow.start();
                } else {
                    Log.d("r","VIDEO OFF");
                    videow.pause();
                    controller.hide();
                }
            }
        }

    }
}


