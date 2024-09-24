package com.jason.memory;

import android.app.ProgressDialog;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import de.hdodenhof.circleimageview.CircleImageView;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.*;

import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.Rotate;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.bumptech.glide.Glide;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import com.jason.memory.FullScreenImageActivity;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ImageButton buttonSendImage;
    private MessageAdapter adapter;
    private WebSocket webSocket;
    private Gson gson;
    private String userId;
    private String userName;
    private static final int PICK_IMAGE_REQUEST = 1;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private DatabaseHelper dbHelper;
    private static final String PREF_USERNAME = "chat_username";
    private static final String CHANGE_USER_COMMAND = ":cu";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        dbHelper = new DatabaseHelper(this);

        // Get or generate username
        SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        userName = prefs.getString(PREF_USERNAME, null);
        if (userName == null) {
            userName = generateRandomName();
            prefs.edit().putString(PREF_USERNAME, userName).apply();
        }
        Toast.makeText(this, "Your username: " + userName, Toast.LENGTH_LONG).show();

        recyclerView = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonSendImage = findViewById(R.id.buttonSendImage);

        adapter = new MessageAdapter(new ArrayList<>(), userName);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        gson = new Gson();

        loadPreviousMessages();
        connectWebSocket();

        buttonSend.setOnClickListener(v -> {
            String message = editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                if (message.equals(CHANGE_USER_COMMAND)) {
                    showChangeUserDialog();
                } else {
                    sendMessage(message);
                }
                editTextMessage.setText("");
            }
        });

        buttonSendImage.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });
    }

    private void showChangeUserDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Change User")
                .setMessage("Do you want to change your username?")
                .setPositiveButton("Yes", (dialog, which) -> changeUserRandomly())
                .setNegativeButton("No", null)
                .show();
    }

    private void changeUserRandomly() {
        String newUserName = generateRandomName();
        SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        prefs.edit().putString(PREF_USERNAME, newUserName).apply();
        userName = newUserName;
        adapter.setCurrentUserName(userName);
        Toast.makeText(this, "Your new username: " + userName, Toast.LENGTH_LONG).show();
    }

    private void loadPreviousMessages() {
        try {
            List<Message> previousMessages = dbHelper.getAllMessages();
            if (previousMessages.isEmpty()) {
                Toast.makeText(this, "No previous messages found", Toast.LENGTH_SHORT).show();
            } else {
                for (Message message : previousMessages) {
                    if (message.isImage()) {
                        // For images, we'll verify their existence when loading
                        verifyAndAddMessage(message);
                    } else {
                        adapter.addMessage(message);
                    }
                }
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                Toast.makeText(this, "Loaded " + previousMessages.size() + " messages", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading messages: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void verifyAndAddMessage(Message message) {
        String imageUrl = message.getImageUrl();
        Glide.with(this)
                .load(imageUrl)
                .apply(new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true))
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        // Image doesn't exist on the server, remove it from the local database
                        dbHelper.deleteMessage(message);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        // Image exists, add it to the adapter
                        adapter.addMessage(message);
                        return false;
                    }
                })
                .preload();
    }


    public void onOpen(WebSocket webSocket, Response response) {
        reconnectAttempts = 0;
        runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Connected to chat server", Toast.LENGTH_SHORT).show());
    }

    private void reconnectWebSocket() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Connection broken. Reconnecting... (Attempt " + reconnectAttempts + ")", Toast.LENGTH_SHORT).show());
            new Handler().postDelayed(this::connectWebSocket, 5000); // 5 seconds delay
        } else {
            runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Failed to reconnect after " + MAX_RECONNECT_ATTEMPTS + " attempts.", Toast.LENGTH_LONG).show());
        }
    }

    private String generateRandomName() {
        String[] names = {"Jason", "Nice", "Jay", "Ben", "Dan", "Dave", "John", "Rob", "Kate", "Luke", "Soye", "Said", "Sun"};
        String newName;
        do {
            newName = names[new Random().nextInt(names.length)];
        } while (newName.equals(userName));
        return newName;
    }


    private void connectWebSocket() {
        if (webSocket != null) {
            webSocket.close(1000, "Reconnecting");
        }
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(Config.WEBSOCKET_URL).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Connected to chat server", Toast.LENGTH_SHORT).show();
                    Log.d("WebSocket", "Connection opened successfully");
                });
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                runOnUiThread(() -> {
                    try {
                        JsonObject jsonMessage = gson.fromJson(text, JsonObject.class);
                        String sender = jsonMessage.get("sender").getAsString();
                        String content = jsonMessage.get("content").getAsString();
                        boolean isImage = jsonMessage.get("is_image").getAsBoolean();
                        String timestamp = jsonMessage.has("timestamp") ? jsonMessage.get("timestamp").getAsString() : null;

                        Message newMessage = new Message(sender, content, isImage);
                        if (timestamp != null) {
                            newMessage.setTimestamp(timestamp);
                        }

                        // Check if the message already exists
                        if (!messageExists(newMessage)) {
                            adapter.addMessage(newMessage);
                            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                            dbHelper.addMessage(newMessage); // Save to local database
                        }
                        Log.d("WebSocket", "Received message: " + text);
                    } catch (Exception e) {
                        Toast.makeText(ChatActivity.this, "Error processing message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("WebSocket", "Error processing message", e);
                    }
                });
            }

            private boolean messageExists(Message newMessage) {
                for (Message message : adapter.getMessages()) {
                    if (message.getSender().equals(newMessage.getSender()) &&
                            message.getContent().equals(newMessage.getContent()) &&
                            message.isImage() == newMessage.isImage() &&
                            message.getTimestamp().equals(newMessage.getTimestamp())) {
                        return true;
                    }
                }
                return false;
            }


            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Connection failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("WebSocket", "Connection failed", t);
                });
            }
        });
    }


    private void sendMessage(String messageContent) {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty("sender", userName);
        jsonMessage.addProperty("content", messageContent);
        jsonMessage.addProperty("is_image", false);
        webSocket.send(jsonMessage.toString());

        Message newMessage = new Message(userName, messageContent, false);
        long id = dbHelper.addMessage(newMessage);
        newMessage.setId(id);
        adapter.addMessage(newMessage);
        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
    }


    private void sendImage(Uri imageUri) {
        ProgressDialog progressDialog = ProgressDialog.show(this, "Uploading", "Please wait...", true);

        new Thread(() -> {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                // Handle rotation
                ExifInterface exif = new ExifInterface(getContentResolver().openInputStream(imageUri));
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int rotationAngle = 0;
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotationAngle = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotationAngle = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotationAngle = 270;
                        break;
                }

                Matrix matrix = new Matrix();
                matrix.postRotate(rotationAngle);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                // Resize while maintaining aspect ratio
                int maxDimension = 1024;
                float scaleFactor = Math.min((float) maxDimension / bitmap.getWidth(), (float) maxDimension / bitmap.getHeight());
                int newWidth = Math.round(bitmap.getWidth() * scaleFactor);
                int newHeight = Math.round(bitmap.getHeight() * scaleFactor);
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                String imageName = "image_" + System.currentTimeMillis() + ".jpg";

                JsonObject jsonMessage = new JsonObject();
                jsonMessage.addProperty("sender", userName);
                jsonMessage.addProperty("type", "image");
                jsonMessage.addProperty("content", base64Image);
                jsonMessage.addProperty("filename", imageName);

                runOnUiThread(() -> {
                    webSocket.send(jsonMessage.toString());
                    progressDialog.dismiss();
                    Toast.makeText(ChatActivity.this, "Image sent", Toast.LENGTH_SHORT).show();
                });

                Message newMessage = new Message(userName, imageName, true);
                dbHelper.addMessage(newMessage);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(ChatActivity.this, "Failed to send image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
                e.printStackTrace();
            }
        }).start();
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);

        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            sendImage(imageUri);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocket != null) {
            webSocket.close(1000, "Activity destroyed");
        }
    }
}

