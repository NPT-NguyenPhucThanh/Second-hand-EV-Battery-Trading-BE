package com.project.tradingev_batter.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.tradingev_batter.Entity.Favorite_post;

@Repository
public interface FavoritePostRepository extends JpaRepository<Favorite_post,Long> {
}
