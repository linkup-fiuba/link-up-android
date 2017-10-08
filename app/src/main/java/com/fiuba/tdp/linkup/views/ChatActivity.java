package com.fiuba.tdp.linkup.views;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.fiuba.tdp.linkup.R;
import com.fiuba.tdp.linkup.components.chat.ChatViewHolder;
import com.fiuba.tdp.linkup.domain.ChatMessage;
import com.fiuba.tdp.linkup.domain.LinkUpUser;
import com.fiuba.tdp.linkup.domain.ServerResponse;
import com.fiuba.tdp.linkup.services.UserManager;
import com.fiuba.tdp.linkup.services.UserService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {
    public static final String CHATS_CHILD = "chats";
    public static final String MESSAGES_CHILD = "messages";
    private static final String TAG = "ChatActivity";
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";
    public static String CHAT_WITH_USER_ID = "CHAT_WITH_USER_ID";
    public static String CHAT_WITH_USER_NAME = "CHAT_WITH_USER_NAME";
    private String otherUserId;
    private String otherUserName;
    private String myUserId;
    private String messageId;
    private String firebaseChatLocation;

    private String mUsername;
    private Button mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;
    private ImageView mAddMessageImageView;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<ChatMessage, ChatViewHolder>
            mFirebaseAdapter;
    private FirebaseAnalytics mFirebaseAnalytics;
    private String myUserLastChatReference;
    private String otherUserLastChatReference;
    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        getOtherUserId();

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null) {
            // Not signed in, launch the Log In activity
            // TODO: avisar que hubo un error, que se loguee devuelta
            startActivity(new Intent(this, LogInActivity.class));
            finish();
            return;
        } else {
//            mUsername = mFirebaseUser.getDisplayName();
            mUsername = UserManager.getInstance().getMyUser().getName();
        }

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        // New message child entries
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();


        attachRecyclerViewAdapter();

        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Conversation data
                DatabaseReference newChatRef = mFirebaseDatabaseReference.child(firebaseChatLocation).push();
                String messageId = newChatRef.getKey();

                ChatMessage chatMessage = new ChatMessage(messageId, UserManager.getInstance().getMyUser().getId(), mUsername, mMessageEditText.getText().toString(), false);
                newChatRef.setValue(chatMessage);

                //my chats and last message data
                DatabaseReference myLastChatRef = mFirebaseDatabaseReference.child(myUserLastChatReference);
                HashMap<String, String> lastChat = new HashMap<String, String>();
                lastChat.put("name", otherUserName);
                lastChat.put("lastMessage", mMessageEditText.getText().toString());
                lastChat.put("otherUserId", otherUserId);
                lastChat.put("messageId", messageId);

                myLastChatRef.setValue(lastChat);

                //OTHER chats and last message data
                DatabaseReference otherLastChatRef = mFirebaseDatabaseReference.child(otherUserLastChatReference);
                HashMap<String, String> otherLastChat = new HashMap<String, String>();
                otherLastChat.put("name", UserManager.getInstance().getMyUser().getName());
                otherLastChat.put("lastMessage", mMessageEditText.getText().toString());
                otherLastChat.put("otherUserId", myUserId);
                otherLastChat.put("messageId", messageId);

                otherLastChatRef.setValue(otherLastChat);


                mMessageEditText.setText("");
            }
        });
    }

    private void getOtherUserId() {
        Intent intentExtras = getIntent();
        Bundle extrasBundle = intentExtras.getExtras();
        if (!extrasBundle.isEmpty()) {
            otherUserId = extrasBundle.getString(CHAT_WITH_USER_ID);
            otherUserName = extrasBundle.getString(CHAT_WITH_USER_NAME);
            myUserId = UserManager.getInstance().getMyUser().getId();

            setUpToolbarTitle();



            if (otherUserId.compareTo(myUserId) < 0) {
                messageId = otherUserId + "_" + myUserId;
            } else {
                messageId = myUserId + "_" + otherUserId;
            }

            firebaseChatLocation = CHATS_CHILD + "/" + messageId + "/" + MESSAGES_CHILD;

            myUserLastChatReference = "users/" + myUserId + "/conversations/" + messageId;
            otherUserLastChatReference = "users/" + otherUserId + "/conversations/" + messageId;


        } else {
            otherUserId = "";
            otherUserName = "Error";
            myUserId = "";
            finish();
            // hubo un error, ir para atras
        }
    }

    private void setUpToolbarTitle() {
        ((TextView) toolbar.findViewById(R.id.other_title_name)).setText(otherUserName);

        final ImageView profileImage = (ImageView) toolbar.findViewById(R.id.other_profile_picture);

        new UserService(this).getUser(otherUserId, new Callback<ServerResponse<LinkUpUser>>() {
            @Override
            public void onResponse(Call<ServerResponse<LinkUpUser>> call, Response<ServerResponse<LinkUpUser>> response) {
                Glide.with(getBaseContext())
                        .load(response.body().data.getPicture())
                        .into(profileImage);
            }

            @Override
            public void onFailure(Call<ServerResponse<LinkUpUser>> call, Throwable t) {

            }
        });

        LinearLayout linearLayout = (LinearLayout) toolbar.findViewById(R.id.toolbar_action);

        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, "HOLAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            }
        });

    }

    private FirebaseRecyclerAdapter<ChatMessage, ChatViewHolder> getFirebaseAdapter() {
        return new FirebaseRecyclerAdapter<ChatMessage,
                ChatViewHolder>(
                ChatMessage.class,
                R.layout.item_message,
                ChatViewHolder.class,
                mFirebaseDatabaseReference.child(firebaseChatLocation)) {

            @Override
            protected void populateViewHolder(final ChatViewHolder viewHolder,
                                              ChatMessage chatMessage, int position) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                viewHolder.bind(chatMessage, mFirebaseDatabaseReference.child(firebaseChatLocation));
            }

            @Override
            public void onDataChanged() {
                // If there are no chat messages, show a view that invites the user to add a message.
//                mEmptyListMessage.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        };
    }

    private void attachRecyclerViewAdapter() {

        mFirebaseAdapter = getFirebaseAdapter();

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }
}