class Message {
    private String sender;
    private String content;
    private boolean isImage;
    private String timestamp;
    private long id;

    public Message(String sender, String content, boolean isImage) {
        this.sender = sender;
        this.content = content;
        this.isImage = isImage;
        this.timestamp = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
    }

    public Message(long id, String sender, String content, boolean isImage, String timestamp) {
        this.id = id;
        this.sender = sender;
        this.content = content;
        this.isImage = isImage;
        this.timestamp = timestamp;
    }

    // Add getter for id
    public long getId() {
        return id;
    }

    // Add setter for id
    public void setId(long id) {
        this.id = id;
    }

    public String getSenderProfileImageUrl() {
        // Return the URL of the sender's profile image
        // You might need to modify your Message class to store this information
        return "https://example.com/profile_image.jpg";
    }

    public void setIsImage(boolean isImage) {
        this.isImage = isImage;
    }

    public String getImageUrl() {
        if (isImage) {
            return content.startsWith("http://") || content.startsWith("https://")
                    ? content
                    : Config.IMAGE_BASE_URL + content;
        }
        return content;  // Return content even if it's not an image
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public boolean isImage() {
        return isImage;
    }

    public String getTimestamp() {
        return timestamp;
    }
}



class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> messages;
    private String currentUserName;

    public MessageAdapter(List<Message> messages, String currentUserName) {
        this.messages = messages;
        this.currentUserName = currentUserName;
    }

