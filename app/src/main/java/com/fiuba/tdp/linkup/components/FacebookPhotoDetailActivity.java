package com.fiuba.tdp.linkup.components;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fiuba.tdp.linkup.R;
import com.fiuba.tdp.linkup.domain.FacebookAlbumItem;
import com.fiuba.tdp.linkup.domain.FacebookPhotoItem;
import com.fiuba.tdp.linkup.services.FacebookService;
import com.fiuba.tdp.linkup.util.DownloadImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * An activity representing a single FacebookPhoto detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link FacebookPhotoListActivity}.
 */
public class FacebookPhotoDetailActivity extends AppCompatActivity {

    public static String ARG_ITEM_ID = "ITEM_ID";
    private String albumId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facebookphoto_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        albumId = getIntent().getStringExtra(ARG_ITEM_ID);


        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.facebookphoto_detail_photos);
        assert recyclerView != null;
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        setupRecyclerView(recyclerView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            navigateUpTo(new Intent(this, FacebookPhotoListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        getFacebookPhotosOfAlbum(recyclerView);
    }

    public void getFacebookPhotosOfAlbum(final RecyclerView recyclerView) {
        final List<FacebookPhotoItem> photos = new ArrayList<>();
        new FacebookService().getPhotosFromAlbum(albumId, new Callback<FacebookPhotoItem[]>() {
            @Override
            public void onResponse(Call<FacebookPhotoItem[]> call, Response<FacebookPhotoItem[]> response) {
                Collections.addAll(photos, response.body());
                recyclerView.setAdapter(new FacebookPhotoDetailActivity.SimpleItemRecyclerViewAdapter(photos));
            }

            @Override
            public void onFailure(Call<FacebookPhotoItem[]> call, Throwable t) {

            }
        });
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<FacebookPhotoDetailActivity.SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<FacebookPhotoItem> mValues;

        public SimpleItemRecyclerViewAdapter(List<FacebookPhotoItem> items) {
            mValues = items;
        }

        @Override
        public FacebookPhotoDetailActivity.SimpleItemRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.facebookphoto_detail_content, parent, false);
            return new FacebookPhotoDetailActivity.SimpleItemRecyclerViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final FacebookPhotoDetailActivity.SimpleItemRecyclerViewAdapter.ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);
            new DownloadImage(holder.mPhoto).execute(mValues.get(position).getPicture());

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //TODO: elegir la foto para el perfil!

                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final ImageView mPhoto;
            public FacebookPhotoItem mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mPhoto = (ImageView) view.findViewById(R.id.photo);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mItem.getId() + "'";
            }
        }
    }
}