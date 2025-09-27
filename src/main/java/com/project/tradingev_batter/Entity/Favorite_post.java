package com.project.tradingev_batter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "favorite_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favorite_post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favoriteid")
    private long favoriteid;

    @Column(name = "created_at")
    private Date created_at;

    @ManyToOne
    @JoinColumn(name = "userid")
    private User users;

    @ManyToOne
    @JoinColumn(name = "postid")
    private Post posts;
}
