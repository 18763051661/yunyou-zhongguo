package cn.edu.qut.support;

import cn.edu.qut.dao.SpotDao;
import cn.edu.qut.entity.ScenicSpot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 真实景点照片服务(只提供真实拍摄照片, 绝不使用 AI 合成图或随机图库图冒充实景)。
 *
 * 数据来源与优先级:
 *   1) 360 图片搜索 JSON 接口: 候选带标题/尺寸, 仅保留"标题含景点名"的结果,
 *      从源头排除与景点无关的垃圾图/防盗链默认图;
 *   2) 360 不可用时降级必应图片搜索, 同样做尺寸/内容校验;
 *   3) 全部失败时输出"印有景点名的占位图", 不用随机风景图误导用户。
 *
 * 质量控制:
 *   - 多候选并行下载, 但按"相关度排名 + 横图优先"挑选, 而非谁下载快用谁;
 *   - ImageIO 解码校验尺寸(拒绝小图/奇怪比例/非图片响应);
 *   - MD5 跨景点全局去重(防盗链 CDN 给不同景点返回同一张默认图);
 *   - 同批次多个不同 URL 返回相同字节时, 整组判定为防盗链默认图并弃用;
 *   - 磁盘(v2 文件名, 与历史坏缓存隔离) + 内存两级缓存。
 *
 * 对外只暴露字节数据 + content-type, 由 {@code ImageController} 同源输出,
 * 前端 <img src="/img/spot/{id}"> 无跨域/防盗链/鉴权问题。
 */
@Service
public class SpotImageService {

    private static final Logger log = LoggerFactory.getLogger(SpotImageService.class);

    /** 每个景点缓存的图片数量(1 主图 + 3 画廊) */
    private static final int WANT = 4;
    private static final int MIN_BYTES = 8 * 1024;
    /** 参与并行下载的最大候选数 */
    private static final int MAX_RACERS = 14;
    /** 整轮下载最长等待时间(只要凑够所需张数会立即返回, 该值仅是兜底上限) */
    private static final long FETCH_BUDGET_SECONDS = 8;
    /** 输出图片最大宽度: 超过则等比缩小, 控制在几百 KB, 卡片/画廊都够用 */
    private static final int MAX_WIDTH = 1600;
    private static final float JPEG_QUALITY = 0.85f;
    /** 磁盘缓存超过该大小则在首次读取时原地转码压缩 */
    private static final int RECOMPRESS_THRESHOLD = 400 * 1024;
    /** 缓存文件名版本: 升级后旧的坏缓存文件自动失效 */
    private static final String CACHE_VERSION = "v2";

    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    /** 360 图片搜索 JSON 接口(中文景点相关度高, 带标题与尺寸) */
    private static final String SO_360 = "https://image.so.com/j?src=srp&sn=0&pn=40&q=";
    /** 必应图片异步搜索接口(降级方案), murl 为候选原图地址 */
    private static final String BING_ASYNC = "https://cn.bing.com/images/async?count=30&first=1"
            + "&mmasync=1&mkt=zh-CN&qft=+filterui:photo-photo&q=";
    private static final Pattern MURL =
            Pattern.compile("murl&quot;:&quot;(.*?)&quot;");

