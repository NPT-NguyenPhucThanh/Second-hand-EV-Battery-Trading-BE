package com.project.tradingev_batter.Entity;

import com.project.tradingev_batter.enums.PostStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "postid")
    private long postid;

    @Column(name = "title", columnDefinition = "NVARCHAR(255)")
    private String title;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "status", columnDefinition = "NVARCHAR(50)")
    @Enumerated(EnumType.STRING)
    private PostStatus status;

    @Column(name = "created_at")
    private Date created_at;

    @Column(name = "updated_at")
    private Date updated_at;

    @OneToMany(mappedBy = "posts")
    private List<Comment> comments = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "products") //ban đầu ở đây để sai là name = "posts"
    private Product products;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User users;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User userReviewed;

    @OneToMany(mappedBy = "posts")
    private List<Favorite_post> favorite_post = new ArrayList<>();
}
