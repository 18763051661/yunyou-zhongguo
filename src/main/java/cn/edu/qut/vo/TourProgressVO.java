package cn.edu.qut.vo;

import java.util.List;

/**
 * 在线游览进度。
 */
public class TourProgressVO {

    private String sessionId;
    private Long routeId;
    private int total;
    private int visited;
    private int percent;
    private boolean finished;
    private List<Long> visitedSpotIds;

    public TourProgressVO(String sessionId, Long routeId, int total, int visited,
                          List<Long> visitedSpotIds) {
        this.sessionId = sessionId;
        this.routeId = routeId;
        this.total = total;
        this.visited = visited;
        this.percent = total == 0 ? 0 : (int) Math.round(visited * 100.0 / total);
        this.finished = total > 0 && visited >= total;
        this.visitedSpotIds = visitedSpotIds;
    }

    public String getSessionId() { return sessionId; }
    public Long getRouteId() { return routeId; }
    public int getTotal() { return total; }
    public int getVisited() { return visited; }
    public int getPercent() { return percent; }
    public boolean isFinished() { return finished; }
    public List<Long> getVisitedSpotIds() { return visitedSpotIds; }
}
