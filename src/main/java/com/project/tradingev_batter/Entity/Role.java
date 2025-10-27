package com.project.tradingev_batter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "roles")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roleid")
    private long roleid;

    @Column(name = "rolename", columnDefinition = "NVARCHAR(50)")
    private String rolename;

    @Column(name = "joindate")
    private Date joindate;

    @ManyToMany(mappedBy = "roles")
    private List<User> users =  new ArrayList<>();
}
