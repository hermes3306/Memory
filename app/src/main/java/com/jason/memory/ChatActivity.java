package com.jason.memory;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.*;

import com.bumptech.glide.load.resource.bitmap.Rotate;
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

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private Button buttonSend;
    private Button buttonSendImage;
    private MessageAdapter adapter;
    private WebSocket webSocket;
    private Gson gson;
    private String userId;
    private String userName;
    private static final int PICK_IMAGE_REQUEST = 1;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        dbHelper = new DatabaseHelper(this);

        recyclerView = findViewById(R.id.recyclerViewMessages);
        adapter = new MessageAdapter(new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        userName = generateRandomName();
        Toast.makeText(this, "Your username: " + userName, Toast.LENGTH_LONG).show();

        recyclerView = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonSendImage = findViewById(R.id.buttonSendImage);

        adapter = new MessageAdapter(new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        gson = new Gson();
        userId = UUID.randomUUID().toString(); // Generate a unique user ID

        //loadPreviousMessages(); // Add this method to load previous messages
        connectWebSocket();

        buttonSend.setOnClickListener(v -> {
            String message = editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
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

//    private void loadPreviousMessages() {
//        // This method should load messages from your local storage or server
//        // For example, if you're using SQLite:
//        List<Message> previousMessages = dbHelper.getAllMessages();
//        for (Message message : previousMessages) {
//            adapter.addMessage(message);
//        }
//        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
//    }

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
        Random random = new Random();
        return names[random.nextInt(names.length)];
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
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Connected to chat server", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                runOnUiThread(() -> {
                    JsonObject jsonMessage = gson.fromJson(text, JsonObject.class);
                    String sender = jsonMessage.get("sender").getAsString();
                    String content = jsonMessage.get("content").getAsString();
                    boolean isImage = jsonMessage.get("is_image").getAsBoolean();
                    String timestamp = jsonMessage.has("timestamp") ? jsonMessage.get("timestamp").getAsString() : null;

                    Message message = new Message(sender, content, isImage);
                    if (timestamp != null) {
                        message.setTimestamp(timestamp);
                    }

                    adapter.addMessage(message);
                    recyclerView.scrollToPosition(adapter.getItemCount() - 1);
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
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Connection failed: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Connection closing: " + reason, Toast.LENGTH_SHORT).show());
            }


            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Connection closed: " + reason, Toast.LENGTH_SHORT).show();
                    reconnectWebSocket();
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
        dbHelper.addMessage(newMessage);
    }


    private void sendImage(Bitmap bitmap) {
        ProgressDialog progressDialog = ProgressDialog.show(this, "Uploading", "Please wait...", true);

        new Thread(() -> {
            try {
                Bitmap resizedBitmap = resizeBitmap(bitmap, 1024, 1024);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
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
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                sendImage(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    public Message(String sender, String content, boolean isImage) {
        this.sender = sender;
        this.content = content;
        this.isImage = isImage;
        this.timestamp = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
    }

    public String getImageUrl() {
        if (isImage) {
            if (content.startsWith("http://") || content.startsWith("https://")) {
                return content;
            } else {
                return Config.IMAGE_BASE_URL + content;
            }
        }
        return null;
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

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.senderTextView.setText(message.getSender() + ": ");

        if (message.isImage()) {
            holder.contentTextView.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.VISIBLE);

            String imageUrl = message.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .transform(new RotateTransformation(0)) // This will auto-rotate based on EXIF data
                        .placeholder(R.drawable.image_placeholder)
                        .error(R.drawable.image_error)
                        .into(holder.imageView);
            } else {
                holder.imageView.setImageResource(R.drawable.image_error);
            }
        } else {
            holder.contentTextView.setVisibility(View.VISIBLE);
            holder.imageView.setVisibility(View.GONE);
            holder.contentTextView.setText(message.getContent());
        }
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

    // Add this static inner class
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView senderTextView;
        TextView contentTextView;
        ImageView imageView;
        TextView timestampTextView;

        MessageViewHolder(View itemView) {
            super(itemView);
            senderTextView = itemView.findViewById(R.id.textViewSender);
            contentTextView = itemView.findViewById(R.id.textViewContent);
            imageView = itemView.findViewById(R.id.imageViewContent);
            timestampTextView = itemView.findViewById(R.id.textViewTimestamp);
        }
    }
}