package com.project.tradingev_batter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "comments")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "commentid")
    private long commentid;

    @Column(name = "text")
    private String text;

    @Column(name = "created_at")
    private Date created_at;

    @ManyToOne
    @JoinColumn(name = "parent_cmt_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent")
    private List<Comment> childrenComment = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "postid")
    private Post posts;

    @ManyToOne
    @JoinColumn(name = "userid")
    private User users;
}
