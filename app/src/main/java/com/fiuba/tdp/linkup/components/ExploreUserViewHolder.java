package com.fiuba.tdp.linkup.components;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.fiuba.tdp.linkup.R;
import com.fiuba.tdp.linkup.domain.Match;
import com.fiuba.tdp.linkup.domain.ServerResponse;
import com.fiuba.tdp.linkup.services.UserManager;
import com.fiuba.tdp.linkup.services.UserService;
import com.fiuba.tdp.linkup.views.ExploreFragment;
import com.fiuba.tdp.linkup.views.OtherProfileActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.R.color.holo_orange_light;
import static android.R.color.holo_red_light;

public class ExploreUserViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = "USER CARD";
    private final Button sendMessageButton;
    private final ImageButton closeImageButton;
    private final ImageButton favoriteImageButton;
    private final ImageButton superLikeImageButton;
    private final ExploreFragment.ExploreUserContentAdapter adapter;
    public String userId;
    public ImageView picture;
    public TextView name;
    public TextView description;
    public boolean favoriteImageButtonChecked = false;
    boolean superLikeImageButtonChecked = false;
    private ViewGroup parent;

    public ExploreUserViewHolder(LayoutInflater inflater, final ViewGroup parent, ExploreFragment.ExploreUserContentAdapter exploreUserContentAdapter) {
        super(inflater.inflate(R.layout.fragment_explore_item_card, parent, false));

        this.parent = parent;
        this.adapter = exploreUserContentAdapter;

        picture = (ImageView) itemView.findViewById(R.id.card_image);
        name = (TextView) itemView.findViewById(R.id.card_title);
        description = (TextView) itemView.findViewById(R.id.card_text);


        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Snackbar.make(v, "Ver Perfil de " + name.getText().toString(),
                        Snackbar.LENGTH_SHORT).show();
                Intent intent = new Intent(v.getContext(), OtherProfileActivity.class);
                intent.putExtra(OtherProfileActivity.ID_USER, Long.parseLong(userId));
                v.getContext().startActivity(intent);
            }
        });

        // Adding Snackbar to Action Button inside card
        sendMessageButton = (Button) itemView.findViewById(R.id.message_button);
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Snackbar.make(v, "Enviar mensaje a " + name.getText().toString(),
                        Snackbar.LENGTH_LONG).show();
            }
        });

        closeImageButton = (ImageButton) itemView.findViewById(R.id.closeButton);
        closeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar.make(v, "Eliminaste de tu listado a " + name.getText().toString(),
                        Snackbar.LENGTH_LONG).show();
                deleteFromAround(userId);
            }
        });

        favoriteImageButton = (ImageButton) itemView.findViewById(R.id.favorite_button);
        favoriteImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pressLikeButton(v);
            }
        });


        superLikeImageButton = (ImageButton) itemView.findViewById(R.id.superlike_button);
        superLikeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar.make(v, "Le pusiste un SUPER like a " + name.getText().toString(),
                        Snackbar.LENGTH_LONG).show();
                if (superLikeImageButtonChecked) {
                    superLikeImageButton.setImageTintList(ContextCompat.getColorStateList(parent.getContext(), R.color.button_grey));
                } else {
                    superLikeImageButton.setImageTintList(ContextCompat.getColorStateList(parent.getContext(), holo_orange_light));
                }
                superLikeImageButtonChecked = !superLikeImageButtonChecked;
            }
        });
    }

    private void deleteFromAround(String userId) {
        new UserService(itemView.getContext()).deleteUserFromAround(UserManager.getInstance().getMyUser().getId(), userId, new Callback<ServerResponse<String>>() {
            @Override
            public void onResponse(Call<ServerResponse<String>> call, Response<ServerResponse<String>> response) {
                Log.e(TAG, "Deleted from arround");
                adapter.deleteItem(getAdapterPosition());
            }

            @Override
            public void onFailure(Call<ServerResponse<String>> call, Throwable t) {
                Snackbar.make(itemView, "Hubo un error al contactar al servidor. Por favor intenta más tarde",
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void pressLikeButton(View v) {
        favoriteImageButtonChecked = !favoriteImageButtonChecked;
        updateLikeStatus();
        if (favoriteImageButtonChecked) {
            sendLikeToServer(v);
        } else {
            sendDeleteLikeToServer(v);
        }
    }

    private void sendDeleteLikeToServer(final View v) {
        new UserService(v.getContext()).deleteLikeToUser(UserManager.getInstance().getMyUser().getId(), userId, new Callback<ServerResponse<String>>() {
            String LOG_LIKE = "DELETE LIKE FROM USER";

            @Override
            public void onResponse(Call<ServerResponse<String>> call, Response<ServerResponse<String>> response) {
                Log.d(LOG_LIKE, "message = " + response.message());
                if (response.isSuccessful()) {
                    Log.d(LOG_LIKE, "-----isSuccess----");
                    Log.d(LOG_LIKE, response.body().data);
                } else {
                    Log.d(LOG_LIKE, "-----isFalse-----");
                    this.onFailure(call, null);
                }
            }

            @Override
            public void onFailure(Call<ServerResponse<String>> call, Throwable t) {
                favoriteImageButtonChecked = !favoriteImageButtonChecked;
                updateLikeStatus();
                Snackbar.make(v, "Hubo un error al contactar al servidor. Por favor intenta más tarde",
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void sendLikeToServer(final View v) {
        new UserService(v.getContext()).postLikeToUser(UserManager.getInstance().getMyUser().getId(), userId, new Callback<ServerResponse<Match>>() {
            String LOG_LIKE = "LIKE USER";

            @Override
            public void onResponse(Call<ServerResponse<Match>> call, Response<ServerResponse<Match>> response) {
                Log.d(LOG_LIKE, "message = " + response.message());
                if (response.isSuccessful()) {
                    Log.d(LOG_LIKE, "-----isSuccess----");
                    Log.d(LOG_LIKE, response.body().data.getLink().toString());

                    if (response.body().data.getLink()) {
                        showAlert("Felicitaciones!", "Hay match!");
                    }

                } else {
                    Log.d(LOG_LIKE, "-----isFalse-----");
                    this.onFailure(call, null);
                }
            }

            @Override
            public void onFailure(Call<ServerResponse<Match>> call, Throwable t) {
                favoriteImageButtonChecked = !favoriteImageButtonChecked;
                updateLikeStatus();
                Snackbar.make(v, "Hubo un error al contactar al servidor. Por favor intenta más tarde",
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    public void updateLikeStatus() {
        if (favoriteImageButtonChecked) {
            favoriteImageButton.setImageTintList(ContextCompat.getColorStateList(parent.getContext(), holo_red_light));
        } else {
            favoriteImageButton.setImageTintList(ContextCompat.getColorStateList(parent.getContext(), R.color.button_grey));
        }
    }

    private void showAlert(String title, String message) {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext());

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(message)
                .setTitle(title);

        // 3. Add the buttons
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                dialog.dismiss();
            }
        });

        // 4. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();

        dialog.show();

    }
}
