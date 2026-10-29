package cn.edu.qut.service;

import cn.edu.qut.common.BusinessException;
import cn.edu.qut.dao.FavoriteDao;
import cn.edu.qut.dao.SpotDao;
import cn.edu.qut.entity.Favorite;
import cn.edu.qut.support.ImageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteDao favoriteDao;
    private final SpotDao spotDao;
    private final ImageService imageService;

    public FavoriteServiceImpl(FavoriteDao favoriteDao, SpotDao spotDao, ImageService imageService) {
        this.favoriteDao = favoriteDao;
        this.spotDao = spotDao;
        this.imageService = imageService;
    }

    @Override
    @Transactional
    public Map<String, Object> toggle(Long userId, Long spotId) {
        if (spotId == null || spotDao.findById(spotId) == null) {
            throw new BusinessException(404, "景点不存在");
        }
        boolean nowFavorited;
        if (favoriteDao.exists(userId, spotId) > 0) {
            favoriteDao.delete(userId, spotId);
            nowFavorited = false;
        } else {
            favoriteDao.insert(userId, spotId);
            nowFavorited = true;
        }
        return Map.of("favorited", nowFavorited, "spotId", spotId);
    }

    @Override
    public List<Map<String, Object>> list(Long userId) {
        return favoriteDao.findByUser(userId).stream().map(this::toMap).toList();
    }

    private Map<String, Object> toMap(Favorite f) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", f.getId());
        map.put("spotId", f.getSpotId());
        map.put("spotName", f.getSpotName());
        map.put("spotSummary", f.getSpotSummary());
        map.put("categoryName", f.getCategoryName());
        map.put("rating", f.getSpotRating());
        map.put("imageUrl", "/img/spot/" + f.getSpotId() + "?n=0&v=2");
        map.put("createdAt", f.getCreatedAt() == null ? null
                : java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").format(f.getCreatedAt()));
        return map;
    }
}