    /** 景点名里的通用后缀, 生成核心匹配词时剥离(长的在前) */
    private static final String[] GENERIC_SUFFIXES = {
            "国家级风景名胜区", "国家级自然保护区", "国家级旅游度假区", "国家森林公园", "国家地质公园",
            "旅游度假区", "风景名胜区", "风景管理区", "旅游风景区", "旅游区", "度假区",
            "风景区", "森林公园", "地质公园", "保护区", "景区", "公园", "旧址", "遗址", "博物院", "博物馆"
    };
    /** 硬排除: 素材水印站/电商站, 不可能给出该景点的真实游客照片 */
    private static final String[] HOST_BLACKLIST = {
            "699pic.com", "58pic.com", "qiantucdn.com", "qian-tu", "nipic.com", "huitu.com",
            "redocn.com", "tooopen.com", "yestone.com", "ooopen.com", "ibaotu.com",
            "taobao.com", "tmall.com", "jd.com", "1688.com", "pinduoduo.com", "gaoyadianjigun"
    };

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(6))
            .build();
    private final ObjectMapper jackson = new ObjectMapper();

    /** 候选下载线程池(daemon, 不阻止容器关闭) */
    private final ExecutorService fetchPool = Executors.newFixedThreadPool(32, r -> {
        Thread t = new Thread(r, "spot-img-fetch");
        t.setDaemon(true);
        return t;
    });
    /** 限制同时打向外网搜索接口的并发, 避免首页多景点同时触发时被限流 */
    private final Semaphore searchGate = new Semaphore(4, true);

    private final SpotDao spotDao;
    private final Path cacheDir;

    private final Map<String, Object> locks = new ConcurrentHashMap<>();
    /** 内存缓存: seed -> 已确认的真实照片(可能少于 WANT) */
    private final Map<String, List<ImageData>> memCache = new ConcurrentHashMap<>();
    /** 连续抓取失败轮数: 网络抖动导致整轮无收获时允许重试, 最多 2 轮后放弃, 避免每次请求都长时间等待 */
    private final Map<String, Integer> failRounds = new ConcurrentHashMap<>();
    /** 跨景点全局图片指纹: md5 -> 首个占用它的 seed, 防止同一张图(含防盗链默认图)被多个景点使用 */
    private static final Map<String, String> GLOBAL_OWNERS = new ConcurrentHashMap<>();

    public SpotImageService(SpotDao spotDao) {
        this.spotDao = spotDao;
        System.setProperty("java.awt.headless", "true");
        this.cacheDir = pickWritableCacheDir();
        if (cacheDir != null) {
            log.info("景点真实照片缓存目录: {}", cacheDir);
        } else {
            log.warn("无可用磁盘缓存目录, 景点图片仅使用内存缓存");
        }
        // 容器启动后后台预热热门景点主图, 首页第一次打开即可秒出
        Thread warmer = new Thread(() -> {
            try {
                Thread.sleep(5000);
                for (ScenicSpot s : spotDao.findTopPopular(12)) {
                    try {
                        forSpot(s.getId(), 0);
                    } catch (Exception ignored) {
                        // 单个景点预热失败不影响其它
                    }
                }
            } catch (Exception ignored) {
                // 预热失败静默
            }
        }, "spot-img-warm");
        warmer.setDaemon(true);
        warmer.start();
    }

    private Path pickWritableCacheDir() {
        List<Path> candidates = List.of(
                Path.of(System.getProperty("java.io.tmpdir"), "yunyou-images"),
                Path.of(System.getProperty("user.home"), ".yunyou-images"));
        for (Path dir : candidates) {
            try {
                Files.createDirectories(dir);
                Path probe = dir.resolve(".wprobe");
                Files.writeString(probe, "ok");
                Files.deleteIfExists(probe);
                return dir;
            } catch (IOException | RuntimeException e) {
                // 尝试下一个候选
            }
        }
        return null;
    }

    /** 一张已就绪的图片 */
    public record ImageData(byte[] bytes, String contentType) {}

    /** 搜索候选: 排名越小相关度越高; titleMatched 表示标题命中景点名 */
    private record Candidate(int rank, String url, String title, boolean titleMatched,
                             boolean landscapeMeta) {}

    /** 下载并通过校验的候选图片 */
    private record Fetched(int rank, boolean titleMatched, boolean landscape,
                           String md5, ImageData img) {}

    /**
     * 按景点 id 取真实照片。
     * @param n 图片序号: 0=主图, 1..3=画廊其它视角
     */
    public ImageData forSpot(long spotId, int n) {
        ScenicSpot spot = spotDao.findById(spotId);
        if (spot == null) {
            return placeholder("云游中国");
        }
        String city = spot.getCity() == null ? "" : spot.getCity().trim();
        String query = spot.getName() + (city.isEmpty() || spot.getName().contains(city) ? "" : " " + city);
        Set<String> keywords = keywords(spot.getName(), city, spot.getProvince());
        int idx = Math.max(0, n);
        List<ImageData> images = ensureImages(query, "spot" + spotId, keywords, idx + 1);
        if (idx < images.size()) {
            return images.get(idx);
        }
        return placeholder(spot.getName());
    }

    /** 按任意关键词取图(用于路线封面等没有单一景点 id 的场景) */
    public ImageData forKeyword(String keyword, int n) {
        if (keyword == null || keyword.isBlank()) {
            return placeholder("云游中国");
        }
        String kw = keyword.trim();
        String seed = "kw" + md5(kw);
        // 路线名没有标题可严格匹配, 走宽松模式(仍保留尺寸/去重/横图等质量控制)
        int idx = Math.max(0, n);
        List<ImageData> images = ensureImages(kw, seed, Set.of(), idx + 1);
        if (idx < images.size()) {
            return images.get(idx);
        }
        return placeholder(kw);
    }

    /**
     * 保证某查询的真实照片集合尽量就绪: 内存 -> 磁盘 -> 360/必应抓取。
     * 每次调用最多做一轮网络抓取(按需为画廊增量补齐), 抓到的只可能是真实照片。
     */
    private List<ImageData> ensureImages(String query, String seed, Set<String> keywords, int wantedSlots) {
        Object lock = locks.computeIfAbsent(seed, k -> new Object());
        synchronized (lock) {
            // 用 md5 合并内存与磁盘缓存, 避免同一张图被重复计数(否则画廊会返回与主图相同的照片)
            LinkedHashMap<String, ImageData> merged = new LinkedHashMap<>();
            memCache.getOrDefault(seed, List.of()).forEach(d -> merged.putIfAbsent(md5Hex(d.bytes()), d));
            if (merged.size() < WANT) {
                for (ImageData d : readDiskCache(seed)) {
                    if (merged.size() >= WANT) {
                        break;
                    }
                    merged.putIfAbsent(md5Hex(d.bytes()), d);
                }
            }
            List<ImageData> result = new ArrayList<>(merged.values());
            int target = Math.min(WANT, Math.max(wantedSlots, result.size()));
            if (result.size() >= target) {
                return commit(seed, result);
            }
            if (failRounds.getOrDefault(seed, 0) >= 2) {
                return commit(seed, result);
            }

            List<Candidate> candidates = new ArrayList<>();
            try {
                searchGate.acquire();
                try {
                    candidates.addAll(search360(query, keywords));
                    // 360 结果过少时叠加必应候选兜底
                    if (candidates.size() < 4) {
                        candidates.addAll(searchBing(query, candidates.size()));
                    }
                } finally {
                    searchGate.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 重启后磁盘缓存的图片先登记指纹, 避免画廊补图时把同一张图再下载一遍
            result.forEach(d -> GLOBAL_OWNERS.putIfAbsent(md5Hex(d.bytes()), seed));

            int before = result.size();
            List<Fetched> picked = selectRealPhotos(candidates, seed, target - before);
            for (int i = 0; i < picked.size(); i++) {
                ImageData img = picked.get(i).img();
                persist(seed, before + i, img);
                result.add(img);
            }
            if (picked.isEmpty()) {
                int fails = failRounds.merge(seed, 1, Integer::sum);
                log.warn("景点图片抓取失败(无通过校验的真实照片, 第{}轮), query={}", fails, query);
            } else {
                // 之前轮次失败过、本次成功, 解除失败锁定
                failRounds.remove(seed);
            }
            return commit(seed, result);
        }
    }

    private List<ImageData> commit(String seed, List<ImageData> result) {
        List<ImageData> frozen = List.copyOf(result);
        List<ImageData> old = memCache.get(seed);
        if (old == null || frozen.size() >= old.size()) {
            memCache.put(seed, frozen);
        }
        return frozen;
    }

    private void persist(String seed, int order, ImageData img) {
        if (cacheDir == null) {
            return;
        }
        try {
            Files.write(cacheDir.resolve(CACHE_VERSION + "_" + seed + "_" + order + ".img"), img.bytes());
        } catch (IOException e) {
            // 磁盘不可写时静默降级为纯内存缓存
        }
    }

    private List<ImageData> readDiskCache(String seed) {
        List<ImageData> list = new ArrayList<>();
        if (cacheDir == null) {
            return list;
        }
        for (int i = 0; i < WANT; i++) {
            File f = cacheDir.resolve(CACHE_VERSION + "_" + seed + "_" + i + ".img").toFile();
            if (f.isFile() && f.length() >= MIN_BYTES) {
                try {
                    byte[] bytes = Files.readAllBytes(f.toPath());
                    // 旧缓存中的超大图(如 3MB 原图)首次读取时原地压缩一次, 之后秒出
                    if (bytes.length > RECOMPRESS_THRESHOLD) {
                        byte[] small = tryRecompress(bytes);
                        if (small != null && small.length < bytes.length) {
                            bytes = small;
                            try {
                                Files.write(f.toPath(), bytes);
                            } catch (IOException ignored) {
                                // 覆盖失败仅本次使用压缩后的字节
                            }
                        }
                    }
                    list.add(new ImageData(bytes, sniffType(bytes)));
                } catch (IOException ignored) {
                    // 单张损坏则跳过
                }
            }
        }
        return list;
    }

    // ------------------------------------------------------------------
    // 图片压缩
    // ------------------------------------------------------------------

    /** 等比缩放到最大宽 {@value #MAX_WIDTH} 并以固定质量编码 JPEG, 把原图控制在几百 KB 以内 */
    private ImageData compressJpeg(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int tw = w;
        int th = h;
        if (w > MAX_WIDTH) {
            tw = MAX_WIDTH;
            th = Math.max(1, (int) Math.round((double) h * MAX_WIDTH / w));
        }
        BufferedImage out;
        if (tw == w && src.getType() == BufferedImage.TYPE_INT_RGB) {
            out = src;
        } else {
            out = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, tw, th);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, tw, th, null);
            g.dispose();
        }
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(96 * 1024);
        try (MemoryCacheImageOutputStream mos = new MemoryCacheImageOutputStream(bos)) {
            writer.setOutput(mos);
            writer.write(null, new IIOImage(out, null, null), param);
        } catch (IOException e) {
            throw new IllegalStateException("JPEG 编码失败", e);
        } finally {
            writer.dispose();
        }
        return new ImageData(bos.toByteArray(), "image/jpeg");
    }

    /** 把磁盘上的旧缓存图转成压缩 JPEG; 无法解码时返回 null(保留原文件) */
    private byte[] tryRecompress(byte[] bytes) {
        try {
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bytes));
            if (bi == null || bi.getWidth() < 480) {
                return null;
            }
            return compressJpeg(bi).bytes();
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // 搜索源
    // ------------------------------------------------------------------

    /**
     * 360 图片搜索: 解析 JSON, 仅保留标题命中景点名、非素材/电商站的候选,
     * 保留接口给出的排名与尺寸信息。
     */
    private List<Candidate> search360(String query, Set<String> keywords) {
        List<Candidate> list = new ArrayList<>();
        int scanned = 0;
        try {
            String url = SO_360 + URLEncoder.encode(query + " 风景", StandardCharsets.UTF_8);
            String body = httpGetString(url, "https://image.so.com/");
            if (body == null) {
                log.debug("360 搜索无响应(非200或网络失败), query={}", query);
                return list;
            }
            JsonNode items = jackson.readTree(body).get("list");
            if (items == null || !items.isArray()) {
                log.debug("360 返回无 list 数组, query={}, bodyHead={}", query,
                        body.substring(0, Math.min(120, body.length())));
                return list;
            }
            boolean strict = !keywords.isEmpty();
            int rank = 0;
            Set<String> seenUrls = new HashSet<>();
            for (JsonNode it : items) {
                scanned++;
                String img = text(it, "img");
                if (img == null || !img.startsWith("http")) {
                    continue;
                }
                String host = hostOf(img);
                if (hostBlacklisted(host)) {
                    continue;
                }
                String title = text(it, "title");
                boolean matched = !strict || titleContains(title, keywords);
                if (strict && !matched) {
                    // 原图标题不相关时, 360 的缩略图同样不相关, 直接跳过该条
                    continue;
                }
                int w = it.path("width").asInt(0);
                int h = it.path("height").asInt(0);
                if (seenUrls.add(img)) {
                    list.add(new Candidate(rank++, img, title, matched, w > 0 && w >= h));
                }
                // 360 缩略图作为同一结果的备份链接(原图站点抽风时兜底)
                String thumb = text(it, "thumb");
                if (thumb != null && thumb.startsWith("http") && !hostBlacklisted(hostOf(thumb))
                        && seenUrls.add(thumb)) {
                    list.add(new Candidate(rank++, thumb, title, matched, w > 0 && w >= h));
                }
            }
        } catch (Exception e) {
            log.warn("360 图片搜索失败 query={}: {}", query, e.getMessage());
        }
        log.debug("360 候选统计: query={}, keywords={}, scanned={}, accepted={}",
                query, keywords, scanned, list.size());
        return list;
    }

    /** 必应图片搜索兜底: 只有 URL, 无可靠标题, 排名排在 360 之后 */
    private List<Candidate> searchBing(String query, int offset) {
        List<Candidate> list = new ArrayList<>();
        try {
            String url = BING_ASYNC + URLEncoder.encode(query, StandardCharsets.UTF_8);
            String body = httpGetString(url, "https://cn.bing.com/");
            if (body == null) {
                return list;
            }
            Matcher m = MURL.matcher(body);
            int rank = 100 + offset;
            while (m.find() && list.size() < 20) {
                String u = m.group(1).replace("&amp;", "&");
                if (u.startsWith("http") && !hostBlacklisted(hostOf(u))) {
                    list.add(new Candidate(rank++, u, null, false, false));
                }
            }
        } catch (Exception e) {
            log.warn("必应图片搜索失败 query={}: {}", query, e.getMessage());
        }
        return list;
    }

    private String httpGetString(String url, String referer) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", UA)
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .header("Referer", referer)
                .timeout(Duration.ofSeconds(10))
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return resp.statusCode() == 200 ? resp.body() : null;
    }

    // ------------------------------------------------------------------
    // 下载、校验、选图
    // ------------------------------------------------------------------

    /**
     * 并行下载候选并挑选通过所有校验的真实照片:
     * 主图位横图优先, 其余按相关度排名; 跨景点/批次 MD5 去重。
     */
    private List<Fetched> selectRealPhotos(List<Candidate> candidates, String seed, int need) {
        List<Fetched> picked = new ArrayList<>();
        if (candidates.isEmpty() || need <= 0) {
            return picked;
        }
        int racers = Math.min(candidates.size(), MAX_RACERS);
        ExecutorCompletionService<Fetched> cs = new ExecutorCompletionService<>(fetchPool);
        List<Future<Fetched>> futures = new ArrayList<>(racers);
        // 本批次每个 md5 来自哪些 host: 同一字节被 3 个及以上不同站点返回 => 防盗链默认图
        Map<String, Set<String>> md5Hosts = new ConcurrentHashMap<>();
        for (int i = 0; i < racers; i++) {
            final Candidate c = candidates.get(i);
            futures.add(cs.submit(() -> downloadAndValidate(c, seed, md5Hosts)));
        }
        long deadlineNs = System.nanoTime() + Duration.ofSeconds(FETCH_BUDGET_SECONDS).toNanos();
        List<Fetched> ok = new ArrayList<>();
        Set<String> okMd5 = new HashSet<>();
        int done = 0;
        try {
            // 凑够所需张数立即返回(候选已按标题强匹配+站点黑名单过滤, 且跨景点 md5 在线占用,
            // 先到的合格图可直接采用), 不再傻等慢站点
            while (done < racers) {
                long leftNs = deadlineNs - System.nanoTime();
                if (leftNs <= 0 || okMd5.size() >= need) {
                    break;
                }
                Future<Fetched> f = cs.poll(leftNs, TimeUnit.NANOSECONDS);
                if (f == null) {
                    break;
                }
                done++;
                try {
                    Fetched got = f.get();
                    if (got != null && okMd5.add(got.md5())) {
                        ok.add(got);
                    }
                } catch (Exception ignored) {
                    // 单个候选失败, 继续其它候选
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            for (Future<Fetched> f : futures) {
                f.cancel(true);
            }
        }

        // 防盗链默认图: 本批次被 >=3 个不同站点以同一字节返回, 全部弃用
        Set<String> hotlinkDefaults = new LinkedHashSet<>();
        md5Hosts.forEach((md5, hosts) -> {
            if (hosts.size() >= 3) {
                hotlinkDefaults.add(md5);
            }
        });
        // 去重(同批次可能有重复 md5), 并按 横图优先 -> 标题命中 -> 排名 排序
        Set<String> chosenMd5 = new LinkedHashSet<>();
        ok.stream()
                .filter(x -> !hotlinkDefaults.contains(x.md5()))
                .sorted(Comparator.comparing(Fetched::landscape).reversed()
                        .thenComparing(x -> !x.titleMatched())
                        .thenComparingInt(Fetched::rank))
                .forEach(x -> {
                    if (picked.size() < need && chosenMd5.add(x.md5())) {
                        picked.add(x);
                    }
                });
        // 释放本轮抓取但未采用图片的指纹占用(磁盘预登记的图片不在 ok 中, 不受影响)
        ok.forEach(x -> {
            if (hotlinkDefaults.contains(x.md5()) || !chosenMd5.contains(x.md5())) {
                GLOBAL_OWNERS.remove(x.md5(), seed);
            }
        });
        return picked;
    }

    /** 下载单个候选并完成全部内容校验, 不合格返回 null */
    private Fetched downloadAndValidate(Candidate c, String seed, Map<String, Set<String>> md5Hosts) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(c.url()))
                    .header("User-Agent", UA)
                    .header("Referer", "https://image.so.com/")
                    .timeout(Duration.ofSeconds(8))
                    .GET().build();
            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                log.debug("图片候选 HTTP {}: {}", resp.statusCode(), c.url());
                return null;
            }
            byte[] body = resp.body();
            if (body == null || body.length < MIN_BYTES) {
                log.debug("图片候选过小 {}B: {}", body == null ? 0 : body.length, c.url());
                return null;
            }
            String type = sniffType(body);
            // 只接受能被 JDK 解码的 jpeg/png/gif(webp/bmp/错误页一律拒绝)
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(body));
            if (img == null) {
                log.debug("图片候选无法解码(type={}): {}", type, c.url());
                return null;
            }
            int w = img.getWidth();
            int h = img.getHeight();
            double ratio = (double) w / Math.max(1, h);
            if (w < 480 || h < 300 || ratio < 0.55 || ratio > 3.2) {
                log.debug("图片候选尺寸不合格 {}x{}: {}", w, h, c.url());
                return null;
            }
            // 统一压缩到最大宽 1600 的 JPEG, 大图(3MB+)缩到几百 KB, 出图快得多
            ImageData data = compressJpeg(img);
            String md5 = md5Hex(data.bytes());
            // 即时占用指纹: 跨景点不可复用, 同景点同字节也只保留一张(在线去重)
            String owner = GLOBAL_OWNERS.putIfAbsent(md5, seed);
            if (owner != null) {
                log.debug("图片候选指纹重复(owner={}): {}", owner, c.url());
                return null;
            }
            md5Hosts.computeIfAbsent(md5, k -> ConcurrentHashMap.newKeySet())
                    .add(hostOf(c.url()));
            return new Fetched(c.rank(), c.titleMatched(), w >= h * 1.15, md5, data);
        } catch (Exception e) {
            log.debug("图片候选下载异常 {}: {}", c.url(), e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    /** 构造标题匹配词: 全名 + 去掉城市前缀和通用后缀的核心名 */
    private Set<String> keywords(String name, String city, String province) {
        Set<String> set = new LinkedHashSet<>();
        if (name == null || name.isBlank()) {
            return set;
        }
        set.add(name.trim());
        String core = name.trim();
        if (city != null && !city.isBlank() && core.startsWith(city.trim()) && core.length() > city.trim().length()) {
            core = core.substring(city.trim().length());
        }
        if (province != null && !province.isBlank() && core.startsWith(province.trim())
                && core.length() > province.trim().length() + 1) {
            core = core.substring(province.trim().length());
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String suffix : GENERIC_SUFFIXES) {
                if (core.endsWith(suffix) && core.length() > suffix.length()) {
                    core = core.substring(0, core.length() - suffix.length());
                    changed = true;
                }
            }
        }
        if (core.length() >= 2) {
            set.add(core);
        }
        return set;
    }

    private boolean titleContains(String title, Set<String> keywords) {
        if (title == null || title.isBlank()) {
            return false;
        }
        for (String kw : keywords) {
            if (kw.length() >= 2 && title.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private boolean hostBlacklisted(String host) {
        if (host == null) {
            return false;
        }
        String h = host.toLowerCase();
        for (String bad : HOST_BLACKLIST) {
            if (h.equals(bad) || h.endsWith("." + bad)) {
                return true;
            }
        }
        return false;
    }

    private String hostOf(String url) {
        try {
            String h = URI.create(url).getHost();
            return h == null ? "" : h.toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asText("");
        return s.isBlank() ? null : s;
    }

    /** 按魔数字节识别 jpeg/png/gif, 识别不了按 jpeg 兜底 (包可见以便单元测试) */
    static String sniffType(byte[] b) {
        if (b != null && b.length >= 4) {
            if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8) {
                return "image/jpeg";
            }
            if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
                return "image/png";
            }
            if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F') {
                return "image/gif";
            }
        }
        return "image/jpeg";
    }

    /** 找不到真实照片时的诚实占位: 印上景点名, 不用随机风景图冒充 */
    private ImageData placeholder(String name) {
        String safe = xmlEscape(name == null || name.isBlank() ? "云游中国" : name);
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='800' height='500'>"
                + "<defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>"
                + "<stop offset='0' stop-color='#0c4a6e'/><stop offset='1' stop-color='#0f766e'/>"
                + "</linearGradient></defs><rect width='800' height='500' fill='url(#g)'/>"
                + "<text x='400' y='238' font-size='44' fill='#ffffff' text-anchor='middle' "
                + "font-family='Microsoft YaHei,sans-serif' font-weight='bold'>" + safe + "</text>"
                + "<text x='400' y='292' font-size='22' fill='#bae6fd' text-anchor='middle' "
                + "font-family='Microsoft YaHei,sans-serif'>实景照片准备中</text></svg>";
        return new ImageData(svg.getBytes(StandardCharsets.UTF_8), "image/svg+xml");
    }

    private static String xmlEscape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(text.hashCode());
        }
    }

    private static String md5Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(bytes));
        } catch (Exception e) {
            return "";
        }
    }
}