    public void setCurrentUserName(String currentUserName) {
        this.currentUserName = currentUserName;
        notifyDataSetChanged();
    }

    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.getSender().equals(currentUserName)) {
            return 1; // Sent message
        } else {
            return 0; // Received message
        }
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }


    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        boolean isCurrentUser = message.getSender().equals(currentUserName);

        holder.senderTextView.setText(message.getSender());
        holder.timestampTextView.setText(message.getTimestamp());
        holder.timestampTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);

        // Align message to right if sent by current user, left otherwise
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.messageContainer.getLayoutParams();
        if (isCurrentUser) {
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            params.removeRule(RelativeLayout.ALIGN_PARENT_START);
            holder.messageContainer.setBackgroundResource(R.drawable.sent_message_background);
            holder.profileImageView.setVisibility(View.GONE);
            holder.senderTextView.setVisibility(View.GONE);
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
            params.removeRule(RelativeLayout.ALIGN_PARENT_END);
            holder.messageContainer.setBackgroundResource(R.drawable.received_message_background);
            holder.profileImageView.setVisibility(View.GONE);
            holder.senderTextView.setVisibility(View.VISIBLE);
        }
        holder.messageContainer.setLayoutParams(params);

        if (message.isImage()) {
            holder.contentTextView.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.VISIBLE);

            String imageUrl = message.getImageUrl();
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.image_placeholder)
                            .error(R.drawable.image_error)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .fitCenter()
                            .override(Target.SIZE_ORIGINAL))
                    .into(holder.imageView);

            holder.imageView.setOnClickListener(v -> openFullScreenImage(v.getContext(), imageUrl));
        } else {
            holder.contentTextView.setVisibility(View.VISIBLE);
            holder.imageView.setVisibility(View.GONE);
            holder.contentTextView.setText(message.getContent());
        }
    }

    private void removeMessageFromDatabase(Message message) {
        // Implement this method to remove the message from your local database
        // You'll need to pass the DatabaseHelper instance to this adapter
        // or use an interface to communicate with the activity
    }


    private void openFullScreenImage(Context context, String imageUrl) {
        Intent intent = new Intent(context, FullScreenImageActivity.class);
        intent.putExtra("IMAGE_URL", imageUrl);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }


    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView senderTextView;
        TextView contentTextView;
        ImageView imageView;
        TextView timestampTextView;
        LinearLayout messageContainer;
        CircleImageView profileImageView;

        MessageViewHolder(View itemView) {
            super(itemView);
            senderTextView = itemView.findViewById(R.id.textViewSender);
            contentTextView = itemView.findViewById(R.id.textViewContent);
            imageView = itemView.findViewById(R.id.imageViewContent);
            timestampTextView = itemView.findViewById(R.id.textViewTimestamp);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            profileImageView = itemView.findViewById(R.id.profileImageView);
        }
    }
}


