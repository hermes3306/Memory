package com.jason.memory;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostActivity extends AppCompatActivity {

    private EditText postEditText;

    private ImageButton submitButton;
    private RecyclerView postsRecyclerView;
    private PostAdapter postAdapter;
    private List<Post> posts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        postEditText = findViewById(R.id.searchField);
        submitButton = findViewById(R.id.composeButton);
        postsRecyclerView = findViewById(R.id.recyclerView);

        posts = generateSamplePosts();
        postAdapter = new PostAdapter(posts);
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        postsRecyclerView.setAdapter(postAdapter);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String postContent = postEditText.getText().toString().trim();
                if (!postContent.isEmpty()) {
                    Post newPost = new Post("User", "EN KR", postContent, new Date(), new ArrayList<>());
                    postAdapter.addPost(newPost);
                    postsRecyclerView.scrollToPosition(0);
                    postEditText.setText("");
                    Toast.makeText(PostActivity.this, "Post submitted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PostActivity.this, "Please enter some content for your post", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private List<Post> generateSamplePosts() {
        List<Post> samplePosts = new ArrayList<>();
        samplePosts.add(new Post("JUPI", "TR EN ES KR CN FR", "This is a sample post content.", new Date(), Arrays.asList("https://picsum.photos/200", "https://picsum.photos/201")));
        samplePosts.add(new Post("Alice", "EN FR", "Another sample post here!", new Date(), Arrays.asList("https://picsum.photos/202")));
        samplePosts.add(new Post("Bob", "ES KR", "Yet another sample post.", new Date(), new ArrayList<>()));
        return samplePosts;
    }
}

 class Post {
    private String userName;
    private String userInfo;
    private String content;
    private Date date;
    private List<String> imageUrls;
    private int likeCount;
    private int commentCount;

    public Post(String userName, String userInfo, String content, Date date, List<String> imageUrls) {
        this.userName = userName;
        this.userInfo = userInfo;
        this.content = content;
        this.date = date;
        this.imageUrls = imageUrls;
        this.likeCount = 0;
        this.commentCount = 0;
    }

    // Getters and setters
    public String getUserName() { return userName; }
    public String getUserInfo() { return userInfo; }
    public String getContent() { return content; }
    public Date getDate() { return date; }
    public List<String> getImageUrls() { return imageUrls; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }

    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
}


class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> posts;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public PostAdapter(List<Post> posts) {
        this.posts = posts;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.userName.setText(post.getUserName());
        holder.userInfo.setText(post.getUserInfo());
        holder.postContent.setText(post.getContent());

        // Load profile image
        Glide.with(holder.itemView.getContext())
                .load(R.drawable.default_profile)
                .into(holder.profileImage);

        // Load post images
        holder.imageGrid.removeAllViews();
        for (String imageUrl : post.getImageUrls()) {
            ImageView imageView = new ImageView(holder.itemView.getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.setMargins(4, 4, 4, 4);
            imageView.setLayoutParams(params);
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .into(imageView);
            holder.imageGrid.addView(imageView);
        }

        // Set click listeners for buttons
        holder.likeButton.setOnClickListener(v -> {
            // Handle like action
        });

        holder.commentButton.setOnClickListener(v -> {
            // Handle comment action
        });

        holder.shareButton.setOnClickListener(v -> {
            // Handle share action
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void addPost(Post post) {
        posts.add(0, post);
        notifyItemInserted(0);
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView userName;
        TextView userInfo;
        TextView postContent;
        GridLayout imageGrid;
        ImageButton likeButton;
        ImageButton commentButton;
        ImageButton shareButton;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            userName = itemView.findViewById(R.id.userName);
            userInfo = itemView.findViewById(R.id.userInfo);
            postContent = itemView.findViewById(R.id.postContent);
            imageGrid = itemView.findViewById(R.id.imageGrid);
            likeButton = itemView.findViewById(R.id.likeButton);
            commentButton = itemView.findViewById(R.id.commentButton);
            shareButton = itemView.findViewById(R.id.shareButton);
        }
    }
}